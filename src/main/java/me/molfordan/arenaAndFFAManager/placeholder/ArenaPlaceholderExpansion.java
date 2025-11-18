package me.molfordan.arenaAndFFAManager.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaPlaceholderExpansion extends PlaceholderExpansion {

    private final ArenaAndFFAManager plugin;
    private final StatsManager statsManager;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^([0-9a-fA-F\\-]{36})_(.+)$");

    public ArenaPlaceholderExpansion(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public String getIdentifier() {
        return "stats";
    }

    @Override
    public String getAuthor() {
        try {
            if (plugin.getDescription() != null &&
                    !plugin.getDescription().getAuthors().isEmpty()) {

                return plugin.getDescription().getAuthors().get(0);
            }
        } catch (Exception ignored) {}
        return "Molfordan";
    }

    @Override
    public String getVersion() {
        try {
            return plugin.getDescription().getVersion();
        } catch (Exception ignored) {}
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (params == null || params.isEmpty())
            return "";

        // UUID based: <uuid>_<key>
        Matcher matcher = UUID_PATTERN.matcher(params);
        if (matcher.matches()) {

            String uuidStr = matcher.group(1);
            String key     = matcher.group(2);

            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerStats stats = loadStats(uuid);
                return resolve(stats, key);

            } catch (IllegalArgumentException ex) {
                return "";
            }
        }

        // If requesting player's own stats
        if (player != null) {
            PlayerStats stats = loadStats(player.getUniqueId());
            return resolve(stats, params);
        }

        return "";
    }

    private PlayerStats loadStats(UUID uuid) {
        PlayerStats stats = statsManager.getStats(uuid);
        if (stats != null) return stats;

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = (op != null && op.getName() != null)
                ? op.getName()
                : uuid.toString();

        // blocking load is allowed because PlaceholderAPI is async-safe
        return statsManager.loadPlayer(uuid, name);
    }

    private String resolve(PlayerStats s, String key) {
        if (s == null) return "0";

        switch (key.toLowerCase()) {

            // Bridge Fight
            case "bridge_kills":
                return String.valueOf(s.getBridgeKills());
            case "bridge_deaths":
                return String.valueOf(s.getBridgeDeaths());
            case "bridge_streak":
            case "bridge_current_streak":
                return String.valueOf(s.getBridgeStreak());
            case "bridge_highest":
            case "bridge_highest_streak":
                return String.valueOf(s.getBridgeHighestStreak());
            case "bridge_kda":
                return String.valueOf(s.getBridgeKDR());

            // Build FFA
            case "build_kills":
                return String.valueOf(s.getBuildKills());
            case "build_deaths":
                return String.valueOf(s.getBuildDeaths());
            case "build_streak":
            case "build_current_streak":
                return String.valueOf(s.getBuildStreak());
            case "build_highest":
            case "build_highest_streak":
                return String.valueOf(s.getBuildHighestStreak());
            case "build_kda":
                return String.valueOf(s.getBuildKDR());
        }

        return "";
    }
}
