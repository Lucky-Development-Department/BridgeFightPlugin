package me.molfordan.arenaAndFFAManager.restore;

import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.SerializableBlockState;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DailyArenaRestorer {

    private final ArenaAndFFAManager plugin;
    private final ArenaManager arenaManager;

    private boolean enabled;
    private boolean announce;
    private LocalTime restoreTime;
    private ZoneId zoneId;

    public DailyArenaRestorer(ArenaAndFFAManager plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;

        ensureConfigDefaults();
        loadConfig();
        if (enabled) scheduleNextRestore();
    }

    private void ensureConfigDefaults() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;

        if (!config.contains("daily-restore.enabled")) {
            config.set("daily-restore.enabled", true);
            changed = true;
        }
        if (!config.contains("daily-restore.time")) {
            config.set("daily-restore.time", "3AM");
            changed = true;
        }
        if (!config.contains("daily-restore.timezone")) {
            config.set("daily-restore.timezone", "GMT+7");
            changed = true;
        }
        if (!config.contains("daily-restore.announce")) {
            config.set("daily-restore.announce", true);
            changed = true;
        }

        if (changed) {
            plugin.debug("[DailyArenaRestorer] Added missing default config keys.");
            plugin.saveConfig();
        }
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        enabled = config.getBoolean("daily-restore.enabled", true);
        announce = config.getBoolean("daily-restore.announce", true);

        String timeStr = config.getString("daily-restore.time", "3AM");
        String zoneStr = config.getString("daily-restore.timezone", "GMT+7");

        restoreTime = parseTime(timeStr);
        zoneId = parseZone(zoneStr);

        plugin.saveConfig();
    }

    /**
     * Parse flexible time formats like:
     * "3AM", "7PM", "12:30PM", "03:00", "15:45"
     */
    private LocalTime parseTime(String input) {
        input = input.trim().toUpperCase(Locale.ENGLISH).replaceAll("\\s+", "");
        try {
            // AM/PM support
            if (input.endsWith("AM") || input.endsWith("PM")) {
                DateTimeFormatter fmt = input.contains(":")
                        ? DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
                        : DateTimeFormatter.ofPattern("ha", Locale.ENGLISH);
                return LocalTime.parse(input, fmt);
            }

            // 24-hour fallback
            DateTimeFormatter fmt24 = DateTimeFormatter.ofPattern("H:mm");
            if (!input.contains(":")) input = input + ":00"; // e.g., "15" → "15:00"
            return LocalTime.parse(input, fmt24);

        } catch (Exception e) {
            plugin.debug("[DailyArenaRestorer] Invalid time format: " + input + " — reset to 3AM");
            plugin.getConfig().set("daily-restore.time", "3AM");
            return LocalTime.of(3, 0);
        }
    }

    private ZoneId parseZone(String zoneStr) {
        try {
            return ZoneId.of(zoneStr);
        } catch (Exception e) {
            plugin.debug("[DailyArenaRestorer] Invalid timezone! Reset to GMT+7");
            plugin.getConfig().set("daily-restore.timezone", "GMT+7");
            return TimeZone.getTimeZone("GMT+7").toZoneId();
        }
    }

    private void scheduleNextRestore() {
        long delayTicks = calculateDelayTicks();

        plugin.debug("[DailyArenaRestorer] Next restore scheduled in " +
                (delayTicks / 20 / 60) + " minutes (" + restoreTime + " " + zoneId + ")");

        new BukkitRunnable() {
            @Override
            public void run() {
                performRestoreAll();
                scheduleNextRestore(); // reschedule daily
            }
        }.runTaskLater(plugin, delayTicks);
    }

    private long calculateDelayTicks() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime next = now.withHour(restoreTime.getHour())
                .withMinute(restoreTime.getMinute())
                .withSecond(0)
                .withNano(0);

        if (now.compareTo(next) >= 0) next = next.plusDays(1);

        return Duration.between(now, next).getSeconds() * 20L;
    }



    public void performRestoreAll() {
        plugin.debug("=== [DailyArenaRestorer] Starting daily arena restore ===");

        if (announce)
            Bukkit.broadcastMessage(ChatColor.YELLOW + "[Server] Daily arena reset is running...");

        for (Arena arena : arenaManager.getAllArenas()) {
            if (arena.getOriginalBlocksMap() == null || arena.getOriginalBlocksMap().isEmpty()) continue;

            World world = Bukkit.getWorld(arena.getWorldName());
            if (world == null) {
                plugin.debug("[DailyArenaRestorer] Skipped arena " + arena.getName() + " (world not loaded)");
                continue;
            }

            plugin.debug("[DailyArenaRestorer] Restoring arena: " + arena.getName());

            for (Map.Entry<String, SerializableBlockState> entry : arena.getOriginalBlocksMap().entrySet()) {
                try {
                    String[] coords = entry.getKey().split(",");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);
                    SerializableBlockState s = entry.getValue();

                    Location loc = new Location(world, x, y, z);
                    world.getBlockAt(loc).setType(s.getType());
                    world.getBlockAt(loc).setData(s.getData());
                } catch (Exception ex) {
                    plugin.debug("[DailyArenaRestorer] Failed to restore block at " +
                            entry.getKey() + " in arena " + arena.getName());
                }
            }
        }

        plugin.debug("=== [DailyArenaRestorer] All arenas restored successfully ===");
        if (announce)
            Bukkit.broadcastMessage(ChatColor.GREEN + "[Server] All arenas have been restored!");
    }

    private Location stringToLocation(World world, String s) {
        String[] split = s.split(",");
        if (split.length != 3) return null;
        try {
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
}
