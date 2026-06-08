package me.molfordan.arenaAndFFAManager.bedfight;

import com.grinderwolf.swm.api.world.SlimeWorld;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.queue.enums.QueueType;
import me.molfordan.arenaAndFFAManager.queue.enums.StatisticType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class BedFightManager {
    private final ArenaAndFFAManager plugin;
    private final Map<UUID, BedFightSession> playerSessionMap = new HashMap<>();
    private final Map<Arena, BedFightSession> activeSessions = new HashMap<>();

    public BedFightManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void startMatch(Arena arena, QueueType queueType, Set<UUID> redTeam, Set<UUID> blueTeam) {
        SlimeWorld template = plugin.getBedFightArenaManager().getSlimeTemplate(arena.getName());
        if (template == null) {
            return;
        }

        World matchWorld = plugin.getBedFightArenaManager().getSlimeAdapter().createMatchWorld(template);
        if (matchWorld == null) {
            return;
        }

        BedFightSession session = new BedFightSession(arena, matchWorld, queueType, redTeam, blueTeam);
        activeSessions.put(arena, session);
        
        for (UUID uuid : redTeam) {
            playerSessionMap.put(uuid, session);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(session.getRedSpawnLoc());
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
                p.setFlying(false);
                plugin.getKitManager().applyBedFightKit(p, "RED");
            }
        }
        for (UUID uuid : blueTeam) {
            playerSessionMap.put(uuid, session);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(session.getBlueSpawnLoc());
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
                p.setFlying(false);
                plugin.getKitManager().applyBedFightKit(p, "BLUE");
            }
        }
        
        // Countdown
        runCountdown(session, redTeam, blueTeam);
    }

    private void runCountdown(BedFightSession session, Set<UUID> redTeam, Set<UUID> blueTeam) {
        // Initial scoreboard update for all
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getBedFightScoreboard().updateScoreboard(p);
        }

        for (int i = 0; i < 10; i++) { // 5 seconds = 10 intervals of 10 ticks
            final int index = i;
            final int secondsLeft = 5 - (index / 2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (index % 2 == 0) { // Only title/sound every 20 ticks (1s)
                    String title = ChatColor.YELLOW + String.valueOf(secondsLeft);
                    for (UUID uuid : session.getAllPlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendTitle(title, "");
                            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                        }
                    }
                }
                
                for (UUID uuid : session.getAllPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) plugin.getBedFightScoreboard().updateScoreboard(p);
                }
            }, index * 10L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : session.getAllPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendTitle(ChatColor.GREEN + "GO!", "");
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1f, 2f);
                    session.setPlayerState(uuid, BedFightState.PLAYING);
                    p.setGameMode(GameMode.SURVIVAL);
                }
            }

            // Apply kits
            for (UUID uuid : redTeam) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) plugin.getKitManager().applyBedFightKit(p, "RED");
            }
            for (UUID uuid : blueTeam) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) plugin.getKitManager().applyBedFightKit(p, "BLUE");
            }
            
            // Start persistent scoreboard update task
            startScoreboardTask(session);
        }, 5 * 20L);
    }

    private void startScoreboardTask(BedFightSession session) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSessions.get(session.getArena()) != session || !session.isActive()) {
                    this.cancel();
                    return;
                }
                for (UUID uuid : session.getAllPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) plugin.getBedFightScoreboard().updateScoreboard(p);
                }
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

        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.hidePlayer(spectator);
        }

        spectator.sendMessage(ChatColor.YELLOW + "You are now spectating the match.");
    }

    public void removePlayerFromSession(Player player) {
        playerSessionMap.remove(player.getUniqueId());
    }

