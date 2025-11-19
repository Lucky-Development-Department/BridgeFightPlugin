package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FrozenManager implements Listener {

    private final ArenaAndFFAManager plugin;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private int titleTaskID = -1;

    public FrozenManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        startTitleSpamTask();
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void freezePlayer(Player player, Player staff) {
        if (isFrozen(player)) return;

        frozenPlayers.add(player.getUniqueId());

        // Give Slowness 255 (maximum level) for total movement cancellation and zoom effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false), true);
        // Give Blindness for visual disorientation/suspect
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false), true);

        // Teleport the player to their exact location to prevent initial movement/falling
        player.teleport(player.getLocation());

        // Send messages
        staff.sendMessage(ChatColor.GREEN + "Successfully froze " + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + ".");
        player.sendMessage(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "YOU ARE FROZEN!" + ChatColor.RED + " Do not disconnect.");

        plugin.debug(staff.getName() + " froze " + player.getName() + " for suspecting cheats.");
    }

    public void unfreezePlayer(Player player, Player staff) {
        if (!isFrozen(player)) return;

        frozenPlayers.remove(player.getUniqueId());

        // Remove effects
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        // Send messages
        staff.sendMessage(ChatColor.GREEN + "Successfully unfroze " + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + ".");
        player.sendMessage(ChatColor.GREEN + "You have been unfrozen. Thank you for your cooperation.");

        plugin.getLogger().info(staff.getName() + " unfroze " + player.getName() + ".");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isFrozen(player)) {
            // Check if the location actually changed (i.e., not just head rotation)
            /*
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {

                // Teleport them back to the original spot to prevent all movement
                event.setTo(event.getFrom());
            }

             */
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (isFrozen((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player)) {
            // The player is frozen and logged out -> Execute ban command

            String playerName = player.getName();
            // The ban command text is color-coded using the format requested by the user
            String command = "tempipban " + playerName + " 7d " +
                    ChatColor.translateAlternateColorCodes('&', "&c&lSENTRY &a&l/ &b" + playerName + " logged out while being frozen");

            // Execute the console command on the next tick
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });

            // Remove from the frozen list immediately
            frozenPlayers.remove(player.getUniqueId());

            plugin.getLogger().warning(playerName + " logged out while frozen and was IP-banned for 7 days.");
        }
    }

        private void startTitleSpamTask() {
            // Run every 20 ticks (1 second)
            titleTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                // Stop the task if no one is frozen
                if (frozenPlayers.isEmpty() && titleTaskID != -1) {
                    // We should keep the task running as freezing is dynamic, but we can optimize.
                    // For simplicity and constant spam, we'll just check the list size inside the loop.
                }

                // Titles need to be compatible with 1.8.9 Player.sendTitle()
                String title = ChatColor.RED.toString() + ChatColor.BOLD + "DO NOT LOG OUT";
                String subTitle = ChatColor.GRAY + "Logout will result in 7d ban";


                for (UUID uuid : frozenPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        // sendTitle(title, subtitle, fadeIn, stay, fadeOut)
                        // fadeIn=0, stay=20 (1 second), fadeOut=0 ensures maximum spam rate
                        player.sendTitle(title, subTitle);
                        player.sendMessage(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "YOU ARE FROZEN!" + ChatColor.RED + " Do not disconnect.");
                    }
                }
            }, 0L, 20L); // Start immediately (0L) and repeat every 20 ticks (1 second)
        }


    public void stopTitleSpamTask() {
        if (titleTaskID != -1) {
            Bukkit.getScheduler().cancelTask(titleTaskID);
        }
    }


}
