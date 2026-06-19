package me.molfordan.bridgefightplugin.object;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
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

    // --- Cosmetics ---
    private String selectedKillMessage = "default";
    private String selectedKillEffect = "none";
    private String selectedTrail = "none";
    private Set<String> purchasedKillMessages = new LinkedHashSet<>();
    private Set<String> purchasedKillEffects = new LinkedHashSet<>();
    private Set<String> purchasedTrails = new LinkedHashSet<>();

    // --- Ranked ---
    private int rankedElo = 1000;
    private int peakElo = 1000;
    private int rankedWins, rankedLosses, rankedKills, rankedDeaths, rankedBeds, rankedStreak, rankedHighestStreak;

    // --- BedFight Specific ---
    private int bedFightBedBreaks = 0;
    public int getBedFightBedBreaks() { return bedFightBedBreaks; }
    public void setBedFightBedBreaks(int breaks) { this.bedFightBedBreaks = Math.max(0, breaks); }


    // --- Unranked ---
    private int unrankedWins, unrankedLosses, unrankedKills, unrankedDeaths, unrankedBeds, unrankedStreak, bestUnrankedStreak;
    
    // --- Duel ---
    private int duelWins, duelLosses;

    private int coins = 0;

    private long lastUpdated;

    public PlayerStats(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.rankedElo = 1000;
        this.peakElo = 1000;
        this.coins = 0;
        addDefaultCosmetics();
    }

    public PlayerStats(UUID uuid) {
        this(uuid, "Unknown");
    }

    // -----------------------------
    //          Getters
    // -----------------------------
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }

    public int getCoins() { return coins; }

    // --- Original Stats (Legacy/Kept) ---
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

    // --- Cosmetics ---
    public String getSelectedKillMessage() { return selectedKillMessage; }
    public String getSelectedKillEffect() { return selectedKillEffect; }
    public String getSelectedTrail() { return selectedTrail; }
    public Set<String> getPurchasedKillMessages() {
        ensureCosmeticSets();
        return purchasedKillMessages;
    }
    public Set<String> getPurchasedKillEffects() {
        ensureCosmeticSets();
        return purchasedKillEffects;
    }
    public Set<String> getPurchasedTrails() {
        ensureCosmeticSets();
        return purchasedTrails;
    }

    // --- New Ranked Stats ---
    public int getRankedElo() { return rankedElo; }
    public int getPeakElo() { return peakElo; }
    public int getRankedWins() { return rankedWins; }
    public int getRankedLosses() { return rankedLosses; }
    public int getRankedKills() { return rankedKills; }
    public int getRankedDeaths() { return rankedDeaths; }
    public int getRankedBeds() { return rankedBeds; }
    public int getRankedStreak() { return rankedStreak; }
    public int getRankedHighestStreak() { return rankedHighestStreak; }

    // --- New Unranked Stats ---
    public int getUnrankedWins() { return unrankedWins; }
    public int getUnrankedLosses() { return unrankedLosses; }
    public int getUnrankedKills() { return unrankedKills; }
    public int getUnrankedDeaths() { return unrankedDeaths; }
    public int getUnrankedBeds() { return unrankedBeds; }
    public int getUnrankedStreak() { return unrankedStreak; }
    public int getUnrankedHighestStreak() { return bestUnrankedStreak; }
    public int getBestUnrankedStreak() { return bestUnrankedStreak; }
    
    // --- Duel Stats ---
    public int getDuelWins() { return duelWins; }
    public int getDuelLosses() { return duelLosses; }

    public long getLastUpdated() { return lastUpdated; }

    // -----------------------------
    //          Setters
    // -----------------------------
    public void setUsername(String username) { this.username = username; }

    // Legacy Setters
    public void setBridgeKills(int value) { this.bridgeKills = Math.max(0, value); }
    public void setBridgeDeaths(int value) { this.bridgeDeaths = Math.max(0, value); }
    public void setBuildKills(int value) { this.buildKills = Math.max(0, value); }
    public void setBuildDeaths(int value) { this.buildDeaths = Math.max(0, value); }
    public void setBridgeStreak(int value) { this.bridgeStreak = Math.max(0, value); }
    public void setBuildStreak(int value) { this.buildStreak = Math.max(0, value); }
    public void setBridgeHighestStreak(int value) { this.bridgeHighestStreak = Math.max(0, value); }
    public void setBuildHighestStreak(int value) { this.buildHighestStreak = Math.max(0, value); }
    public void setBridgeDailyStreak(int value) { this.bridgeDailyStreak = Math.max(0, value); }
    public void setBuildDailyStreak(int value) { this.buildDailyStreak = Math.max(0, value); }
    public void setBridgeDailyHighestStreak(int value) { this.bridgeDailyHighestStreak = Math.max(0, value); }
    public void setBuildDailyHighestStreak(int value) { this.buildDailyHighestStreak = Math.max(0, value); }
    public void setLastSelectedBridgeKit(String kitName) { this.lastSelectedBridgeKit = kitName; }

    // --- Cosmetics ---
    public void setSelectedKillMessage(String id) { this.selectedKillMessage = id; }
    public void setSelectedKillEffect(String id) { this.selectedKillEffect = id; }
    public void setSelectedTrail(String id) { this.selectedTrail = id; }
    public void setPurchasedKillMessages(Collection<String> ids) {
        this.purchasedKillMessages = cleanCosmeticIds(ids);
        this.purchasedKillMessages.add("default");
    }
    public void setPurchasedKillEffects(Collection<String> ids) {
        this.purchasedKillEffects = cleanCosmeticIds(ids);
        this.purchasedKillEffects.add("none");
    }
    public void setPurchasedTrails(Collection<String> ids) {
        this.purchasedTrails = cleanCosmeticIds(ids);
        this.purchasedTrails.add("none");
    }
    public boolean hasPurchasedKillMessage(String id) {
        ensureCosmeticSets();
        return purchasedKillMessages.contains(id);
    }
    public boolean hasPurchasedKillEffect(String id) {
        ensureCosmeticSets();
        return purchasedKillEffects.contains(id);
    }
    public boolean hasPurchasedTrail(String id) {
        ensureCosmeticSets();
        return purchasedTrails.contains(id);
    }
    public void addPurchasedKillMessage(String id) {
        ensureCosmeticSets();
        addCosmeticId(purchasedKillMessages, id);
    }
    public void addPurchasedKillEffect(String id) {
        ensureCosmeticSets();
        addCosmeticId(purchasedKillEffects, id);
    }
    public void addPurchasedTrail(String id) {
        ensureCosmeticSets();
        addCosmeticId(purchasedTrails, id);
    }

    // Ranked Setters
    public void setRankedElo(int elo) { this.rankedElo = elo; }
    public void setPeakElo(int peakElo) { this.peakElo = peakElo; }
    public void setRankedWins(int wins) { this.rankedWins = Math.max(0, wins); }
    public void setRankedLosses(int losses) { this.rankedLosses = Math.max(0, losses); }
    public void setRankedKills(int kills) { this.rankedKills = Math.max(0, kills); }
    public void setRankedDeaths(int deaths) { this.rankedDeaths = Math.max(0, deaths); }
    public void setRankedBeds(int beds) { this.rankedBeds = Math.max(0, beds); }
    public void setRankedStreak(int streak) { this.rankedStreak = Math.max(0, streak); }
    public void setRankedHighestStreak(int streak) { this.rankedHighestStreak = Math.max(0, streak); }

    // Unranked Setters
    public void setUnrankedWins(int wins) { this.unrankedWins = Math.max(0, wins); }
    public void setUnrankedLosses(int losses) { this.unrankedLosses = Math.max(0, losses); }
    public void setUnrankedKills(int kills) { this.unrankedKills = Math.max(0, kills); }
    public void setUnrankedDeaths(int deaths) { this.unrankedDeaths = Math.max(0, deaths); }
    public void setUnrankedBeds(int beds) { this.unrankedBeds = Math.max(0, beds); }
    public void setUnrankedStreak(int streak) { this.unrankedStreak = Math.max(0, streak); }
    public void setBestUnrankedStreak(int streak) { this.bestUnrankedStreak = Math.max(0, streak); }

    // Duel Setters
    public void setDuelWins(int wins) { this.duelWins = Math.max(0, wins); }
    public void setDuelLosses(int losses) { this.duelLosses = Math.max(0, losses); }

    public void setCoins(int coins) { this.coins = Math.max(0, coins); }


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

    private void addDefaultCosmetics() {
        purchasedKillMessages.add("default");
        purchasedKillEffects.add("none");
        purchasedTrails.add("none");
        addCosmeticId(purchasedKillMessages, selectedKillMessage);
        addCosmeticId(purchasedKillEffects, selectedKillEffect);
        addCosmeticId(purchasedTrails, selectedTrail);
    }

    private void ensureCosmeticSets() {
        if (purchasedKillMessages == null) purchasedKillMessages = new LinkedHashSet<>();
        if (purchasedKillEffects == null) purchasedKillEffects = new LinkedHashSet<>();
        if (purchasedTrails == null) purchasedTrails = new LinkedHashSet<>();
        addDefaultCosmetics();
    }

    private Set<String> cleanCosmeticIds(Collection<String> ids) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (ids == null) return cleaned;

        for (String id : ids) {
            addCosmeticId(cleaned, id);
        }
        return cleaned;
    }

    private void addCosmeticId(Set<String> ids, String id) {
        if (id == null || id.trim().isEmpty()) return;
        ids.add(id.trim());
    }
}
