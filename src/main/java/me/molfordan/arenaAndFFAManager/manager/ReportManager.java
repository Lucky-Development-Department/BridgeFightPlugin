package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReportManager {

    private final ArenaAndFFAManager plugin;
    private final File file;
    private FileConfiguration config;

    public ReportManager(ArenaAndFFAManager plugin, File dataFolder) {
        this.plugin = plugin;
        this.file = new File(dataFolder, "reports.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public int createReport(UUID reporter, UUID reported, String reason) {
        int id = getNextReportId();

        config.set("reports." + id + ".reporter", reporter.toString());
        config.set("reports." + id + ".reported", reported.toString());
        config.set("reports." + id + ".reason", reason);
        config.set("reports." + id + ".time", System.currentTimeMillis());

        save();
        return id;
    }

    public Map<Integer, ReportData> getAllReports() {
        Map<Integer, ReportData> map = new LinkedHashMap<>();

        if (!config.contains("reports")) return map;

        for (String key : config.getConfigurationSection("reports").getKeys(false)) {
            int id = Integer.parseInt(key);
            UUID reporter = UUID.fromString(config.getString("reports." + key + ".reporter"));
            UUID reported = UUID.fromString(config.getString("reports." + key + ".reported"));
            String reason = config.getString("reports." + key + ".reason");
            long time = config.getLong("reports." + key + ".time");

            map.put(id, new ReportData(id, reporter, reported, reason, time));
        }
        return map;
    }

    public int getNextReportId() {
        if (!config.contains("reports")) return 1;

        return config.getConfigurationSection("reports").getKeys(false)
                .stream().mapToInt(Integer::parseInt).max().orElse(0) + 1;
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ReportData {
        public final int id;
        public final UUID reporter;
        public final UUID reported;
        public final String reason;
        public final long time;

        public ReportData(int id, UUID reporter, UUID reported, String reason, long time) {
            this.id = id;
            this.reporter = reporter;
            this.reported = reported;
            this.reason = reason;
            this.time = time;
        }
    }

    public void clearAllReports() {
        // Remove the entire "reports" section
        config.set("reports", null);

        // Save the now-empty file
        save();

        // Reload it so getAllReports() sees it's empty
        this.config = YamlConfiguration.loadConfiguration(file);
    }
}
