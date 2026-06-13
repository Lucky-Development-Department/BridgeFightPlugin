package me.molfordan.bridgefightplugin.queue;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.queue.enums.MatchType;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MatchmakingService {
    private final BridgeFightPlugin plugin;
    private final Map<QueueType, List<QueueEntry>> queues = new EnumMap<>(QueueType.class);
    private final Map<UUID, String> playerQueuedWorld = new ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask matchmakingTask;
    private int tickCounter = 0;

    public MatchmakingService(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        for (QueueType type : QueueType.values()) {
            queues.put(type, new ArrayList<>());
        }
        startMatchmakingTask();
    }

    private static class QueueEntry {
        final Set<UUID> players;
        final long joinTime;
        final int averageElo;

        QueueEntry(Set<UUID> players, int averageElo) {
            this.players = players;
            this.joinTime = System.currentTimeMillis();
            this.averageElo = averageElo;
        }

        public long getWaitTime() {
            return System.currentTimeMillis() - joinTime;
        }

        public int getSize() {
            return players.size();
        }
    }

    public boolean isInWaitingQueue(UUID uuid) {
        return queues.values().stream()
                .flatMap(List::stream)
                .anyMatch(entry -> entry.players.contains(uuid));
    }

    private void startMatchmakingTask() {
        matchmakingTask = new BukkitRunnable() {
            @Override
            public void run() {
                processMatchmaking();

                tickCounter++;
                if (tickCounter >= 100) {
                    tickCounter = 0;
                    sendSearchingMessages();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (matchmakingTask != null) {
            matchmakingTask.cancel();
            matchmakingTask = null;
        }
    }

    private void sendSearchingMessages() {
        for (Map.Entry<QueueType, List<QueueEntry>> queueEntry : queues.entrySet()) {
            QueueType type = queueEntry.getKey();
            for (QueueEntry entry : queueEntry.getValue()) {
                for (UUID uuid : entry.players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        if (type == QueueType.SOLO_RANKED || type == QueueType.DUO_RANKED) {
                            int waitSeconds = (int) (entry.getWaitTime() / 1000);
                            int eloRange = calculateEloRange(waitSeconds);
                            p.sendMessage(ChatColor.YELLOW + "Searching for match (ELO Range: " + 
                                (entry.averageElo - eloRange) + "-" + (entry.averageElo + eloRange) + ")...");
                        } else {
                            p.sendMessage(ChatColor.YELLOW + "Bedfight - Searching for match....");
                        }
                    }
                }
            }
        }
    }

    public void addToQueue(Player player, QueueType type) {
        Set<UUID> members = new HashSet<>();
        
        if (plugin.getPartyManager().isInParty(player)) {
            UUID leaderId = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
            if (!leaderId.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Only the party leader can join the queue!");
                return;
            }
            
            Set<UUID> partyMembers = plugin.getPartyManager().getPartyMembers(leaderId);
            
            if (type.getMatchType() == MatchType.SOLO) {
                player.sendMessage(ChatColor.RED + "You cannot join a solo queue while in a party!");
                return;
            }
            
            if (type.getMatchType() == MatchType.DUO && partyMembers.size() > 2) {
                player.sendMessage(ChatColor.RED + "Duo queue only supports parties of up to 2 players!");
                return;
            }
            
            if (type.getMatchType() == MatchType.PARTY && partyMembers.size() < 2) {
                player.sendMessage(ChatColor.RED + "Party modes require at least 2 players!");
                return;
            }
            
            members.addAll(partyMembers);
        } else {
            members.add(player.getUniqueId());
        }

        // Check if any member is already in queue
        for (UUID uuid : members) {
            if (isInWaitingQueue(uuid)) {
                player.sendMessage(ChatColor.RED + "One or more players are already in a queue!");
                return;
            }
        }

        // Calculate Average ELO
        int totalElo = 0;
        for (UUID uuid : members) {
            totalElo += plugin.getStatsManager().getStats(uuid).getRankedElo();
        }
        int avgElo = totalElo / members.size();

        QueueEntry entry = new QueueEntry(members, avgElo);
        QueueType actualType = (type == QueueType.PARTY_DUO_QUEUE) ? QueueType.DUO_UNRANKED : type;
        queues.get(actualType).add(entry);
        
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (p.getWorld().getName().startsWith("bf_")) {
                    playerQueuedWorld.put(uuid, p.getWorld().getName());
                }
                p.sendMessage(ChatColor.GREEN + "Joined queue: " + type.getDisplayName());
                giveLeaveItem(p);
            }
        }
    }
    
    public String getQueuedWorld(UUID uuid) {
        return playerQueuedWorld.remove(uuid);
    }
    
    public void giveLeaveItem(Player player) {
        plugin.getLogger().info("DEBUG: Setting leave item for " + player.getName());
        ItemStack leaveItem = new ItemStack(Material.BED);
        ItemMeta meta = leaveItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Leave Queue (Right Click)");
            leaveItem.setItemMeta(meta);
        }
        player.getInventory().clear();
        player.getInventory().setItem(8, leaveItem);
        plugin.getLogger().info("DEBUG: Leave item set in slot 8 for " + player.getName());

        updateFlightState(player);
    }


    private void updateFlightState(Player player) {
        boolean hasPerm = player.hasPermission("luckyessentials.fly");
        boolean inBFWorld = player.getWorld().getName().startsWith("bf_");

        if (inBFWorld || hasPerm) {
            player.setAllowFlight(true);
            player.setFlying(true);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }


    public void removeFromQueue(Player player) {
        plugin.getLogger().info("DEBUG: removeFromQueue called for " + player.getName());
        boolean removed = false;
        
        for (List<QueueEntry> list : queues.values()) {
            Iterator<QueueEntry> it = list.iterator();
            while (it.hasNext()) {
                QueueEntry entry = it.next();
                if (entry.players.contains(player.getUniqueId())) {
                    plugin.getLogger().info("DEBUG: Found player " + player.getName() + " in queue, removing entry.");
                    entry.players.forEach(uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendMessage(ChatColor.RED + "Left the queue!");
                            restoreLobbyState(p);
                        }
                    });
                    it.remove();
                    removed = true;
                }
            }
        }
        
        if (!removed) {
            plugin.getLogger().info("DEBUG: Player " + player.getName() + " was not found in any queue.");
        }
    }

    private void restoreLobbyState(Player p) {
        String buildFFAWorld = plugin.getConfigManager().getBuildFFAWorldName();
        String bridgeFightWorld = plugin.getConfigManager().getBridgeFightWorldName();
        String lobbyWorld = plugin.getConfigManager().getLobbyWorldName();
        
        // Teleport to lobby
        p.teleport(plugin.getConfigManager().getLobbyLocation());
        
        // Clear scoreboard
        p.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard());
        
        if (p.getWorld().getName().equals(buildFFAWorld)) {
            plugin.getKitManager().applyBuildFFAKit(p);
        } else if (p.getWorld().getName().equals(bridgeFightWorld)) {
            plugin.getKitManager().applyBridgeFightKit(p);
        } else if (p.getWorld().getName().equals(lobbyWorld)) {
            plugin.getSpawnItem().giveSpawnItem(p);
        }
    }

    public void processMatchmaking() {
        for (QueueType type : QueueType.values()) {
            if (type == QueueType.PARTY_DUO_QUEUE) continue;
            
            List<QueueEntry> queue = queues.get(type);
            queue.removeIf(entry -> !validateEntry(entry));
            
            if (queue.isEmpty()) continue;

            switch (type.getMatchType()) {
                case SOLO:
                    handleSoloMatching(type, queue);
                    break;
                case DUO:
                    handleDuoMatching(type, queue);
                    break;
                case PARTY:
                    handlePartyMatching(type, queue);
                    break;
            }
        }
    }

    private void handleSoloMatching(QueueType type, List<QueueEntry> queue) {
        if (type.getStatisticType() == me.molfordan.bridgefightplugin.queue.enums.StatisticType.RANKED) {
            matchRanked(type, queue, 2);
        } else {
            matchFIFO(type, queue, 2);
        }
    }

    private void handleDuoMatching(QueueType type, List<QueueEntry> queue) {
        if (type.getStatisticType() == me.molfordan.bridgefightplugin.queue.enums.StatisticType.RANKED) {
            matchRanked(type, queue, 4);
        } else {
            matchFIFO(type, queue, 4);
        }
    }

    private void matchFIFO(QueueType type, List<QueueEntry> queue, int requiredPlayers) {
        List<QueueEntry> selected = new ArrayList<>();
        int currentCount = 0;
        
        for (QueueEntry entry : queue) {
            if (currentCount + entry.getSize() <= requiredPlayers) {
                selected.add(entry);
                currentCount += entry.getSize();
            }
            if (currentCount == requiredPlayers) break;
        }

        if (currentCount == requiredPlayers) {
            queue.removeAll(selected);
            startMatchFromEntries(type, selected);
        }
    }

    private void matchRanked(QueueType type, List<QueueEntry> queue, int requiredPlayers) {
        // Sort by ELO for efficient neighbor searching
        queue.sort(Comparator.comparingInt(e -> e.averageElo));

        for (int i = 0; i < queue.size(); i++) {
            QueueEntry primary = queue.get(i);
            List<QueueEntry> teamMatches = new ArrayList<>();
            teamMatches.add(primary);
            int currentCount = primary.getSize();
            
            int range = calculateEloRange((int) (primary.getWaitTime() / 1000));

            for (int j = 0; j < queue.size(); j++) {
                if (i == j) continue;
                QueueEntry candidate = queue.get(j);
                
                if (Math.abs(primary.averageElo - candidate.averageElo) <= range) {
                    if (currentCount + candidate.getSize() <= requiredPlayers) {
                        teamMatches.add(candidate);
                        currentCount += candidate.getSize();
                    }
                }
                if (currentCount == requiredPlayers) break;
            }

            if (currentCount == requiredPlayers) {
                queue.removeAll(teamMatches);
                startMatchFromEntries(type, teamMatches);
                return;
            }
        }
    }

    private void handlePartyMatching(QueueType type, List<QueueEntry> queue) {
        if (type == QueueType.PARTY_FIGHT) {
            matchPartyFight(queue);
        } else if (type == QueueType.PARTY_SPLIT) {
            matchPartySplit(queue);
        }
    }

    private void matchPartyFight(List<QueueEntry> queue) {
        if (queue.size() < 2) return;
        
        for (int i = 0; i < queue.size(); i++) {
            QueueEntry e1 = queue.get(i);
            for (int j = i + 1; j < queue.size(); j++) {
                QueueEntry e2 = queue.get(j);
                if (Math.abs(e1.getSize() - e2.getSize()) <= 1) {
                    queue.remove(j);
                    queue.remove(i);
                    startMatchFromEntries(QueueType.PARTY_FIGHT, Arrays.asList(e1, e2));
                    return;
                }
            }
        }
    }

    private void matchPartySplit(List<QueueEntry> queue) {
        if (queue.isEmpty()) return;
        QueueEntry entry = queue.remove(0);
        if (entry.getSize() < 2) return;
        
        List<UUID> players = new ArrayList<>(entry.players);
        Collections.shuffle(players);
        
        Set<UUID> redTeam = new HashSet<>();
        Set<UUID> blueTeam = new HashSet<>();
        
        for (int i = 0; i < players.size(); i++) {
            if (i % 2 == 0) redTeam.add(players.get(i));
            else blueTeam.add(players.get(i));
        }
        
        Arena arena = getRandomArena();
        if (arena == null) {
            queue.add(0, entry);
            return;
        }
        
        announceOpponents(redTeam, blueTeam);
        announceMapName(redTeam, blueTeam, arena.getName());
        plugin.getBedFightManager().startMatch(arena, QueueType.PARTY_SPLIT, redTeam, blueTeam);
    }

    private void startMatchFromEntries(QueueType type, List<QueueEntry> entries) {
        Arena arena = getRandomArena();
        if (arena == null) {
            queues.get(type).addAll(0, entries);
            return;
        }

        // Split entries into two teams as evenly as possible
        Set<UUID> team1 = new HashSet<>();
        Set<UUID> team2 = new HashSet<>();
        int team1Size = 0;
        int requiredPerTeam = type.getMinPlayers() / 2;

        for (QueueEntry entry : entries) {
            if (team1Size + entry.getSize() <= requiredPerTeam) {
                team1.addAll(entry.players);
                team1Size += entry.getSize();
            } else {
                team2.addAll(entry.players);
            }
        }

        announceOpponents(team1, team2);
        announceMapName(team1, team2, arena.getName());
        plugin.getBedFightManager().startMatch(arena, type, team1, team2);
    }

    private int calculateEloRange(int secondsWaiting) {
        return 100 + (secondsWaiting / 5) * 40;
    }

    private boolean validateEntry(QueueEntry entry) {
        for (UUID uuid : entry.players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return false;
            if (plugin.getBedFightManager().isInMatch(p)) return false;
        }
        return true;
    }

    private Arena getRandomArena() {
        List<Arena> arenas = new ArrayList<>(plugin.getBedFightArenaManager().getArenas());
        if (arenas.isEmpty()) return null;
        return arenas.get(new Random().nextInt(arenas.size()));
    }

    private void announceMapName(Set<UUID> t1, Set<UUID> t2, String mapName) {
        String msg = ChatColor.YELLOW + "Map: " + ChatColor.GREEN + mapName;
        t1.forEach(u -> { Player p = Bukkit.getPlayer(u); if (p != null) p.sendMessage(msg); });
        t2.forEach(u -> { Player p = Bukkit.getPlayer(u); if (p != null) p.sendMessage(msg); });
    }

    private void announceOpponents(Set<UUID> t1, Set<UUID> t2) {
        String t1Names = t1.stream().map(u -> Bukkit.getOfflinePlayer(u).getName()).collect(Collectors.joining(", "));
        String t2Names = t2.stream().map(u -> Bukkit.getOfflinePlayer(u).getName()).collect(Collectors.joining(", "));

        t1.forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(ChatColor.YELLOW + "Your opponent is " + ChatColor.GREEN + t2Names);
        });
        t2.forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(ChatColor.YELLOW + "Your opponent is " + ChatColor.GREEN + t1Names);
        });
    }

    public void openQueueGUI(Player player) {
        new me.molfordan.bridgefightplugin.queue.QueueGUI(plugin).openMain(player);
    }
}
