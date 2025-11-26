package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderPearlListener implements Listener {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_SECONDS = 5;
    private static final long COOLDOWN_MILLIS = COOLDOWN_SECONDS * 1000;
    private final ArenaAndFFAManager plugin;

    public EnderPearlListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(id)) {
            long last = cooldowns.get(id);
            long remaining = (last + COOLDOWN_MILLIS) - now;

            if (remaining > 0) {
                event.setCancelled(true);

                // Restore pearl if client consumed it early
                //player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));

                double sec = remaining / 1000.0;
                player.sendMessage("§cYou must wait §e" + String.format("%.1f", sec) + "s §cbefore using another Ender Pearl!");
                return;
            }
        }

        // Register cooldown start
        cooldowns.put(id, now);

        // Schedule notification task
        long expectedFinish = now + COOLDOWN_MILLIS;
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    // Player offline or cooldown changed → don't notify
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) return;

                    Long stored = cooldowns.get(id);
                    if (stored == null) return;
                    if (stored + COOLDOWN_MILLIS != expectedFinish) return;

                    p.sendMessage("§aYou can now use your Ender Pearl again!");
                },
                COOLDOWN_SECONDS * 20L
        );
    }

    public static long getCooldownSeconds() { return COOLDOWN_SECONDS; }
    public static long getCooldownMillis() { return COOLDOWN_MILLIS; }
    public static Map<UUID, Long> getCooldowns() { return cooldowns; }
}
