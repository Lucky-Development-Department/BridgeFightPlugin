package me.molfordan.arenaAndFFAManager.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LeaderboardPlaceholderExpansion extends PlaceholderExpansion {

    private final ArenaAndFFAManager plugin;
    private final StatsManager statsManager;

    private final Map<String, List<LBEntry>> leaderboardCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0;

    public class LBEntry {
        public UUID uuid;
        public String name;
        public int value;

        public LBEntry(UUID uuid, String name, int value) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }
    }

    public LeaderboardPlaceholderExpansion(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public @NotNull String getIdentifier() { return "arena"; }

    @Override
    public @NotNull String getAuthor() { return "Molfordan"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    // ========================================================================
    // CACHE UPDATE (EVERY 10s)
    // ========================================================================
    public void updateLeaderboardCache() {

        if (System.currentTimeMillis() - lastUpdate < 10_000)
            return;

        lastUpdate = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {

                Map<String, List<LBEntry>> newCache = new HashMap<>();
                List<PlayerStats> allPlayers = new ArrayList<>(statsManager.getAllPlayers());

                List<PlayerStats> stats = new ArrayList<>(allPlayers);

                // --- BRIDGE ---
                newCache.put("bridge_kills", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBridgeKills(); }
                }));

                newCache.put("bridge_deaths", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBridgeDeaths(); }
                }));

                newCache.put("bridge_highest_streak", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBridgeHighestStreak(); }
                }));

                newCache.put("bridge_streak", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBridgeStreak(); }
                }));

                // --- BUILD ---
                newCache.put("build_kills", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBuildKills(); }
                }));

                newCache.put("build_deaths", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBuildDeaths(); }
                }));

                newCache.put("build_highest_streak", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBuildHighestStreak(); }
                }));

                newCache.put("build_streak", sort(stats, new Getter() {
                    public int get(PlayerStats s) { return s.getBuildStreak(); }
                }));

                leaderboardCache.clear();
                leaderboardCache.putAll(newCache);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error updating leaderboard: " + e.getMessage());
            }
            }
        });
    }

    // Java-8 compatible functional interface
    private interface Getter {
        int get(PlayerStats s);
    }

    private List<LBEntry> sort(List<PlayerStats> list, Getter getter) {
        plugin.debug("Sorting " + list.size() + " players for leaderboard");
        return list.stream()
                .filter(Objects::nonNull)
                .peek(s -> plugin.debug("Processing player: " + s.getUsername() +
                        " (UUID: " + s.getUuid() + ")"))
                .sorted((a, b) -> Integer.compare(getter.get(b), getter.get(a)))
                .map(s -> {
                    UUID uuid = s.getUuid();
                    String name = s.getUsername();
                    plugin.debug("Mapping player: " + name + " (UUID: " + uuid + ")");

                    if (name == null || name.isEmpty()) {
                        name = Bukkit.getOfflinePlayer(uuid).getName();
                        if (name == null) {
                            name = "Unknown";
                            plugin.debug("Could not resolve name for UUID: " + uuid);
                        }
                    }

                    return new LBEntry(uuid, name, getter.get(s));
                })
                .collect(Collectors.toList());
    }


    // ========================================================================
    // PLACEHOLDER HANDLER
    // example:
    // %arena_top_1_bridge_kills_name%
    // %arena_top_1_bridge_kills_value%
    // ========================================================================
    @Override
    public String onPlaceholderRequest(Player player, String params) {

        updateLeaderboardCache();

        if (!params.startsWith("top_")) return null;

        try {
            // top_1_bridge_kills_name
            String[] p = params.split("_");

            int rank = Integer.parseInt(p[1]);
            String type = p[p.length - 1]; // name / value

            // join middle: bridge_kills, build_deaths, etc.
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < p.length - 1; i++) {
                if (i > 2) sb.append("_");
                sb.append(p[i]);
            }

            String key = sb.toString();

            List<LBEntry> list = leaderboardCache.get(key);
            if (list == null || list.isEmpty()) return "";

            if (rank < 1 || rank > list.size()) return "";

            LBEntry entry = list.get(rank - 1);

            if ("name".equalsIgnoreCase(type)) return entry.name;
            if ("value".equalsIgnoreCase(type)) return String.valueOf(entry.value);

        } catch (Exception ignored) {}

        return "";
    }


    public Map<String, List<LBEntry>> getCache() {
        return leaderboardCache;
    }

    private List<LBEntry> getLeaderboardData(String type) {
        return statsManager.getAllPlayers().stream()
                .filter(s -> {
                    int value = 0;
                    switch (type) {
                        case "bridge_kills": value = s.getBridgeKills(); break;
                        case "bridge_deaths": value = s.getBridgeDeaths(); break;
                        case "bridge_streak": value = s.getBridgeStreak(); break;
                        case "bridge_highest": value = s.getBridgeHighestStreak(); break;
                        case "build_kills": value = s.getBuildKills(); break;
                        case "build_deaths": value = s.getBuildDeaths(); break;
                        case "build_streak": value = s.getBuildStreak(); break;
                        case "build_highest": value = s.getBuildHighestStreak(); break;
                    }
                    return value > 0;
                })
                .sorted((a, b) -> {
                    int aVal = 0, bVal = 0;
                    switch (type) {
                        case "bridge_kills": aVal = a.getBridgeKills(); bVal = b.getBridgeKills(); break;
                        case "bridge_deaths": aVal = a.getBridgeDeaths(); bVal = b.getBridgeDeaths(); break;
                        case "bridge_streak": aVal = a.getBridgeStreak(); bVal = b.getBridgeStreak(); break;
                        case "bridge_highest": aVal = a.getBridgeHighestStreak(); bVal = b.getBridgeHighestStreak(); break;
                        case "build_kills": aVal = a.getBuildKills(); bVal = b.getBuildKills(); break;
                        case "build_deaths": aVal = a.getBuildDeaths(); bVal = b.getBuildDeaths(); break;
                        case "build_streak": aVal = a.getBuildStreak(); bVal = b.getBuildStreak(); break;
                        case "build_highest": aVal = a.getBuildHighestStreak(); bVal = b.getBuildHighestStreak(); break;
                    }
                    return Integer.compare(bVal, aVal);
                })
                .limit(10)
                .map(s -> new LBEntry(s.getUuid(), s.getUsername(), getValue(s, type)))
                .collect(Collectors.toList());
    }

    private int getValue(PlayerStats stats, String type) {
        switch (type) {
            case "bridge_kills": return stats.getBridgeKills();
            case "bridge_deaths": return stats.getBridgeDeaths();
            case "bridge_streak": return stats.getBridgeStreak();
            case "bridge_highest": return stats.getBridgeHighestStreak();
            case "build_kills": return stats.getBuildKills();
            case "build_deaths": return stats.getBuildDeaths();
            case "build_streak": return stats.getBuildStreak();
            case "build_highest": return stats.getBuildHighestStreak();
            default: return 0;
        }
    }
}
