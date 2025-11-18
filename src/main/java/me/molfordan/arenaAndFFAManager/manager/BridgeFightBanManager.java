package me.molfordan.arenaAndFFAManager.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BridgeFightBanManager {

    private final File file;
    private final FileConfiguration config;

    private final Map<UUID, BanData> bans = new HashMap<>();

    private final Map<UUID, Integer> history = new HashMap<>();

    public BridgeFightBanManager(File dataFolder) {
        this.file = new File(dataFolder, "BridgeFightBan.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
                Bukkit.getLogger().info("[BridgeFight] Created BridgeFightBan.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        loadBans();
    }

    public static class BanData {
        public final long expireAt;  // -1 = permanent
        public final String reason;

        public BanData(long expireAt, String reason) {
            this.expireAt = expireAt;
            this.reason = reason;
        }
    }

    // -------------------------------
    // LOADING & SAVING
    // -------------------------------

    private void loadBans() {
        // Load active bans
        if (config.isConfigurationSection("bans")) {
            for (String uuidString : config.getConfigurationSection("bans").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                long expire = config.getLong("bans." + uuidString + ".expire", -1);
                String reason = config.getString("bans." + uuidString + ".reason", "BridgeFight rules violation");

                bans.put(uuid, new BanData(expire, reason));
            }
        }

        // Load history
        if (config.isConfigurationSection("history")) {
            for (String uuidString : config.getConfigurationSection("history").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                int count = config.getInt("history." + uuidString, 0);
                history.put(uuid, count);
            }
        }
    }
    private void saveBans() {
        config.set("bans", null);
        config.set("history", null);

        for (Map.Entry<UUID, BanData> entry : bans.entrySet()) {
            UUID uuid = entry.getKey();
            BanData data = entry.getValue();

            config.set("bans." + uuid + ".expire", data.expireAt);
            config.set("bans." + uuid + ".reason", data.reason);
        }

        for (Map.Entry<UUID, Integer> entry : history.entrySet()) {
            config.set("history." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, BanData> getBans() {
        return bans;
    }


    // -------------------------------
    // BAN LOGIC
    // -------------------------------

    public void ban(UUID uuid, long durationMillis, String reason) {
        long expireAt = (durationMillis == -1) ? -1 : System.currentTimeMillis() + durationMillis;

        bans.put(uuid, new BanData(expireAt, reason));

        // Increment ban history
        history.put(uuid, history.getOrDefault(uuid, 0) + 1);

        saveBans();
    }

    public void unban(UUID uuid) {
        bans.remove(uuid);
        saveBans();
    }

    public boolean isPlayerBanned(UUID uuid) {
        if (!bans.containsKey(uuid)) return false;

        BanData data = bans.get(uuid);

        // permanent ban
        if (data.expireAt == -1) return true;

        // expired?
        if (System.currentTimeMillis() > data.expireAt) {
            unban(uuid);
            return false;
        }

        return true;
    }

    public int getBanHistory(UUID uuid) {
        return history.getOrDefault(uuid, 0);
    }


    public String getBanReason(UUID uuid) {
        BanData data = bans.get(uuid);
        return data == null ? null : data.reason;
    }

    public long getBanExpire(UUID uuid) {
        BanData data = bans.get(uuid);
        return data == null ? -1 : data.expireAt;
    }
}