public void endMatch(BedFightSession session, Player winner) {
    if (!session.isActive()) {
        return;
    }

    QueueType type = session.getQueueType();
    boolean isRanked = type.getStatisticType() == StatisticType.RANKED;
    boolean isUnranked = type.getStatisticType() == StatisticType.UNRANKED;
    String winningTeam = winner != null ? session.getTeam(winner.getUniqueId()) : null;

    // Save stats for participants
    for (UUID uuid : session.getAllPlayers()) {
        BedFightStats matchStats = session.getStats(uuid);
        me.molfordan.arenaAndFFAManager.object.PlayerStats dbStats = plugin.getStatsManager().getStats(uuid);

        if (isRanked) {
            dbStats.setRankedKills(dbStats.getRankedKills() + matchStats.kills);
            //dbStats.setRankedDeaths(dbStats.getRankedDeaths() + matchStats.deaths);
            dbStats.setRankedBeds(dbStats.getRankedBeds() + matchStats.bedsBroken);
            
            if (winningTeam != null) {
                if (session.getTeam(uuid).equals(winningTeam)) {
                    dbStats.setRankedWins(dbStats.getRankedWins() + 1);
                } else {
                    dbStats.setRankedLosses(dbStats.getRankedLosses() + 1);
                }
            }
        } else if (isUnranked) {
            dbStats.setUnrankedKills(dbStats.getUnrankedKills() + matchStats.kills);
            //dbStats.setUnrankedDeaths(dbStats.getUnrankedDeaths() + matchStats.deaths);
            dbStats.setUnrankedBeds(dbStats.getUnrankedBeds() + matchStats.bedsBroken);
            
            if (winningTeam != null) {
                if (session.getTeam(uuid).equals(winningTeam)) {
                    dbStats.setUnrankedWins(dbStats.getUnrankedWins() + 1);
                } else {
                    dbStats.setUnrankedLosses(dbStats.getUnrankedLosses() + 1);
                }
            }
        }

        plugin.getStatsManager().savePlayer(dbStats);
    }
    
    // ELO Updates for Ranked
    int eloChange = 0;
    if (isRanked && winningTeam != null) {
        eloChange = updateElo(session, winningTeam);
    }

    // Mark as ended
    session.setActive(false);
    for (UUID uuid : session.getAllPlayers()) {
        session.setPlayerState(uuid, BedFightState.ENDED);
    }
    for (UUID specId : session.getSpectators()) {
        session.setPlayerState(specId, BedFightState.ENDED);
    }

    String winMsg = ChatColor.GOLD + (winner != null ? winner.getName() : "Nobody") + " won the BedFight!";
    String winnerName = (winner != null ? winner.getName() : "Nobody");

    // Ranked result message
    String rankedResultMsg = "";
    if (isRanked && winningTeam != null) {
        plugin.getLogger().info("DEBUG: Ranked result msg. Winner team: " + winningTeam);
        
        String winnerPlayerName = "";
        Collection<UUID> winnerTeamPlayersSet = session.getInitialTeamPlayers(winningTeam);
        plugin.getLogger().info("DEBUG: Winner team players size: " + winnerTeamPlayersSet.size());
        List<UUID> winnerTeamPlayers = new ArrayList<>(winnerTeamPlayersSet);
        for (UUID uuid : winnerTeamPlayers) {
            winnerPlayerName = Bukkit.getOfflinePlayer(uuid).getName();
            break;
        }
        
        String loserPlayerName = "Eliminated";
        String loserTeam = winningTeam.equals("RED") ? "BLUE" : "RED";
        plugin.getLogger().info("DEBUG: Loser team: " + loserTeam);
        
        Collection<UUID> loserTeamPlayersSet = session.getInitialTeamPlayers(loserTeam);
        plugin.getLogger().info("DEBUG: Loser team players size: " + loserTeamPlayersSet.size());
        
        if (!loserTeamPlayersSet.isEmpty()) {
            List<UUID> loserTeamPlayers = new ArrayList<>(loserTeamPlayersSet);
            for (UUID uuid : loserTeamPlayers) {
                loserPlayerName = Bukkit.getOfflinePlayer(uuid).getName();
                break;
            }
        }

        int winnerNewElo = 0;
        int loserNewElo = 0;

        if (!winnerTeamPlayers.isEmpty()) {
            winnerNewElo = plugin.getStatsManager().getStats(winnerTeamPlayers.iterator().next()).getRankedElo();
        }

        if (!loserTeamPlayersSet.isEmpty()) {
            loserNewElo = plugin.getStatsManager().getStats(loserTeamPlayersSet.iterator().next()).getRankedElo();
        }

        rankedResultMsg = ChatColor.YELLOW + "" + ChatColor.BOLD + "Match Result" + "\n" +
                          ChatColor.GREEN + "Winner: " + ChatColor.YELLOW + winnerPlayerName + 
                          ChatColor.GRAY + " | " + 
                          ChatColor.RED + "Loser: " + ChatColor.YELLOW + loserPlayerName + "\n" +
                          ChatColor.GREEN + winnerNewElo + ChatColor.GRAY + " (+" + eloChange + ")" + 
                          "              " + // "Long space"
                          ChatColor.RED + loserNewElo + ChatColor.GRAY + " (-" + eloChange + ")";
    }

    // Broadcast to all participants and spectators
    Set<UUID> allInitialPlayers = new HashSet<>(session.getInitialTeamPlayers("RED"));
    allInitialPlayers.addAll(session.getInitialTeamPlayers("BLUE"));
    
    for (UUID uuid : allInitialPlayers) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            // Rematch logic for Duel
            if (session.getQueueType() == QueueType.DUEL && winner != null) {
                Player opponent = null;
                for (UUID pId : allInitialPlayers) {
                    if (!pId.equals(p.getUniqueId())) {
                        opponent = Bukkit.getPlayer(pId);
                        break;
                    }
                }
                if (opponent != null) {
                    net.md_5.bungee.api.chat.TextComponent rematchComp = new net.md_5.bungee.api.chat.TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + " (REMATCH)");
                    rematchComp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("Click to challenge!").create()));
                    rematchComp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/duel " + opponent.getName()));
                    p.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent(winMsg), rematchComp);
                } else {
                    p.sendMessage(winMsg);
                }
            } else {
                p.sendMessage(winMsg);
            }
            if (!rankedResultMsg.isEmpty()) p.sendMessage(rankedResultMsg);

            if (p.equals(winner)) {
                // If they won, show victory, otherwise defeat
                p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "VICTORY!", ChatColor.GREEN + winnerName + ChatColor.WHITE + " won the match!");
            } else {
                p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT!", ChatColor.RED + winnerName + ChatColor.WHITE + " won the match!");
            }
        }
    }
    for (UUID specId : new ArrayList<>(session.getSpectators())) {
        Player spec = Bukkit.getPlayer(specId);
        if (spec != null) {
            spec.sendMessage(winMsg);
            spec.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT!", ChatColor.GREEN + winnerName + ChatColor.WHITE + " won the match!");
        }
    }

        // 1. Clear inventory and enable flight
        ItemStack leaveItem = new ItemStack(Material.BED);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Leave (Right Click)");
        leaveItem.setItemMeta(meta);

        // Apply state to all participants
        for (UUID uuid : new ArrayList<>(session.getAllPlayers())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().clear();
                p.setHealth(20.0);
                p.setFoodLevel(20);
                p.getInventory().setArmorContents(null);
                p.getInventory().setItem(8, leaveItem);
                p.setGameMode(GameMode.ADVENTURE);
                p.setAllowFlight(true);
                p.setFlying(true);
            }
        }

        // Handle spectators
        for (UUID specId : new ArrayList<>(session.getSpectators())) {
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
                for (UUID uuid : session.getAllPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                }
                for (UUID specId : session.getSpectators()) {
                    Player spec = Bukkit.getPlayer(specId);
                    if (spec != null) spec.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                }
            }, 1L);

            Location lobby = plugin.getConfigManager().getLobbyLocation();
            
            // Cleanup participants and spectators
            Collection<Player> toTeleport = new java.util.ArrayList<>();
            for (UUID uuid : session.getAllPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getWorld().equals(session.getMatchWorld())) toTeleport.add(p);
            }
            
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
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean isInParty = plugin.getPartyManager().isInParty(p.getUniqueId());
                    plugin.getLogger().info("DEBUG: Post-match inventory restoration for " + p.getName() + ". In party: " + isInParty);
                    
                    if (isInParty) {
                        plugin.getPartyManager().givePartyItems(p);
                    } else {
                        plugin.getSpawnItem().giveSpawnItem(p);
                    }
                }, 1L);
            }
            
            for (UUID specId : session.getSpectators()) {
                playerSessionMap.remove(specId);
            }

            // Unload and delete world
            plugin.getBedFightArenaManager().getSlimeAdapter().unloadWorld(session.getMatchWorld().getName());

            // Remove players from session map AFTER teleport/cleanup
            for (UUID uuid : session.getAllPlayers()) {
                playerSessionMap.remove(uuid);
            }
        }, 10 * 20L);

        activeSessions.remove(session.getArena());
    }

    private int updateElo(BedFightSession session, String winningTeam) {
        Set<UUID> winners = session.getInitialTeamPlayers(winningTeam);
        Set<UUID> losers = session.getInitialTeamPlayers(winningTeam.equals("RED") ? "BLUE" : "RED");
        
        if (winners.isEmpty() || losers.isEmpty()) return 0;
        
        // Calculate average ELO for teams
        double avgWinnerElo = 0;
        for (UUID uuid : winners) avgWinnerElo += plugin.getStatsManager().getStats(uuid).getRankedElo();
        avgWinnerElo /= winners.size();
        
        double avgLoserElo = 0;
        for (UUID uuid : losers) avgLoserElo += plugin.getStatsManager().getStats(uuid).getRankedElo();
        avgLoserElo /= losers.size();
        
        // Elo calculation constants
        double kFactor = 32;
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (avgLoserElo - avgWinnerElo) / 400.0));
        
        int eloChange = (int) Math.round(kFactor * (1.0 - expectedWinner));
        if (eloChange < 10) eloChange = 10; // Minimum 10 ELO gain/loss
        
        plugin.getLogger().info("DEBUG: ELO Calculation. AvgWinner: " + avgWinnerElo + ", AvgLoser: " + avgLoserElo + ", Change: " + eloChange);
        
        for (UUID uuid : winners) {
            me.molfordan.arenaAndFFAManager.object.PlayerStats stats = plugin.getStatsManager().getStats(uuid);
            int oldElo = stats.getRankedElo();
            stats.setRankedElo(oldElo + eloChange);
            if (stats.getRankedElo() > stats.getPeakElo()) stats.setPeakElo(stats.getRankedElo());
            plugin.getStatsManager().savePlayer(stats);
            plugin.getLogger().info("DEBUG: Saving winner " + uuid + ". Old: " + oldElo + ", New: " + stats.getRankedElo());
        }
        
        for (UUID uuid : losers) {
            me.molfordan.arenaAndFFAManager.object.PlayerStats stats = plugin.getStatsManager().getStats(uuid);
            int oldElo = stats.getRankedElo();
            stats.setRankedElo(Math.max(0, oldElo - eloChange));
            plugin.getStatsManager().savePlayer(stats);
            plugin.getLogger().info("DEBUG: Saving loser " + uuid + ". Old: " + oldElo + ", New: " + stats.getRankedElo());
        }
        return eloChange;
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
