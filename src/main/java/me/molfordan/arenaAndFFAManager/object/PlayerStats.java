package me.molfordan.arenaAndFFAManager.object;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private String username;

    // --- Totals ---
    private int bridgeKills;
    private int bridgeDeaths;
    private int buildKills;
    private int buildDeaths;

    // --- Streaks ---
    private int bridgeStreak;
    private int bridgeHighestStreak;

    private int buildStreak;
    private int buildHighestStreak;

    // --- Daily Streaks ---
    private int bridgeDailyStreak;
    private int bridgeDailyHighestStreak;

    private int buildDailyStreak;
    private int buildDailyHighestStreak;

    // --- Kit Selection ---
    private String lastSelectedBridgeKit;

    private long lastUpdated;

    public PlayerStats(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public PlayerStats(UUID uuid) {
        this(uuid, "Unknown"); // Calls the existing constructor with a default username
    }

    // -----------------------------
    //          Getters
    // -----------------------------
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }

    public int getBridgeKills() { return bridgeKills; }
    public int getBridgeDeaths() { return bridgeDeaths; }

    public int getBuildKills() { return buildKills; }
    public int getBuildDeaths() { return buildDeaths; }

    public int getBridgeStreak() { return bridgeStreak; }
    public int getBridgeHighestStreak() { return bridgeHighestStreak; }

    public int getBuildStreak() { return buildStreak; }
    public int getBuildHighestStreak() { return buildHighestStreak; }

    public int getBridgeDailyStreak() { return bridgeDailyStreak; }
    public int getBridgeDailyHighestStreak() { return bridgeDailyHighestStreak; }

    public int getBuildDailyStreak() { return buildDailyStreak; }
    public int getBuildDailyHighestStreak() { return buildDailyHighestStreak; }

    public String getLastSelectedBridgeKit() { return lastSelectedBridgeKit; }

    public long getLastUpdated() { return lastUpdated; }
    // -----------------------------
    //          Setters
    // -----------------------------
    public void setUsername(String username) { this.username = username; }

    // Totals are settable (database import)
    public void setBridgeKills(int value) { this.bridgeKills = Math.max(0, value); }
    public void setBridgeDeaths(int value) { this.bridgeDeaths = Math.max(0, value); }

    public void setBuildKills(int value) { this.buildKills = Math.max(0, value); }
    public void setBuildDeaths(int value) { this.buildDeaths = Math.max(0, value); }

    // Streaks (used internally only)
    public void setBridgeStreak(int value) { this.bridgeStreak = Math.max(0, value); }
    public void setBuildStreak(int value) { this.buildStreak = Math.max(0, value); }

    public void setBridgeHighestStreak(int value) { this.bridgeHighestStreak = Math.max(0, value); }
    public void setBuildHighestStreak(int value) { this.buildHighestStreak = Math.max(0, value); }

    // Daily Streaks (used internally only)
    public void setBridgeDailyStreak(int value) { this.bridgeDailyStreak = Math.max(0, value); }
    public void setBuildDailyStreak(int value) { this.buildDailyStreak = Math.max(0, value); }

    public void setBridgeDailyHighestStreak(int value) { this.bridgeDailyHighestStreak = Math.max(0, value); }
    public void setBuildDailyHighestStreak(int value) { this.buildDailyHighestStreak = Math.max(0, value); }

    public void setLastSelectedBridgeKit(String kitName) { this.lastSelectedBridgeKit = kitName; }

    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    // -----------------------------
    //      Increment Functions
    // -----------------------------
    /*
    public void addBridgeKill() {
        bridgeKills++;
        bridgeStreak++;

        if (bridgeStreak > bridgeHighestStreak) {
            bridgeHighestStreak = bridgeStreak;
        }
    }

     */

    public void addBridgeDeath() {
        bridgeDeaths++;
        bridgeStreak = 0;
    }
    /*
    public void addBuildKill() {
        buildKills++;
        buildStreak++;

        if (buildStreak > buildHighestStreak) {
            buildHighestStreak = buildStreak;
        }
    }

     */

    public void addBuildDeath() {
        buildDeaths++;
        buildStreak = 0;
    }

    // -----------------------------
    //      Utility Methods
    // -----------------------------

    public void resetBridgeStreak() {
        bridgeStreak = 0;
    }

    public void resetBuildStreak() {
        buildStreak = 0;
    }

    public void resetAllDailyStreaks() {
        bridgeDailyStreak = 0;
        buildDailyStreak = 0;
    }

    public void addBridgeKill(boolean incrementStreak) {
        bridgeKills++;
        if (incrementStreak) {
            bridgeStreak++;
            if (bridgeStreak > bridgeHighestStreak) {
                bridgeHighestStreak = bridgeStreak;
            }
        }
    }

    public void addBuildKill(boolean incrementStreak) {
        buildKills++;
        if (incrementStreak) {
            buildStreak++;
            if (buildStreak > buildHighestStreak) {
                buildHighestStreak = buildStreak;
            }
        }
    }

    public double getBridgeKDR() {
        if (bridgeDeaths == 0) {
            return bridgeKills;
        }
        double kdr = (double) bridgeKills / bridgeDeaths;
        return Math.round(kdr * 100.0) / 100.0;
    }

    public double getBuildKDR() {
        if (buildDeaths == 0) {
            return buildKills;
        }
        double kdr = (double) buildKills / buildDeaths;
        return Math.round(kdr * 100.0) / 100.0;
    }
}
