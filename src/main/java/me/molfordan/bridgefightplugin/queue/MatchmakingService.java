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

public class MatchmakingService {
    private final BridgeFightPlugin plugin;
    private final Map<QueueType, List<Set<UUID>>> queues = new EnumMap<>(QueueType.class);
    private final Map<UUID, Long> queueJoinTimes = new HashMap<>();
    private int tickCounter = 0;

    public MatchmakingService(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        for (QueueType type : QueueType.values()) {
            queues.put(type, new ArrayList<>());
        }
        startMatchmakingTask();
    }

    public boolean isInWaitingQueue(UUID uuid) {
        for (List<Set<UUID>> queueList : queues.values()) {
            for (Set<UUID> entry : queueList) {
                if (entry.contains(uuid)) return true;
            }
        }
        return false;
    }

    private void startMatchmakingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processMatchmaking();

                // Send waiting message every 5 seconds (100 ticks)
                tickCounter++;
                if (tickCounter >= 100) {
                    tickCounter = 0;
                    for (Map.Entry<QueueType, List<Set<UUID>>> entry : queues.entrySet()) {
                        QueueType type = entry.getKey();
                        List<Set<UUID>> queue = entry.getValue();
                        for (Set<UUID> players : queue) {
                            for (UUID uuid : players) {
                                Player p = Bukkit.getPlayer(uuid);
                                if (p != null) {
                                    if (type == QueueType.SOLO_RANKED) {
                                        int pElo = plugin.getStatsManager().getStats(uuid).getRankedElo();
                                        long waitTime = System.currentTimeMillis() - queueJoinTimes.getOrDefault(uuid, System.currentTimeMillis());
                                        int secondsWaiting = (int) (waitTime / 1000);
                                        int eloRange = 100 + (secondsWaiting / 5) * 40;
                                        p.sendMessage(ChatColor.YELLOW + "Searching for match (ELO Range: " + (pElo - eloRange) + "-" + (pElo + eloRange) + ")...");
                                    } else {
                                        p.sendMessage(ChatColor.YELLOW + "Bedfight - Searching for match....");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Run every tick
    }

    public void addToQueue(Player player, QueueType type) {
        Set<UUID> entry = new HashSet<>();
        
        // Handle Party
        if (plugin.getPartyManager().isInParty(player)) {
            UUID leaderId = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
            if (!leaderId.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Only the party leader can join the queue!");
                return;
            }
            
            Set<UUID> members = plugin.getPartyManager().getPartyMembers(leaderId);
            
            // Validation based on QueueType
            if (type.getMatchType() == MatchType.SOLO) {
                player.sendMessage(ChatColor.RED + "You cannot join a solo queue while in a party!");
                return;
            }
            
            if (type.getMatchType() == MatchType.DUO && members.size() > 2) {
                player.sendMessage(ChatColor.RED + "Duo queue only supports parties of up to 2 players!");
                return;
            }
            
            if (type.getMatchType() == MatchType.PARTY && members.size() < 2) {
                player.sendMessage(ChatColor.RED + "Party modes require at least 2 players!");
                return;
            }
            
            entry.addAll(members);
        } else {
            // No party: Solo players can join Solo or Duo.
            entry.add(player.getUniqueId());
        }

        // Check if already in queue
        for (List<Set<UUID>> q : queues.values()) {
            for (Set<UUID> e : q) {
                for (UUID uuid : entry) {
                    if (e.contains(uuid)) {
                        player.sendMessage(ChatColor.RED + "One or more players in your party are already in a queue!");
                        return;
                    }
                }
            }
        }

        // Add to queue
        QueueType actualType = (type == QueueType.PARTY_DUO_QUEUE) ? QueueType.DUO_UNRANKED : type;
        queues.get(actualType).add(entry);
        
        long now = System.currentTimeMillis();
        for (UUID uuid : entry) {
            queueJoinTimes.put(uuid, now);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(ChatColor.GREEN + "Joined queue: " + type.getDisplayName());
                giveLeaveItem(p);
            }
        }
    }
    
    public void giveLeaveItem(Player player) {
        ItemStack leaveItem = new ItemStack(Material.BED);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Leave Queue (Right Click)");
        leaveItem.setItemMeta(meta);
        player.getInventory().clear();
        player.getInventory().setItem(8, leaveItem);
    }

    public void removeFromQueue(Player player) {
        UUID leaderId = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
        Set<UUID> toRemove = null;
        
        for (List<Set<UUID>> q : queues.values()) {
            Iterator<Set<UUID>> it = q.iterator();
            while (it.hasNext()) {
                Set<UUID> entry = it.next();
                if (entry.contains(player.getUniqueId())) {
                    toRemove = entry;
                    it.remove();
                    break;
                }
            }
        }
        
        if (toRemove != null) {
            for (UUID uuid : toRemove) {
                queueJoinTimes.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(ChatColor.RED + "Left the queue!");
                    String buildFFAWorld = plugin.getConfigManager().getBuildFFAWorldName();
                    String bridgeFightWorld = plugin.getConfigManager().getBridgeFightWorldName();
                    String lobbyWorld = plugin.getConfigManager().getLobbyWorldName();
                    if (p.getWorld().getName().equals(buildFFAWorld)){
                        plugin.getKitManager().applyBuildFFAKit(p);
                        return;
                    }
                    if (p.getWorld().getName().equals(bridgeFightWorld)){
                        plugin.getKitManager().applyBridgeFightKit(p);
                        return;
                    }
                    if (p.getWorld().getName().equals(lobbyWorld)) {
                        plugin.getSpawnItem().giveSpawnItem(p);
                    }
                }
            }
        }
    }

    public void processMatchmaking() {
        for (QueueType type : QueueType.values()) {
            if (type == QueueType.PARTY_DUO_QUEUE) continue; // Handled as DUO_UNRANKED
            
            List<Set<UUID>> queue = queues.get(type);
            if (queue.isEmpty() || getTotalPlayers(queue) < type.getMinPlayers()) continue;

            if (type == QueueType.SOLO_UNRANKED || type == QueueType.SOLO_RANKED) {
                matchSolo(type, queue);
            } else if (type == QueueType.DUO_UNRANKED || type == QueueType.DUO_RANKED) {
                matchDuo(type, queue);
            } else if (type == QueueType.PARTY_FIGHT) {
                matchPartyFight(queue);
            } else if (type == QueueType.PARTY_SPLIT) {
                matchPartySplit(queue);
            }
        }
    }

    private int getEloRange(long waitTimeMs) {
        int secondsWaiting = (int) (waitTimeMs / 1000);
        return 100 + (secondsWaiting / 5) * 40;
    }

    private void matchSolo(QueueType type, List<Set<UUID>> queue) {
        if (queue.size() < 2) return;

        if (type == QueueType.SOLO_UNRANKED) {
            Set<UUID> e1 = queue.remove(0);
            Set<UUID> e2 = queue.remove(0);

            if (!validateEntry(e1) || !validateEntry(e2)) {
                if (validateEntry(e1)) queue.add(0, e1);
                if (validateEntry(e2)) queue.add(0, e2);
                return;
            }

            Arena arena = getRandomArena();
            if (arena == null) {
                queue.add(0, e2);
                queue.add(0, e1);
                return;
            }
            
            plugin.getBedFightManager().startMatch(arena, type, e1, e2);
        } else if (type == QueueType.SOLO_RANKED) {
            for (int i = 0; i < queue.size(); i++) {
                Set<UUID> e1 = queue.get(i);
                if (!validateEntry(e1)) { queue.remove(i); i--; continue; }
                UUID p1Id = e1.iterator().next();
                
                long waitTime = System.currentTimeMillis() - queueJoinTimes.getOrDefault(p1Id, System.currentTimeMillis());
                int eloRange = getEloRange(waitTime);
                int p1Elo = plugin.getStatsManager().getStats(p1Id).getRankedElo();

                for (int j = i + 1; j < queue.size(); j++) {
                    Set<UUID> e2 = queue.get(j);
                    if (!validateEntry(e2)) { queue.remove(j); j--; continue; }
                    UUID p2Id = e2.iterator().next();

                    int p2Elo = plugin.getStatsManager().getStats(p2Id).getRankedElo();
                    if (Math.abs(p1Elo - p2Elo) <= eloRange) {
                        queue.remove(j);
                        queue.remove(i);
                        
                        Arena arena = getRandomArena();
                        if (arena == null) {
                            queue.add(0, e2);
                            queue.add(0, e1);
                            return;
                        }
                        
                        plugin.getBedFightManager().startMatch(arena, type, e1, e2);
                        return;
                    }
                }
            }
        }
    }

    private void matchDuo(QueueType type, List<Set<UUID>> queue) {
        if (getTotalPlayers(queue) < 4) return;

        boolean isRanked = type == QueueType.DUO_RANKED;
        
        List<Set<UUID>> team1Entries = new ArrayList<>();
        List<Set<UUID>> team2Entries = new ArrayList<>();
        int team1Size = 0;
        int team2Size = 0;

        // Simplified: Pick first entries for team 1, then search for suitable opponents for team 2
        Iterator<Set<UUID>> it = queue.iterator();
        Set<UUID> e1 = null;
        while (it.hasNext()) {
            Set<UUID> entry = it.next();
            if (!validateEntry(entry)) { it.remove(); continue; }
            if (team1Size + entry.size() <= 2) {
                team1Entries.add(entry);
                team1Size += entry.size();
                it.remove();
            }
            if (team1Size == 2) break;
        }

        if (team1Size != 2) {
            queue.addAll(0, team1Entries);
            return;
        }

        // Search for team 2
        Iterator<Set<UUID>> it2 = queue.iterator();
        while (it2.hasNext()) {
            Set<UUID> entry = it2.next();
            if (!validateEntry(entry)) { it2.remove(); continue; }
            
            if (isRanked) {
                // ELO validation
                long waitTime = 0; // Simplified for team ELO: use oldest player wait time
                for(UUID u : team1Entries.get(0)) waitTime = Math.max(waitTime, System.currentTimeMillis() - queueJoinTimes.getOrDefault(u, System.currentTimeMillis()));
                int eloRange = getEloRange(waitTime);
                
                int team1Elo = 0;
                for(Set<UUID> e : team1Entries) for(UUID u : e) team1Elo += plugin.getStatsManager().getStats(u).getRankedElo();
                team1Elo /= team1Size;
                
                int team2Elo = 0;
                for(UUID u : entry) team2Elo += plugin.getStatsManager().getStats(u).getRankedElo();
                team2Elo /= entry.size();
                
                if (Math.abs(team1Elo - team2Elo) > eloRange) continue;
            }

            if (team2Size + entry.size() <= 2) {
                team2Entries.add(entry);
                team2Size += entry.size();
                it2.remove();
            }
            if (team2Size == 2) break;
        }

        if (team2Size == 2) {
            Set<UUID> team1 = new HashSet<>();
            for (Set<UUID> e : team1Entries) team1.addAll(e);
            Set<UUID> team2 = new HashSet<>();
            for (Set<UUID> e : team2Entries) team2.addAll(e);

            Arena arena = getRandomArena();
            if (arena == null) {
                queue.addAll(0, team2Entries);
                queue.addAll(0, team1Entries);
                return;
            }
            plugin.getBedFightManager().startMatch(arena, type, team1, team2);
        } else {
            queue.addAll(0, team1Entries);
        }
    }

    private int getTotalPlayers(List<Set<UUID>> queue) {
        int count = 0;
        for (Set<UUID> set : queue) count += set.size();
        return count;
    }

    private void matchPartyFight(List<Set<UUID>> queue) {
        if (queue.size() < 2) return;
        
        // Simple matching: first two parties with similar sizes (tolerance 1)
        for (int i = 0; i < queue.size(); i++) {
            Set<UUID> e1 = queue.get(i);
            if (!validateEntry(e1)) { queue.remove(i); i--; continue; }
            
            for (int j = i + 1; j < queue.size(); j++) {
                Set<UUID> e2 = queue.get(j);
                if (!validateEntry(e2)) { queue.remove(j); j--; continue; }
                
                if (Math.abs(e1.size() - e2.size()) <= 1) {
                    queue.remove(j);
                    queue.remove(i);
                    
                    Arena arena = getRandomArena();
                    if (arena == null) {
                        queue.add(0, e2);
                        queue.add(0, e1);
                        return;
                    }
                    plugin.getBedFightManager().startMatch(arena, QueueType.PARTY_FIGHT, e1, e2);
                    return;
                }
            }
        }
    }

    private void matchPartySplit(List<Set<UUID>> queue) {
        if (queue.isEmpty()) return;
        Set<UUID> entry = queue.remove(0);
        if (!validateEntry(entry)) return;
        
        if (entry.size() < 2) {
            // Should not happen due to addToQueue checks, but safe
            return;
        }
        
        List<UUID> players = new ArrayList<>(entry);
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
        plugin.getBedFightManager().startMatch(arena, QueueType.PARTY_SPLIT, redTeam, blueTeam);
    }
    
    private boolean validateEntry(Set<UUID> entry) {
        for (UUID uuid : entry) {
            if (Bukkit.getPlayer(uuid) == null) return false;
        }
        return true;
    }

    private Arena getRandomArena() {
        List<Arena> arenas = new ArrayList<>(plugin.getBedFightArenaManager().getArenas());
        if (arenas.isEmpty()) return null;
        return arenas.get(new Random().nextInt(arenas.size()));
    }

    public void openQueueGUI(Player player) {
        new me.molfordan.bridgefightplugin.queue.QueueGUI(plugin).openMain(player);
    }
    }
