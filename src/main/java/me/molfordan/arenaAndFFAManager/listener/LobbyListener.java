package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LobbyListener implements Listener {

    private final ConfigManager configManager;
    // Define the Y level below which a player is considered in the void
    private static final double VOID_Y_LEVEL = 0.0;

    private ArenaAndFFAManager plugin;

    private KitManager kitManager;
    
    // Track players who should receive bridge fight spawn items
    public final Set<UUID> bridgeFightSpawnRecipients = new HashSet<>();

    // Must have a constructor to inject the ConfigManager
    public LobbyListener(ConfigManager configManager, ArenaAndFFAManager plugin, KitManager kitManager) {
        this.configManager = configManager;
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    // Add player to bridge fight spawn recipients list
    public void addBridgeFightSpawnRecipient(UUID playerId) {
        bridgeFightSpawnRecipients.add(playerId);
    }

    // Helper method to check if the player is in the lobby world
    private boolean isPlayerInLobby(Player player) {
        Location lobbyLoc = configManager.getLobbyLocation();
        // Return false if lobby is not set, or if player's world does not match the lobby world
        if (lobbyLoc == null || !player.getWorld().getName().equals(lobbyLoc.getWorld().getName())) {
            return false;
        }
        return true;
    }

    private boolean isPlayerInBridgeFight(Player player){
        Location brideFightLoc = configManager.getBridgeFightLocation();

        if (brideFightLoc == null || !player.getWorld().getName().equals(brideFightLoc.getWorld().getName())){
            return false;
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        PlayerStats playerStats = plugin.getStatsManager().getStats(player.getUniqueId());

        Location lobbyLoc = configManager.getLobbyLocation();

        if (lobbyLoc == null) {
            player.sendMessage(ChatColor.RED + "Lobby location is not set! Please contact an admin.");
            return;
        }

        // Teleport player to lobby
        player.teleport(lobbyLoc);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        plugin.getSpawnItem().giveSpawnItem(player);
        ArenaAndFFAManager.getPlugin().getStatsManager().resetStreak(player.getUniqueId(), ArenaType.FFA);
        ArenaAndFFAManager.getPlugin().getStatsManager().resetStreak(player.getUniqueId(), ArenaType.FFABUILD);
        // Requirement: Set gamemode to Adventure
        player.setGameMode(GameMode.ADVENTURE);
        event.setJoinMessage(null);

        // Send patch notes to player
        List<String> patchNotes = configManager.getPatchNotes();
        if (patchNotes != null) {
            player.sendMessage(ChatColor.GOLD + "----- Patch Notes -----");
            for (String note : patchNotes) {
                player.sendMessage(ChatColor.WHITE + "- " + note);
            }
            player.sendMessage(ChatColor.GOLD + "-----------------------");
        }
    }



    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event){
        // Ensure the entity being damaged is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Only cancel damage if the player is in the lobby world
        if (!isPlayerInLobby(player)) return;

        // Requirement: Take no damage (including fall damage)
        event.setCancelled(true);
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        // Check if both the damager and the entity being damaged are players
        if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Requirement: Unable to hit players
        if (isPlayerInLobby(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event){
        Player player = event.getPlayer();

        if (isPlayerInLobby(player)){
            if (configManager.isBuildMode(player.getUniqueId())) return;
            event.setCancelled(true);
            return;
        }

        if (isPlayerInBridgeFight(player)){
            if (configManager.isBuildMode(player.getUniqueId())) return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) return;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR) return;

        if (isPlayerInLobby(player)){
            if (configManager.isBuildMode(player.getUniqueId())) return;
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) event.setCancelled(true);


        }
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event){
        Player player = event.getPlayer();

        if (isPlayerInLobby(player)){
            if (configManager.isBuildMode(player.getUniqueId())) return;
            event.setCancelled(true);
            return;
        }

        if (isPlayerInBridgeFight(player)){
            if (configManager.isBuildMode(player.getUniqueId())) return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location lobbyLoc = configManager.getLobbyLocation();

        // Check if the lobby is set and if the player is in the lobby world
        if (!isPlayerInLobby(player) || lobbyLoc == null) return;

        // Requirement: Teleport back to lobby location if player falls into the void
        if (player.getLocation().getY() < VOID_Y_LEVEL) {
            // Teleport the player back to the set lobby location
            player.teleport(lobbyLoc);
            plugin.getSpawnItem().giveSpawnItem(player);

        }
    }

    @EventHandler
    public void onWorldChangeEvent(PlayerChangedWorldEvent event){
        Player player = event.getPlayer();
        World buildFFAWorld = configManager.getBuildFFALocation().getWorld();
        World lobbyWorld = configManager.getLobbyLocation().getWorld();
        World bridgeFightWorld = configManager.getBridgeFightLocation().getWorld();

        if (buildFFAWorld == null) return;

        if (lobbyWorld == null) return;

        if (bridgeFightWorld == null) return;

        if (player.getWorld().equals(buildFFAWorld)){
            player.setGameMode(GameMode.SURVIVAL);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                kitManager.applyBuildFFAKit(player);
            }, 1);

            return;
        }

        if (player.getWorld().equals(lobbyWorld)){
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getSpawnItem().giveSpawnItem(player);
            }, 1);
            player.setGameMode(GameMode.ADVENTURE);
            if (player.hasPermission("luckyessentials.fly")){

                player.setAllowFlight(true);
                player.setFlying(true);
                Location loc = player.getLocation();
                double locY = loc.getY();
                player.teleport(new Location(loc.getWorld(), loc.getX(), locY + 2, loc.getZ()));
            }
            // Flight state will be handled by FlightManager through PlayerJoinEvent in GlobalListener
            return;
        }



        if (player.getWorld().equals(bridgeFightWorld)){
            boolean isBanned = plugin.getBridgeFightBanManager().isPlayerBanned(player.getUniqueId());
            if (isBanned){
                player.teleport(lobbyWorld.getSpawnLocation());
                return;
            }
            player.setGameMode(GameMode.SURVIVAL);
            /*
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only give bridge fight spawn items if player used /bridgefight or /housing command
                if (bridgeFightSpawnRecipients.contains(player.getUniqueId())) {
                    plugin.getSpawnItem().giveBridgeFightSpawnItem(player);
                    bridgeFightSpawnRecipients.remove(player.getUniqueId()); // Remove after giving items
                }
            }, 1);
            return;

             */
        }


    }
}