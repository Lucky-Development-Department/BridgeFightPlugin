package me.molfordan.bridgefightplugin.bedfight;

import com.grinderwolf.swm.api.world.SlimeWorld;
import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.bedfight.events.DuelEndEvent;
import me.molfordan.bridgefightplugin.bedfight.events.DuelStartEvent;
import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import me.molfordan.bridgefightplugin.queue.enums.StatisticType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BedFightManager {
    private final BridgeFightPlugin plugin;
    private final Map<UUID, BedFightSession> playerSessionMap = new ConcurrentHashMap<>();
    private final Map<UUID, BedFightSession> spectatorSessionMap = new ConcurrentHashMap<>();
    private final Map<Arena, BedFightSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> gameEndTimes = new ConcurrentHashMap<>();

    public BedFightManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    public void setGameEndTime(UUID uuid, long time) {
        gameEndTimes.put(uuid, time);
    }

    public long getGameEndTime(UUID uuid) {
        return gameEndTimes.getOrDefault(uuid, 0L);
    }

    public void startMatch(Arena arena, QueueType queueType, Set<UUID> redTeam, Set<UUID> blueTeam) {
        SlimeWorld template = plugin.getBedFightArenaManager().getSlimeTemplate(arena.getName());
        if (template == null) return;

        World matchWorld = plugin.getBedFightArenaManager().getSlimeAdapter().createMatchWorld(template);
        if (matchWorld == null) return;

        cleanupPreviousQueuedWorlds(redTeam, blueTeam);

        BedFightSession session = new BedFightSession(arena, matchWorld, queueType, redTeam, blueTeam);
        
        // Final verification that players are still available and not in another match
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || isInMatch(p)) {
                plugin.getBedFightArenaManager().getSlimeAdapter().unloadWorld(matchWorld.getName());
                return;
            }
        }

        session.setSessionState(BedFightSessionState.COUNTDOWN);
        activeSessions.put(arena, session);

        if (session.getQueueType() == QueueType.SOLO_UNRANKED) {

            plugin.getBedFightHologramManager().createTeamHolograms(session);
        }
        
        setupPlayers(session, redTeam, "RED");
        setupPlayers(session, blueTeam, "BLUE");
        
        runCountdown(session);
    }

    private void cleanupPreviousQueuedWorlds(Set<UUID> redTeam, Set<UUID> blueTeam) {
        Set<UUID> all = new HashSet<>(redTeam);
        all.addAll(blueTeam);
        for (UUID uuid : all) {
            String queuedWorldName = plugin.getMatchmakingService().getQueuedWorld(uuid);
            if (queuedWorldName != null) {
                plugin.getBedFightArenaManager().getSlimeAdapter().unloadWorld(queuedWorldName);
            }
        }
    }

    private void setupPlayers(BedFightSession session, Set<UUID> team, String color) {
        for (UUID uuid : team) {
            playerSessionMap.put(uuid, session);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(color.equals("RED") ? session.getRedSpawnLoc() : session.getBlueSpawnLoc());
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
                p.setFlying(false);
                plugin.getKitManager().applyBedFightKit(p, color);
            }
        }
    }

    private void runCountdown(BedFightSession session) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!session.isActive()) { this.cancel(); return; }
                
                if (ticks % 20 == 0) {
                    int secondsLeft = 5 - (ticks / 20);
                    if (secondsLeft > 0) {
                        broadcastTitle(session, ChatColor.YELLOW + String.valueOf(secondsLeft), "", Sound.NOTE_PLING, 1f, 1f);
                    }
                }
                
                updateAllScoreboards(session);
                
                if (ticks >= 100) {
                    this.cancel();
                    startGameplay(session);
                }
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void startGameplay(BedFightSession session) {
        session.setSessionState(BedFightSessionState.RUNNING);
        broadcastTitle(session, ChatColor.GREEN + "GO!", "", Sound.NOTE_PLING, 1f, 2f);
        
        plugin.getBedFightHologramManager().removeHolograms(session);
        
        // Call DuelStartEvent
        Bukkit.getPluginManager().callEvent(new DuelStartEvent(session));

        for (UUID uuid : session.getAllPlayers()) {
            session.setPlayerState(uuid, BedFightPlayerState.PLAYING);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                plugin.getKitManager().applyBedFightKit(p, session.getTeam(uuid));
            }
        }
        startScoreboardTask(session);
    }

    private void broadcastTitle(BedFightSession session, String title, String subtitle, Sound sound, float vol, float pitch) {
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendTitle(title, subtitle);
                if (sound != null) p.playSound(p.getLocation(), sound, vol, pitch);
            }
        }
    }

    private void startScoreboardTask(BedFightSession session) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!session.isActive()) { this.cancel(); return; }
                updateAllScoreboards(session);
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    public void addSpectator(BedFightSession session, Player spectator) {
        session.addSpectator(spectator.getUniqueId());
        session.setPlayerState(spectator.getUniqueId(), BedFightPlayerState.SPECTATOR);
        spectatorSessionMap.put(spectator.getUniqueId(), session);

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

        // Hide spectator from all participants
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.hidePlayer(spectator);
        }

        // Show other spectators to this spectator, and this spectator to other spectators
        for (UUID specId : session.getSpectators()) {
            Player otherSpec = Bukkit.getPlayer(specId);
            if (otherSpec != null && !otherSpec.equals(spectator)) {
                spectator.showPlayer(otherSpec);
                otherSpec.showPlayer(spectator);
            }
        }

        SpectatorListener.giveSpectatorItems(spectator);
        spectator.sendMessage(ChatColor.YELLOW + "You are now spectating the match.");
    }

    private void updateAllScoreboards(BedFightSession session) {
        Set<UUID> all = new HashSet<>(session.getAllPlayers());
        all.addAll(session.getSpectators());
        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getBedFightScoreboard().updateScoreboard(p);
        }
    }

    public void endMatch(BedFightSession session, String winningTeam, boolean isForfeit) {
        if (session.getSessionState() == BedFightSessionState.ENDING || session.getSessionState() == BedFightSessionState.CLEANUP) return;

        session.setSessionState(BedFightSessionState.ENDING);
        saveMatchStats(session, winningTeam);

        // Call DuelEndEvent
        Bukkit.getPluginManager().callEvent(new DuelEndEvent(session, winningTeam, isForfeit));
        
        int eloChange = 0;
        if (session.getQueueType().getStatisticType() == StatisticType.RANKED && winningTeam != null) {
            eloChange = updateElo(session, winningTeam);
        }

        long now = System.currentTimeMillis();
        Set<UUID> allParticipants = new HashSet<>(session.getAllPlayers());
        allParticipants.addAll(session.getSpectators());

        for (UUID uuid : allParticipants) {
            if (session.getPlayerState(uuid) != BedFightPlayerState.DIED) {
                session.setPlayerState(uuid, BedFightPlayerState.ENDED);
            }
            setGameEndTime(uuid, now);
        }

        broadcastMatchResults(session, winningTeam, eloChange);
        setupEndItems(session, allParticipants);

        new BukkitRunnable() {
            int seconds = 10;
            @Override
            public void run() {
                if (seconds <= 0) {
                    session.setSessionState(BedFightSessionState.CLEANUP);
                    cleanupSession(session);
                    activeSessions.remove(session.getArena());
                    this.cancel();
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void saveMatchStats(BedFightSession session, String winningTeam) {
        StatisticType statType = session.getQueueType().getStatisticType();
        if (statType == StatisticType.NONE) return;

        for (UUID uuid : session.getAllPlayers()) {
            BedFightStats matchStats = session.getStats(uuid);
            me.molfordan.bridgefightplugin.object.PlayerStats dbStats = plugin.getStatsManager().getStats(uuid);
            boolean won = session.getInitialTeam(uuid).equals(winningTeam);

            if (statType == StatisticType.RANKED) {
                dbStats.setRankedKills(dbStats.getRankedKills() + matchStats.kills);
                dbStats.setRankedBeds(dbStats.getRankedBeds() + matchStats.bedsBroken);
                if (winningTeam != null) {
                    if (won) dbStats.setRankedWins(dbStats.getRankedWins() + 1);
                    else dbStats.setRankedLosses(dbStats.getRankedLosses() + 1);
                }
            } else if (statType == StatisticType.UNRANKED) {
                dbStats.setUnrankedKills(dbStats.getUnrankedKills() + matchStats.kills);
                dbStats.setUnrankedBeds(dbStats.getUnrankedBeds() + matchStats.bedsBroken);
                if (winningTeam != null) {
                    if (won) dbStats.setUnrankedWins(dbStats.getUnrankedWins() + 1);
                    else dbStats.setUnrankedLosses(dbStats.getUnrankedLosses() + 1);
                }
            }
            plugin.getStatsManager().savePlayer(dbStats);
        }
    }

    private void broadcastMatchResults(BedFightSession session, String winningTeam, int eloChange) {
        String winnerName = "Nobody";
        ChatColor winnerColor = ChatColor.WHITE;

        if (winningTeam != null) {
            winnerColor = winningTeam.equalsIgnoreCase("RED") ? ChatColor.RED : ChatColor.BLUE;
            winnerName = session.getInitialTeamPlayers(winningTeam).stream()
                    .map(u -> Bukkit.getOfflinePlayer(u).getName())
                    .collect(java.util.stream.Collectors.joining(" & "));
        }

        String winMsg = winnerColor + winnerName + ChatColor.YELLOW + " won the match!";
        
        for (UUID uuid : session.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            
            p.sendMessage(winMsg);
            p.playSound(p.getLocation(), Sound.EXPLODE, 1f, 1f);
            
            String playerTeam = session.getInitialTeam(uuid);
            if (playerTeam != null && playerTeam.equals(winningTeam)) {
                p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "VICTORY!", ChatColor.YELLOW + "You won the match!");
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1f);
            } else {
                p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT!", winnerColor + winnerName + ChatColor.YELLOW + " won!");
            }
        }
    }

    private void setupEndItems(BedFightSession session, Set<UUID> players) {
        ItemStack leaveItem = createItem(Material.BED, ChatColor.RED + "Leave (Right Click)");
        ItemStack playAgain = createItem(Material.PAPER, ChatColor.GREEN + "Play Again (Right Click)");
        
        boolean canRequeue = session.getQueueType() != QueueType.DUEL;

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            
            // Determine if player won
            boolean won = session.getInitialTeam(uuid) != null && session.getInitialTeam(uuid).equals(session.getWinningTeam());

            // Clear inventory first
            p.getInventory().clear();
            
            // Only clear armor if the player lost
            if (!won) {
                p.getInventory().setArmorContents(null);
            }
            
            p.setHealth(20.0);
            p.setFoodLevel(20);
            
            // Main hand (slot 0) for Play Again, slot 8 for Leave
            if (canRequeue && session.getInitialTeam(uuid) != null) {
                p.getInventory().setItem(0, playAgain);
                p.setMetadata("lastQueueType", new org.bukkit.metadata.FixedMetadataValue(plugin, session.getQueueType().name()));
            }
            p.getInventory().setItem(8, leaveItem);

            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(true);
        }
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void cleanupSession(BedFightSession session) {
        Location lobby = plugin.getConfigManager().getLobbyLocation();
        Set<UUID> all = new HashSet<>(session.getAllPlayers());
        all.addAll(session.getSpectators());

        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            // 1. If they joined a DIFFERENT session already, don't touch them
            if (getSession(p) != null && getSession(p) != session) {
                removePlayerFromSession(uuid, session);
                continue;
            }

            // 2. If they are already in a DIFFERENT world (lobby, buildffa, etc), just clean up session data
            if (p.getWorld() != session.getMatchWorld()) {
                removePlayerFromSession(uuid, session);
                continue;
            }

            // 3. They are still in the match world - we MUST move them as world is unloading
            removePlayerFromSession(uuid, session);
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.teleport(lobby);

            // 4. Check if they are in a queue (e.g. via /playagain)
            if (plugin.getMatchmakingService().isInWaitingQueue(p.getUniqueId())) {
                p.sendMessage(ChatColor.YELLOW + "Match world closed, you've been moved to lobby while in queue.");
                continue;
            }

            // 5. Default cleanup for players who didn't play again or move
            p.setFlying(false);
            p.setAllowFlight(false);
            p.getInventory().clear();
            p.sendMessage(ChatColor.YELLOW + "The match has ended, returning to lobby.");
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isInMatch(p)) return;
                if (plugin.getMatchmakingService().isInWaitingQueue(p.getUniqueId())) {
                    plugin.getMatchmakingService().giveLeaveItem(p);
                } else if (plugin.getPartyManager().isInParty(p.getUniqueId())) {
                    plugin.getPartyManager().givePartyItems(p);
                } else {
                    plugin.getSpawnItem().giveSpawnItem(p);
                }
                if (p.hasPermission("luckyessentials.fly")) p.setAllowFlight(true);
            }, 1L);
        }
        plugin.getBedFightArenaManager().getSlimeAdapter().unloadWorld(session.getMatchWorld().getName());
    }

    private int updateElo(BedFightSession session, String winningTeam) {
        Set<UUID> winners = session.getInitialTeamPlayers(winningTeam);
        Set<UUID> losers = session.getInitialTeamPlayers(winningTeam.equals("RED") ? "BLUE" : "RED");
        if (winners.isEmpty() || losers.isEmpty()) return 0;
        
        double avgWinnerElo = winners.stream().mapToDouble(u -> plugin.getStatsManager().getStats(u).getRankedElo()).average().orElse(1000);
        double avgLoserElo = losers.stream().mapToDouble(u -> plugin.getStatsManager().getStats(u).getRankedElo()).average().orElse(1000);
        
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (avgLoserElo - avgWinnerElo) / 400.0));
        int eloChange = Math.max(10, (int) Math.round(32 * (1.0 - expectedWinner)));
        
        winners.forEach(u -> updatePlayerElo(u, eloChange));
        losers.forEach(u -> updatePlayerElo(u, -eloChange));
        return eloChange;
    }

    private void updatePlayerElo(UUID uuid, int change) {
        me.molfordan.bridgefightplugin.object.PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        stats.setRankedElo(Math.max(0, stats.getRankedElo() + change));
        if (stats.getRankedElo() > stats.getPeakElo()) stats.setPeakElo(stats.getRankedElo());
        plugin.getStatsManager().savePlayer(stats);
    }

    public void removePlayerFromSession(Player player) {
        removePlayerFromSession(player.getUniqueId(), getSession(player));
    }

    public void removePlayerFromSession(UUID uuid, BedFightSession targetSession) {
        if (targetSession != null) {
            targetSession.getPlayersByTeam("RED").remove(uuid);
            targetSession.getPlayersByTeam("BLUE").remove(uuid);
            targetSession.getSpectators().remove(uuid);
        }
        
        // Only remove from global maps if the associated session is the one we're targeting
        if (playerSessionMap.get(uuid) == targetSession) {
            playerSessionMap.remove(uuid);
        }
        if (spectatorSessionMap.get(uuid) == targetSession) {
            spectatorSessionMap.remove(uuid);
        }
    }

    public BedFightSession getSession(Player player) {
        return playerSessionMap.getOrDefault(player.getUniqueId(), spectatorSessionMap.get(player.getUniqueId()));
    }

    public boolean isInMatch(Player player) { return getSession(player) != null; }
    public Collection<BedFightSession> getAllActiveArenas() { return activeSessions.values(); }
    public Collection<Arena> getAllArenas() { return plugin.getBedFightArenaManager().getArenas(); }
}
