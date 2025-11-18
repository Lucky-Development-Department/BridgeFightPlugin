package me.molfordan.arenaAndFFAManager.restore;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PersistentRestoreManager {
    private final ArenaAndFFAManager plugin;
    private final File file;
    private final Map<String, PendingRestore> restores = Collections.synchronizedMap(new HashMap<>());

    public PersistentRestoreManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pending-restores.yml");
    }

    /* ==========================================================
       ADD / REMOVE RESTORES
    ========================================================== */
    public void addRestore(PendingRestore restore) {
        String key = makeKey(restore.getArenaName(), restore.getLocation());
        restores.put(key, restore);
        plugin.debug("[DEBUG] [PersistentRestore] Added restore for " + key +
                " (" + restore.getMaterial().name() + ")");
    }

    public void removeRestore(String key) {
        restores.remove(key);
        plugin.debug("[DEBUG] [PersistentRestore] Removed restore for " + key);
    }

    /* ==========================================================
       SAVE ALL
    ========================================================== */
    public synchronized void saveAll() {
        if (restores.isEmpty()) return;

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, PendingRestore> e : restores.entrySet()) {
            String key = e.getKey();
            PendingRestore r = e.getValue();

            yaml.set(key + ".arena", r.getArenaName());
            yaml.set(key + ".world", r.getLocation().getWorld().getName());
            yaml.set(key + ".x", r.getLocation().getBlockX());
            yaml.set(key + ".y", r.getLocation().getBlockY());
            yaml.set(key + ".z", r.getLocation().getBlockZ());
            yaml.set(key + ".material", r.getMaterial().name());
            yaml.set(key + ".data", r.getData());
            yaml.set(key + ".timeRemaining", r.getTimeRemaining());
        }

        try {
            yaml.save(file);
            plugin.debug("[DEBUG] [PersistentRestore] Saved " + restores.size() + " pending restores.");
        } catch (IOException ex) {
            plugin.debug("[PersistentRestore] Failed to save: " + ex.getMessage());
        }
    }

    /* ==========================================================
       LOAD ALL
    ========================================================== */
    public void loadAll() {
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int loaded = 0;

        for (String key : yaml.getKeys(false)) {
            try {
                String arena = yaml.getString(key + ".arena");
                String worldName = yaml.getString(key + ".world");
                int x = yaml.getInt(key + ".x");
                int y = yaml.getInt(key + ".y");
                int z = yaml.getInt(key + ".z");
                Material mat = Material.valueOf(yaml.getString(key + ".material"));
                byte data = (byte) yaml.getInt(key + ".data");
                int timeRemaining = yaml.getInt(key + ".timeRemaining");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.debug("[PersistentRestore] Skipped restore " + key + " - world not loaded.");
                    continue;
                }

                PendingRestore restore = new PendingRestore(arena, worldName, x, y, z, mat, data, timeRemaining);
                restores.put(key, restore);
                scheduleRestore(restore, key);
                loaded++;
            } catch (Exception ex) {
                plugin.debug("[PersistentRestore] Failed to load restore: " + key + " (" + ex.getMessage() + ")");
            }
        }

        plugin.debug("[PersistentRestore] Loaded " + loaded + " pending restores from file.");
    }

    /* ==========================================================
       RESTORE SCHEDULING
    ========================================================== */
    private void scheduleRestore(PendingRestore restore, String key) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (restore.getLocation().getWorld() == null) {
                        plugin.debug("[PersistentRestore] Skipping restore - world missing: " + key);
                        return;
                    }
                    restore.getLocation().getBlock().setType(restore.getMaterial());
                    restore.getLocation().getBlock().setData(restore.getData());
                    plugin.debug("[DEBUG] [PersistentRestore] Executed restore at " + key);
                } finally {
                    restores.remove(key);
                }
            }
        }.runTaskLater(plugin, restore.getTimeRemaining() * 20L);
    }

    /* ==========================================================
       AUTO SAVE
    ========================================================== */
    public void startAutoSave() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, 20L * 60, 20L * 60);
    }

    /* ==========================================================
       UTILITIES
    ========================================================== */
    private String makeKey(String arena, org.bukkit.Location loc) {
        return arena + "|" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
