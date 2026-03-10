package me.molfordan.arenaAndFFAManager.placeholder;

//import com.mysql.jdbc.PreparedStatement;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import me.molfordan.arenaAndFFAManager.database.*;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        
        // Initialize cache on startup
        Bukkit.getScheduler().runTaskLater(plugin, this::updateLeaderboardCache, 20L);
    }

    @Override
    public @NotNull String getIdentifier() { return "arena"; }

    @Override
    public @NotNull String getAuthor() { return "Molfordan"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    // Also register stats identifier for personal stats
    public String getStatsIdentifier() { return "stats"; }

    // ========================================================================
    // CACHE UPDATE (EVERY 2s)
    // ========================================================================
    public void updateLeaderboardCache() {

        if (System.currentTimeMillis() - lastUpdate < 2_000)
            return;

        lastUpdate = System.currentTimeMillis();

        try {

            Map<String, List<LBEntry>> newCache = new HashMap<>();
            
            //plugin.debug("[Leaderboard] Updating cache - getting top players from database");
            
            // Get top players from database for each category
            List<PlayerStats> bridgeKills = statsManager.getTopPlayers(ArenaType.BRIDGE, "kills", 100);
            List<PlayerStats> bridgeDeaths = statsManager.getTopPlayers(ArenaType.BRIDGE, "deaths", 100);
            List<PlayerStats> bridgeHighestStreak = statsManager.getTopPlayers(ArenaType.BRIDGE, "highest_streak", 100);
            List<PlayerStats> bridgeCurrentStreak = statsManager.getTopPlayers(ArenaType.BRIDGE, "streak", 100);
            
            List<PlayerStats> buildKills = statsManager.getTopPlayers(ArenaType.BUILD, "kills", 100);
            List<PlayerStats> buildDeaths = statsManager.getTopPlayers(ArenaType.BUILD, "deaths", 100);
            List<PlayerStats> buildHighestStreak = statsManager.getTopPlayers(ArenaType.BUILD, "highest_streak", 100);
            List<PlayerStats> buildCurrentStreak = statsManager.getTopPlayers(ArenaType.BUILD, "streak", 100);
/*
            //plugin.debug("[Leaderboard] Retrieved from database - bridgeKills: " + bridgeKills.size() + 
            //            ", bridgeDeaths: " + bridgeDeaths.size() + ", bridgeHighestStreak: " + bridgeHighestStreak.size() +
            //            ", bridgeCurrentStreak: " + bridgeCurrentStreak.size() + ", buildKills: " + buildKills.size() + 
            //            ", buildDeaths: " + buildDeaths.size() + ", buildHighestStreak: " + buildHighestStreak.size() + 
            //            ", buildCurrentStreak: " + buildCurrentStreak.size());


 */
            // Bridge leaderboards
            newCache.put("bridge_kills", convertToLBEntries(bridgeKills, 
                s -> s.getBridgeKills()));
            newCache.put("bridge_deaths", convertToLBEntries(bridgeDeaths, 
                s -> s.getBridgeDeaths()));
            newCache.put("bridge_highest_streak", convertToLBEntries(bridgeHighestStreak, 
                s -> s.getBridgeHighestStreak()));
            newCache.put("bridge_streak", convertToLBEntries(bridgeCurrentStreak, 
                s -> s.getBridgeStreak()));

            // Build leaderboards
            newCache.put("build_kills", convertToLBEntries(buildKills, 
                s -> s.getBuildKills()));
            newCache.put("build_deaths", convertToLBEntries(buildDeaths, 
                s -> s.getBuildDeaths()));
            newCache.put("build_highest_streak", convertToLBEntries(buildHighestStreak, 
                s -> s.getBuildHighestStreak()));
            newCache.put("build_streak", convertToLBEntries(buildCurrentStreak, 
                s -> s.getBuildStreak()));

            //plugin.debug("[Leaderboard] Cache keys created: " + String.join(", ", newCache.keySet()));

            leaderboardCache.clear();
            leaderboardCache.putAll(newCache);

            //plugin.debug("[Leaderboard] Cache updated with " + newCache.size() + " categories");

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update leaderboard cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<LBEntry> convertToLBEntries(List<PlayerStats> stats, java.util.function.Function<PlayerStats, Integer> getter) {
        return stats.stream()
                .limit(100)
                .map(s -> new LBEntry(s.getUuid(), s.getUsername(), getter.apply(s)))
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PLACEHOLDER REQUEST
    // ========================================================================
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {

        //plugin.debug("[Leaderboard] === PLACEHOLDER REQUEST ===");
        //plugin.debug("[Leaderboard] Raw identifier: '" + identifier + "'");
        //plugin.debug("[Leaderboard] Requested by player: " + (player != null ? player.getName() : "null"));
        //plugin.debug("[Leaderboard] Identifier length: " + identifier.length());
        //plugin.debug("[Leaderboard] Starts with 'top_': " + identifier.startsWith("top_"));
        //plugin.debug("[Leaderboard] Starts with 'stats_': " + identifier.startsWith("stats_"));

        // Update cache if needed
        updateLeaderboardCache();

        // -------------------------------------------------------------------
        // PERSONAL STATS (new format: %stats_<type>_<stat>%)
        // -------------------------------------------------------------------
        if (identifier.startsWith("stats_")) {
            if (player == null) return "N/A";

            PlayerStats stats = statsManager.getStats(player.getUniqueId());
            if (stats == null) return "N/A";

            String[] parts = identifier.split("_");
            if (parts.length < 3) return "Invalid format";

            String type = parts[1]; // bridge or build
            String stat = parts[2]; // kills, deaths, highest, kda, streak

            if (type.equals("bridge")) {
                switch (stat) {
                    case "kills": return String.valueOf(stats.getBridgeKills());
                    case "deaths": return String.valueOf(stats.getBridgeDeaths());
                    case "highest": return String.valueOf(stats.getBridgeHighestStreak());
                    case "streak": return String.valueOf(stats.getBridgeStreak());
                    case "kda": {
                        int deaths = stats.getBridgeDeaths();
                        if (deaths == 0) return String.valueOf(stats.getBridgeKills());
                        double kda = (double) stats.getBridgeKills() / deaths;
                        return String.format("%.2f", kda);
                    }
                    default: return "N/A";
                }
            } else if (type.equals("build")) {
                switch (stat) {
                    case "kills": return String.valueOf(stats.getBuildKills());
                    case "deaths": return String.valueOf(stats.getBuildDeaths());
                    case "highest": return String.valueOf(stats.getBuildHighestStreak());
                    case "streak": return String.valueOf(stats.getBuildStreak());
                    case "kda": {
                        int deaths = stats.getBuildDeaths();
                        if (deaths == 0) return String.valueOf(stats.getBuildKills());
                        double kda = (double) stats.getBuildKills() / deaths;
                        return String.format("%.2f", kda);
                    }
                    default: return "N/A";
                }
            }
            return "N/A";
        }

        // -------------------------------------------------------------------
        // LEADERBOARD ENTRIES (new format: %arena_top_<rank>_<type>_<stat>_<name|value>%)
        // -------------------------------------------------------------------
        if (identifier.startsWith("top_")) {
            //plugin.debug("[Leaderboard] Processing TOP_ placeholder");
            
            // Parse: top_<rank>_<type>_<stat>_<name|value>
            // Example: top_1_bridge_highest_streak_name
            String[] parts = identifier.split("_");
            //plugin.debug("[Leaderboard] Split parts: " + Arrays.toString(parts));
            //plugin.debug("[Leaderboard] Parts length: " + parts.length);
            
            if (parts.length < 5) {
                //plugin.debug("[Leaderboard] Invalid format for identifier: " + identifier + 
            //            ". Expected format: top_<rank>_<type>_<stat>_<name|value>");
                return ChatColor.RED + "Invalid format. Use: top_<rank>_<type>_<stat>_<name|value>";
            }

            int rank;
            try {
                rank = Integer.parseInt(parts[1]);
                if (rank < 1) {
                    //plugin.debug("[Leaderboard] Rank must be positive: " + rank + " in identifier: " + identifier);
                    return ChatColor.RED + "Rank must be 1 or higher";
                }
            } catch (NumberFormatException e) {
                //plugin.debug("[Leaderboard] Invalid rank number in identifier: " + identifier + 
            //            ". Rank must be a number.");
                return ChatColor.RED + "Invalid rank number";
            }

            String type = parts[2]; // bridge or build
            
            // Reconstruct stat name from remaining parts (excluding rank, type, and final dataType)
            String dataType = parts[parts.length - 1]; // Last part is always name or value
            StringBuilder statBuilder = new StringBuilder();
            
            // Build stat name from parts[3] to parts[length-2]
            for (int i = 3; i < parts.length - 1; i++) {
                if (i > 3) statBuilder.append("_");
                statBuilder.append(parts[i]);
            }
            String stat = statBuilder.toString();
            
            //plugin.debug("[Leaderboard] Parsed - Type: " + type + ", Stat: " + stat + ", DataType: " + dataType + ", Rank: " + rank);

            // Convert stat name to database format
            String dbStat;
            if (stat.equals("highest")) {
                dbStat = "highest_streak";
            } else if (stat.equals("current")) {
                dbStat = "streak";
            } else {
                dbStat = stat; // Use as-is for "streak", "highest_streak", etc.
            }
            
            String key = type + "_" + dbStat;
            //plugin.debug("[Leaderboard] Stat conversion: " + stat + " -> " + dbStat + " (key: " + key + ")");
            //plugin.debug("[Leaderboard] Available cache keys: " + String.join(", ", leaderboardCache.keySet()));
            List<LBEntry> leaderboard = leaderboardCache.get(key);

            //plugin.debug("[Leaderboard] Requesting: " + key + " rank " + rank + " (" + dataType + ") (cache size: " + 
            //            (leaderboard != null ? leaderboard.size() : "null") + ")");

            // If cache is empty or null, try to update it immediately
            if (leaderboard == null || leaderboard.isEmpty()) {
                //plugin.debug("[Leaderboard] Cache empty, updating immediately");
                lastUpdate = 0; // Force update
                updateLeaderboardCache();
                leaderboard = leaderboardCache.get(key);
            }

            if (leaderboard == null) {
                //plugin.debug("[Leaderboard] No leaderboard data available for: " + key);
                return ChatColor.GRAY + "No data";
            }

            if (leaderboard.isEmpty()) {
                //plugin.debug("[Leaderboard] Empty leaderboard for: " + key);
                return ChatColor.GRAY + "No entries";
            }

            if (rank > leaderboard.size()) {
                //plugin.debug("[Leaderboard] Rank " + rank + " exceeds available entries (" + 
            //            leaderboard.size() + ") for: " + key);
                return ChatColor.GRAY + "Rank " + rank + " (Top " + leaderboard.size() + ")";
            }

            LBEntry entry = leaderboard.get(rank - 1);
            
            if (dataType.equals("name")) {
                String name = entry.name != null && !entry.name.isEmpty() ? entry.name : "Unknown";
                return name;
            } else if (dataType.equals("value")) {
                return String.valueOf(entry.value);
            } else {
                return ChatColor.RED + "Invalid data type. Use: name or value";
            }
        }

        // -------------------------------------------------------------------
        // LEGACY FORMAT SUPPORT (old format for backward compatibility)
        // -------------------------------------------------------------------
        String[] parts = identifier.split("_");
        /*
        if (parts.length < 3) {
            plugin.debug("[Leaderboard] Invalid format for identifier: " + identifier + 
                        ". Expected format: type_stat_rank or stats_type_stat");
            return ChatColor.RED + "Invalid format";
        }

         */

        String type = parts[0]; // bridge or build
        String stat = parts[1]; // kills, deaths, highest_streak
        int rank;

        try {
            rank = Integer.parseInt(parts[2]);
            if (rank < 1) {
                //plugin.debug("[Leaderboard] Rank must be positive: " + rank + " in identifier: " + identifier);
                return ChatColor.RED + "Rank must be 1 or higher";
            }
        } catch (NumberFormatException e) {
            /*
            plugin.debug("[Leaderboard] Invalid rank number in identifier: " + identifier +
                        ". Rank must be a number.");

             */
            return ChatColor.RED + "Invalid rank number";
        }

        String key = type + "_" + stat;
        List<LBEntry> leaderboard = leaderboardCache.get(key);
        /*
        plugin.debug("[Leaderboard] Requesting (legacy): " + key + " rank " + rank + " (cache size: " + 
                   (leaderboard != null ? leaderboard.size() : "null") + ")");

         */

        // If cache is empty or null, try to update it immediately
        if (leaderboard == null || leaderboard.isEmpty()) {
            ////plugin.debug("[Leaderboard] Cache empty, updating immediately");
            lastUpdate = 0; // Force update
            updateLeaderboardCache();
            leaderboard = leaderboardCache.get(key);
        }
        /*

        if (leaderboard == null) {
            //plugin.debug("[Leaderboard] No leaderboard data available for: " + key);
            return ChatColor.GRAY + "No data";
        }

        if (leaderboard.isEmpty()) {
            //plugin.debug("[Leaderboard] Empty leaderboard for: " + key);
            return ChatColor.GRAY + "No entries";
        }

        if (rank > leaderboard.size()) {
            plugin.debug("[Leaderboard] Rank " + rank + " exceeds available entries (" + 
                        leaderboard.size() + ") for: " + key);
            return ChatColor.GRAY + "Rank " + rank + " (Top " + leaderboard.size() + ")";
        }

         */

        LBEntry entry = leaderboard.get(rank - 1);
        String name = entry.name != null && !entry.name.isEmpty() ? entry.name : "Unknown";
        return name + ChatColor.WHITE + ": " + ChatColor.GREEN+ entry.value;
    }

    // ========================================================================
    // UTILITY
    // ========================================================================
    public interface Getter {
        int get(PlayerStats s);
    }

    private List<LBEntry> sort(List<PlayerStats> stats, Getter getter) {
        return stats.stream()
                .sorted((a, b) -> Integer.compare(getter.get(b), getter.get(a)))
                .limit(100)
                .map(s -> new LBEntry(s.getUuid(), s.getUsername(), getter.get(s)))
                .collect(Collectors.toList());
    }

    // Get cached leaderboard data
    public Map<String, List<LBEntry>> getLeaderboardCache() {
        return new HashMap<>(leaderboardCache);
    }

    // Force cache refresh
    public void refreshCache() {
        lastUpdate = 0;
        updateLeaderboardCache();
    }
}
