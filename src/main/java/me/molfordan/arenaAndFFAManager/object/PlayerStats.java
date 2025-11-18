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

    public PlayerStats(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
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
        return (double) bridgeKills / bridgeDeaths;
    }

    public double getBuildKDR() {
        if (buildDeaths == 0) {
            return buildKills;
        }
        return (double) buildKills / buildDeaths;
    }
}
