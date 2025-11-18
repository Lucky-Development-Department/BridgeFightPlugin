package me.molfordan.arenaAndFFAManager.commands;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.database.DatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.DatabaseManager;
import me.molfordan.arenaAndFFAManager.database.MongoDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;
    private final DatabaseManager databaseManager;

    public LeaderboardCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    // mapping friendly args -> DB field names (SQL/Mongo)
    private String resolveField(String gamemode, String metric) {
        gamemode = gamemode.toLowerCase();
        metric = metric.toLowerCase();

        if (gamemode.equals("bridge")) {
            switch (metric) {
                case "kills":
                    return "bridge_kills";
                case "deaths":
                    return "bridge_deaths";
                case "streak":
                    return "bridge_streak";
                case "highest":
                    return "bridge_highest_streak";
            }
        } else if (gamemode.equals("build")) {
            switch (metric) {
                case "kills":
                    return "build_kills";
                case "deaths":
                    return "build_deaths";
                case "streak":
                    return "build_streak";
                case "highest":
                    return "build_highest_streak";
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /leaderboard <bridge|build> <kills|deaths|streak|highest> [top]");
            return true;
        }

        String gamemode = args[0].toLowerCase();
        String metric = args[1].toLowerCase();
        int top = 10;
        if (args.length >= 3) {
            try {
                top = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        top = Math.max(1, Math.min(100, top)); // clamp 1..100

        String field = resolveField(gamemode, metric);
        if (field == null) {
            sender.sendMessage("§cInvalid gamemode or metric. Gamemodes: bridge, build. Metrics: kills, deaths, streak, highest");
            return true;
        }

        sender.sendMessage("§7Fetching leaderboard...");

        DatabaseConnector connector = databaseManager.getConnector();

        final int limit = top;

        // run DB work async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Entry> results = new ArrayList<>();

            try {
                if (connector instanceof SQLDatabaseConnector) {
                    SQLDatabaseConnector sql = (SQLDatabaseConnector) connector;
                    try (Connection conn = sql.getConnection()) {
                        String sqlQuery = "SELECT uuid, username, " + field +
                                " FROM player_stats ORDER BY " + field +
                                " DESC LIMIT ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlQuery)) {
                            ps.setInt(1, limit); // ← FIXED
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    String uuidStr = rs.getString("uuid");
                                    String username = rs.getString("username");
                                    int value = rs.getInt(field);
                                    UUID uuid = UUID.fromString(uuidStr);
                                    results.add(new Entry(uuid, username != null ? username : uuidStr, value));
                                }
                            }
                        }
                    }

                } else if (connector instanceof MongoDatabaseConnector) {
                    MongoDatabaseConnector mongo = (MongoDatabaseConnector) connector;
                    FindIterable<Document> docs = mongo.getMongoDatabase()
                            .getCollection("player_stats")
                            .find()
                            .sort(new Document(field, -1))
                            .limit(limit); // ← FIXED

                    for (Document d : docs) {
                        String uuidStr = d.getString("uuid");
                        String username = d.getString("username");
                        int value = 0;
                        Object valObj = d.get(field);
                        if (valObj instanceof Number) value = ((Number) valObj).intValue();
                        UUID uuid = UUID.fromString(uuidStr);
                        results.add(new Entry(uuid, username != null ? username : uuidStr, value));
                    }

                } else {
                    // YAML fallback (manual sorting needed)
                    results = results.stream()
                            .sorted(Comparator.comparingInt(Entry::getValue).reversed())
                            .limit(limit) // ← FIXED
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Prepare final list: if some results have username empty, try to get OfflinePlayer name
            for (Entry e : results) {
                if (e.getName() == null || e.getName().isEmpty() || e.getName().equals(e.getUuid().toString())) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(e.getUuid());
                    if (op != null && op.getName() != null) e.setName(op.getName());
                }
            }

            // Send result back on main thread
            final List<Entry> finalResults = results;
            Bukkit.getScheduler().runTask(plugin, () -> {
                String header = String.format("§8§m------------------ §6§lLeaderboard §8§m------------------");
                sender.sendMessage(header);
                String title = String.format("§eTop %d — %s %s", finalResults.size() == 0 ? 0 : finalResults.size(), capitalize(gamemode), capitalize(metric));
                sender.sendMessage(title);

                if (finalResults.isEmpty()) {
                    sender.sendMessage("§cNo entries found.");
                } else {
                    int rank = 1;
                    for (Entry e : finalResults) {
                        sender.sendMessage(String.format(" §7#%d §f%s §8- §a%d", rank, e.getName(), e.getValue()));
                        rank++;
                    }
                }

                sender.sendMessage("§8§m------------------------------------------------");
            });
        });

        return true;
    }

    private static class Entry {
        private final UUID uuid;
        private String name;
        private final int value;

        Entry(UUID uuid, String name, int value) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }

        UUID getUuid() {
            return uuid;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        int getValue() {
            return value;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
