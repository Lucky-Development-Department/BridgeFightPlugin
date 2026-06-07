package me.molfordan.arenaAndFFAManager.bedfight;

import com.grinderwolf.swm.api.world.SlimeWorld;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedFightManager {
    private final ArenaAndFFAManager plugin;
    private final Map<UUID, BedFightSession> playerSessionMap = new HashMap<>();
    private final Map<Arena, BedFightSession> activeSessions = new HashMap<>();

    public BedFightManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void startMatch(Arena arena, Player red, Player blue) {
        SlimeWorld template = plugin.getBedFightArenaManager().getSlimeTemplate(arena.getName());
        if (template == null) {
            String error = ChatColor.RED + "Could not find Slime template for arena: " + arena.getName();
            red.sendMessage(error);
            blue.sendMessage(error);
            return;
        }

        World matchWorld = plugin.getBedFightArenaManager().getSlimeAdapter().createMatchWorld(template);
        if (matchWorld == null) {
            String error = ChatColor.RED + "Failed to generate match world via SlimeWorldManager.";
            red.sendMessage(error);
            blue.sendMessage(error);
            return;
        }

        BedFightSession session = new BedFightSession(arena, matchWorld, red, blue);
        activeSessions.put(arena, session);
        playerSessionMap.put(red.getUniqueId(), session);
        playerSessionMap.put(blue.getUniqueId(), session);

        red.teleport(session.getRedSpawnLoc());
        blue.teleport(session.getBlueSpawnLoc());
        
        // Prepare state: Survival, No Fly, Apply Kit
        red.setGameMode(GameMode.SURVIVAL);
        blue.setGameMode(GameMode.SURVIVAL);
        red.setAllowFlight(false);
        red.setFlying(false);
        blue.setAllowFlight(false);
        blue.setFlying(false);

        plugin.getKitManager().applyBedFightKit(red, "RED");
        plugin.getKitManager().applyBedFightKit(blue, "BLUE");

        // Countdown
        runCountdown(session, red, blue);
    }

    private void runCountdown(BedFightSession session, Player red, Player blue) {
        // Initial scoreboard update
        plugin.getBedFightScoreboard().updateScoreboard(red);
        plugin.getBedFightScoreboard().updateScoreboard(blue);

        for (int i = 0; i < 10; i++) { // 5 seconds = 10 intervals of 10 ticks
            final int index = i;
            final int secondsLeft = 5 - (index / 2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (index % 2 == 0) { // Only title/sound every 20 ticks (1s)
                    String title = ChatColor.YELLOW + String.valueOf(secondsLeft);
                    red.sendTitle(title, "");
                    blue.sendTitle(title, "");
                    red.playSound(red.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    blue.playSound(blue.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                }
                
                plugin.getBedFightScoreboard().updateScoreboard(red);
                plugin.getBedFightScoreboard().updateScoreboard(blue);
            }, index * 10L); // 10 ticks interval
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            red.sendTitle(ChatColor.GREEN + "GO!", "");
            blue.sendTitle(ChatColor.GREEN + "GO!", "");
            red.playSound(red.getLocation(), Sound.NOTE_PLING, 1f, 2f);
            blue.playSound(blue.getLocation(), Sound.NOTE_PLING, 1f, 2f);

            session.setPlayerState(red.getUniqueId(), BedFightState.PLAYING);
            session.setPlayerState(blue.getUniqueId(), BedFightState.PLAYING);

            // Force Survival
            red.setGameMode(GameMode.SURVIVAL);
            blue.setGameMode(GameMode.SURVIVAL);

            // Apply kit
            plugin.getKitManager().applyBedFightKit(red, "RED");
            plugin.getKitManager().applyBedFightKit(blue, "BLUE");
            
            // Start persistent scoreboard update task
            startScoreboardTask(session);
        }, 5 * 20L);
    }

    private void startScoreboardTask(BedFightSession session) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSessions.get(session.getArena()) != session) {
                    this.cancel();
                    return;
                }
                Player p1 = Bukkit.getPlayer(session.getRedPlayer());
                Player p2 = Bukkit.getPlayer(session.getBluePlayer());
                if (p1 != null) plugin.getBedFightScoreboard().updateScoreboard(p1);
                if (p2 != null) plugin.getBedFightScoreboard().updateScoreboard(p2);
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    public void addSpectator(BedFightSession session, Player spectator) {
        session.addSpectator(spectator.getUniqueId());
        playerSessionMap.put(spectator.getUniqueId(), session);

        Location specLoc = session.getArena().getCenter();
        if (specLoc == null || specLoc.getWorld() == null) {
            specLoc = session.getMatchWorld().getSpawnLocation();
        }
        
        spectator.teleport(specLoc);
        spectator.setGameMode(GameMode.ADVENTURE);
        spectator.setAllowFlight(true);
        spectator.setFlying(true);
        spectator.getInventory().clear();
        spectator.getInventory().setArmorContents(null);

        Player red = Bukkit.getPlayer(session.getRedPlayer());
        Player blue = Bukkit.getPlayer(session.getBluePlayer());

        if (red != null) red.hidePlayer(spectator);
        if (blue != null) blue.hidePlayer(spectator);

        spectator.sendMessage(ChatColor.YELLOW + "You are now spectating the match.");
    }

    public void removePlayerFromSession(Player player) {
        playerSessionMap.remove(player.getUniqueId());
    }

    public void endMatch(BedFightSession session, Player winner) {
        // Check if already ended
        if (session.getPlayerState(session.getRedPlayer()) == BedFightState.ENDED || 
            session.getPlayerState(session.getBluePlayer()) == BedFightState.ENDED) {
            return;
        }
        
        // Mark as ended
        session.setPlayerState(session.getRedPlayer(), BedFightState.ENDED);
        session.setPlayerState(session.getBluePlayer(), BedFightState.ENDED);
        for (UUID specId : session.getSpectators()) {
            session.setPlayerState(specId, BedFightState.ENDED);
        }
        
        String winMsg = ChatColor.GOLD + (winner != null ? winner.getName() : "Nobody") + " won the BedFight!";
        String winnerName = (winner != null ? winner.getName() : "Nobody");

        // Broadcast to all participants and spectators
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(winMsg);
                p.playSound(p.getLocation(), Sound.EXPLODE, 1f, 1f);
                if (p.equals(winner)) {
                    p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "VICTORY!", ChatColor.GREEN + winnerName + ChatColor.WHITE + " won the match!");
                } else {
                    p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT!", ChatColor.RED + winnerName + ChatColor.WHITE + " won the match!");
                }
            }
        }
        for (UUID specId : session.getSpectators()) {
            Player spec = Bukkit.getPlayer(specId);
            if (spec != null) {
                spec.sendMessage(winMsg);
                spec.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + "GAME OVER", ChatColor.WHITE + winnerName + " won the match!");
            }
        }

        // 1. Clear inventory and enable flight
        ItemStack leaveItem = new ItemStack(Material.BED);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Leave (Right Click)");
        leaveItem.setItemMeta(meta);

        // Apply state to all participants
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.getInventory().setItem(8, leaveItem);
                p.setGameMode(GameMode.ADVENTURE);
                p.setAllowFlight(true);
                p.setFlying(true);
            }
        }

        // Handle spectators
        for (UUID specId : session.getSpectators()) {
            Player spec = Bukkit.getPlayer(specId);
            if (spec != null) {
                spec.getInventory().clear();
                spec.getInventory().setArmorContents(null);
                spec.getInventory().setItem(8, leaveItem);
                spec.setGameMode(GameMode.ADVENTURE);
                spec.setAllowFlight(true);
                spec.setFlying(true);
            }
        }

        // 2. Wait 10 seconds then teleport to lobby and disable flight
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove scoreboard 1 tick before teleport
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p1 = Bukkit.getPlayer(session.getRedPlayer());
                Player p2 = Bukkit.getPlayer(session.getBluePlayer());
                if (p1 != null) p1.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                if (p2 != null) p2.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                for (UUID specId : session.getSpectators()) {
                    Player spec = Bukkit.getPlayer(specId);
                    if (spec != null) spec.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                }
            }, 1L);

            Location lobby = plugin.getConfigManager().getLobbyLocation();
            
            // Cleanup participants and spectators
            Player p1 = Bukkit.getPlayer(session.getRedPlayer());
            Player p2 = Bukkit.getPlayer(session.getBluePlayer());
            
            Collection<Player> toTeleport = new java.util.ArrayList<>();
            if (p1 != null && p1.getWorld().equals(session.getMatchWorld())) toTeleport.add(p1);
            if (p2 != null && p2.getWorld().equals(session.getMatchWorld())) toTeleport.add(p2);
            
            for (UUID specId : session.getSpectators()) {
                Player spec = Bukkit.getPlayer(specId);
                if (spec != null && spec.getWorld().equals(session.getMatchWorld())) toTeleport.add(spec);
            }
            
            for (Player p : toTeleport) {
                p.teleport(lobby);
                p.setFlying(false);
                p.setAllowFlight(false);
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
            }
            
            for (UUID specId : session.getSpectators()) {
                playerSessionMap.remove(specId);
            }

            // Unload and delete world
            plugin.getBedFightArenaManager().getSlimeAdapter().unloadWorld(session.getMatchWorld().getName());

            // Remove players from session map AFTER teleport/cleanup
            playerSessionMap.remove(session.getRedPlayer());
            playerSessionMap.remove(session.getBluePlayer());
        }, 10 * 20L);

        activeSessions.remove(session.getArena());
    }

    public BedFightSession getSession(Player player) {
        return playerSessionMap.get(player.getUniqueId());
    }

    public BedFightSession getSession(Arena arena) {
        return activeSessions.get(arena);
    }
    
    public boolean isInMatch(Player player) {
        return playerSessionMap.containsKey(player.getUniqueId());
    }

    public Collection<BedFightSession> getAllActiveArenas() {
        return activeSessions.values();
    }

    public Collection<Arena> getAllArenas() {
        return plugin.getBedFightArenaManager().getArenas();
    }
}
