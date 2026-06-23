package me.molfordan.bridgefightplugin.cosmetics.crate;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.cosmetics.objects.CosmeticTier;
import me.molfordan.bridgefightplugin.cosmetics.objects.KillEffect;
import me.molfordan.bridgefightplugin.cosmetics.objects.KillMessage;
import me.molfordan.bridgefightplugin.cosmetics.objects.Trail;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CrateManager {

    public static final String CRATE_GUI_TITLE = ChatColor.DARK_GRAY + "Rolling Crate...";

    private final BridgeFightPlugin plugin;
    private final CrateConfig config;
    private final List<Location> locations = new ArrayList<>();
    private final Map<Location, Hologram> holograms = new HashMap<>();
    private final Set<UUID> activeRolls = new HashSet<>();
    private final Map<UUID, RolledCosmetic> pendingRewards = new HashMap<>();
    private final Map<UUID, Location> pendingLocations = new HashMap<>();
    private final Map<UUID, Boolean> pendingDuplicates = new HashMap<>();

    private final String[] animationFrames = {
            "&a&lCOSMETICS CRATE",
            "&e&lCOSMETICS CRATE",
            "&f&lCOSMETICS CRATE"
    };
    private int frameIndex = 0;

    public CrateManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.config = new CrateConfig(plugin);
        startAnimationTask();
        // Delay by 1 tick so all worlds (including multiverse worlds) are
        // fully loaded before we try to resolve Bukkit.getWorld(name).
        Bukkit.getScheduler().runTaskLater(plugin, this::loadCrates, 1L);
    }

    public void loadCrates() {
        // Clear all existing DH holograms to prevent duplicates
        for (Hologram holo : holograms.values()) {
            if (holo != null) {
                DHAPI.removeHologram(holo.getName());
            }
        }
        holograms.clear();

        locations.clear();
        config.load();
        locations.addAll(config.getCrateLocations());

        for (Location loc : locations) {
            spawnHologram(loc);
        }
    }

    public void registerCrate(Location loc) {
        if (!locations.contains(loc)) {
            locations.add(loc);
            config.saveCrateLocations(locations);
            spawnHologram(loc);
        }
    }

    public void unregisterCrate(Location loc) {
        locations.remove(loc);
        config.saveCrateLocations(locations);
        Hologram holo = holograms.remove(loc);
        if (holo != null) {
            DHAPI.removeHologram(holo.getName());
        } else {
            String name = getHologramName(loc);
            DHAPI.removeHologram(name);
        }
        cleanupExistingArmorStands(loc);
    }

    public boolean isCrate(Location loc) {
        for (Location l : locations) {
            if (l.getWorld().getName().equals(loc.getWorld().getName())
                    && l.getBlockX() == loc.getBlockX()
                    && l.getBlockY() == loc.getBlockY()
                    && l.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public Location getMatchingCrateLocation(Location loc) {
        for (Location l : locations) {
            if (l.getWorld().getName().equals(loc.getWorld().getName())
                    && l.getBlockX() == loc.getBlockX()
                    && l.getBlockY() == loc.getBlockY()
                    && l.getBlockZ() == loc.getBlockZ()) {
                return l;
            }
        }
        return null;
    }

    private String getHologramName(Location loc) {
        return "crate_holo_" + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public void spawnHologram(Location loc) {
        if (loc.getWorld() == null) {
            plugin.getLogger().warning("spawnHologram() aborted: world is NULL.");
            return;
        }

        // Force-load the chunk first so we can reliably cleanup and spawn
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        boolean wasLoaded = loc.getWorld().isChunkLoaded(chunkX, chunkZ);
        if (!wasLoaded) {
            loc.getWorld().loadChunk(chunkX, chunkZ);
        }

        String name = getHologramName(loc);
        Hologram existing = DHAPI.getHologram(name);
        if (existing != null) {
            DHAPI.removeHologram(name);
        }

        cleanupExistingArmorStands(loc);

        // Spawn DecentHologram with title and click to interact lines
        Location holoLoc = loc.clone().add(0.5, 1.6, 0.5);
        Hologram holo = DHAPI.createHologram(name, holoLoc);
        DHAPI.addHologramLine(holo, ChatColor.translateAlternateColorCodes('&', animationFrames[0]));
        DHAPI.addHologramLine(holo, ChatColor.translateAlternateColorCodes('&', "&8Click to interact"));

        holograms.put(loc, holo);
    }

    private void cleanupTitleArmorStand(Location loc) {
        if (loc.getWorld() == null) return;
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        if (!loc.getWorld().isChunkLoaded(chunkX, chunkZ)) return;

        Location titleArea = loc.clone().add(0.5, 1.55, 0.5);
        for (org.bukkit.entity.Entity entity : loc.getChunk().getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand as = (ArmorStand) entity;
                if (as.getCustomName() != null) {
                    String name = ChatColor.stripColor(as.getCustomName());
                    if (name.contains("COSMETICS") || name.contains("CRATE")) {
                        if (as.getLocation().distanceSquared(titleArea) < 2.0) {
                            as.remove();
                        }
                    }
                }
            }
        }
    }

    private void cleanupSubtitleArmorStand(Location loc) {
        if (loc.getWorld() == null) return;
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        if (!loc.getWorld().isChunkLoaded(chunkX, chunkZ)) return;

        Location subArea = loc.clone().add(0.5, 1.2, 0.5);
        for (org.bukkit.entity.Entity entity : loc.getChunk().getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand as = (ArmorStand) entity;
                if (as.getCustomName() != null) {
                    String name = ChatColor.stripColor(as.getCustomName()).toLowerCase();
                    if (name.contains("click to interact")) {
                        if (as.getLocation().distanceSquared(subArea) < 2.0) {
                            as.remove();
                        }
                    }
                }
            }
        }
    }

    private void cleanupExistingArmorStands(Location loc) {
        cleanupTitleArmorStand(loc);
        cleanupSubtitleArmorStand(loc);
    }

    private void startAnimationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    frameIndex = (frameIndex + 1) % animationFrames.length;
                    String nextFrameName = ChatColor.translateAlternateColorCodes('&', animationFrames[frameIndex]);

                    for (Location loc : locations) {
                        if (loc.getWorld() == null) continue;

                        int chunkX = loc.getBlockX() >> 4;
                        int chunkZ = loc.getBlockZ() >> 4;
                        if (!loc.getWorld().isChunkLoaded(chunkX, chunkZ)) continue;

                        String name = getHologramName(loc);
                        Hologram holo = holograms.get(loc);
                        if (holo == null) {
                            holo = DHAPI.getHologram(name);
                        }

                        if (holo == null) {
                            plugin.getLogger().warning("Hologram " + name + " missing — re-spawning.");
                            cleanupExistingArmorStands(loc);
                            Location holoLoc = loc.clone().add(0.5, 1.6, 0.5);
                            holo = DHAPI.createHologram(name, holoLoc);
                            DHAPI.addHologramLine(holo, nextFrameName);
                            DHAPI.addHologramLine(holo, ChatColor.translateAlternateColorCodes('&', "&8Click to interact"));
                            holograms.put(loc, holo);
                        } else {
                            DHAPI.setHologramLine(holo, 0, nextFrameName);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Exception in crate animation task: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // 5 ticks ≈ 4 frames/second
    }

    public Set<UUID> getActiveRolls() {
        return activeRolls;
    }

    public BridgeFightPlugin getPlugin() {
        return plugin;
    }

    public void startRoll(Player player, Location crateLoc) {
        UUID uuid = player.getUniqueId();
        if (activeRolls.contains(uuid)) {
            player.sendMessage(ChatColor.RED + "Please wait until your current crate opening is finished!");
            return;
        }

        if (!plugin.getBalanceManager().hasBalance(player, 2000)) {
            player.sendMessage(ChatColor.RED + "You need at least 2,000 coins to open a Cosmetics Crate!");
            return;
        }

        // Deduct 2,000 coins immediately to prevent glitches
        plugin.getBalanceManager().removeBalance(player, 2000);
        activeRolls.add(uuid);

        // Decide the rolled cosmetic immediately
        RolledCosmetic rolled = rollCosmetic();
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);

        // Only check purchased set — not free/permission based ownership
        boolean duplicate = isPurchased(stats, rolled);

        if (duplicate) {
            plugin.getBalanceManager().addBalance(player, 1500);
        } else {
            awardCosmetic(stats, rolled);
            plugin.getStatsManager().savePlayer(stats);

            if (rolled.getTypeName().equals("Kill Effect") && plugin.getDeathMessageManager() != null) {
                plugin.getDeathMessageManager().updateKillEffectCache(player);
            } else if (rolled.getTypeName().equals("Trail") && plugin.getCosmeticsListener() != null) {
                plugin.getCosmeticsListener().updatePlayerTrailCache(player);
            }
        }

        pendingRewards.put(uuid, rolled);
        pendingLocations.put(uuid, crateLoc);
        pendingDuplicates.put(uuid, duplicate);

        // Pre-generate conveyor sequence (34 items: won item at index 29)
        List<RolledCosmetic> seq = new ArrayList<>();
        for (int i = 0; i < 34; i++) {
            if (i == 29) {
                seq.add(rolled);
            } else {
                seq.add(getRandomCosmeticFromPool());
            }
        }

        // Create and open 3-row GUI
        Inventory inv = Bukkit.createInventory(null, 27, CRATE_GUI_TITLE);
        player.openInventory(inv);

        // Start dynamic conveyor steps
        scheduleNextStep(player, inv, seq, 0, crateLoc);
    }

    private void scheduleNextStep(Player player, Inventory inv, List<RolledCosmetic> seq, int step, Location crateLoc) {
        UUID uuid = player.getUniqueId();
        if (!player.isOnline() || !activeRolls.contains(uuid)) {
            activeRolls.remove(uuid);
            pendingRewards.remove(uuid);
            pendingLocations.remove(uuid);
            pendingDuplicates.remove(uuid);
            return;
        }

        // 1. Randomize colorful glass panes in rows 1 and 3
        int[] colorfulData = { 1, 2, 3, 4, 5, 6, 9, 10, 11, 14 };
        Random random = new Random();
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                inv.setItem(i, createItem(Material.ARROW, ChatColor.RED+ "↓ Indicator ↓"));
                inv.setItem(i + 18, createItem(Material.ARROW, ChatColor.RED + "↑ Indicator ↑"));
            } else {
                int color1 = colorfulData[random.nextInt(colorfulData.length)];
                int color2 = colorfulData[random.nextInt(colorfulData.length)];
                inv.setItem(i, createItem(Material.STAINED_GLASS_PANE, color1, " "));
                inv.setItem(i + 18, createItem(Material.STAINED_GLASS_PANE, color2, " "));
            }
        }

        // 2. Shift row 2 items
        for (int i = 0; i < 9; i++) {
            RolledCosmetic rc = seq.get(step + i);
            inv.setItem(9 + i, createCosmeticItemStack(rc));
        }

        player.updateInventory();
        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

        if (step < 25) {
            // Easing delay logic
            int delay;
            if (step < 10) {
                delay = 2;
            } else if (step < 15) {
                delay = 3;
            } else if (step < 18) {
                delay = 4;
            } else if (step < 21) {
                delay = 6;
            } else if (step < 23) {
                delay = 8;
            } else {
                delay = 12;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    scheduleNextStep(player, inv, seq, step + 1, crateLoc);
                }
            }.runTaskLater(plugin, delay);
        } else {
            finishRoll(player, seq.get(29), crateLoc);
        }
    }

    public void finishRoll(Player player, RolledCosmetic rolled, Location crateLoc) {
        UUID uuid = player.getUniqueId();
        if (!activeRolls.contains(uuid)) return;

        // Read the pre-decided duplicate flag set atomically in startRoll
        boolean duplicate = Boolean.TRUE.equals(pendingDuplicates.remove(uuid));

        pendingRewards.remove(uuid);
        pendingLocations.remove(uuid);
        activeRolls.remove(uuid);

        player.closeInventory();

        player.sendTitle(
                duplicate ? ChatColor.GOLD + "" + ChatColor.BOLD + "DUPLICATE!" : ChatColor.GREEN + "" + ChatColor.BOLD + "UNLOCKED!",
                rolled.getTier().getColor() + ChatColor.translateAlternateColorCodes('&', rolled.getDisplayName())
        );

        if (duplicate) {
            player.playSound(player.getLocation(), Sound.LAVA_POP, 1.0f, 0.8f);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e&lCRATE! &7You rolled the duplicate cosmetic: " + rolled.getTier().getColor() + rolled.getDisplayName() + " &7(Already owned!)."));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&lREFUND! &7Since you already owned it, you have been refunded &a1,500 coins&7!"));
        } else {
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e&lCRATE! &7You rolled the cosmetic: " + rolled.getTier().getColor() + rolled.getDisplayName() + " &7(&aNEW!&7)."));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&lUNLOCKED! &7You can now equip this cosmetic in &e/cosmetics&7!"));
        }

        spawnUnlockParticles(crateLoc, rolled.getTier());
    }

    public void forceCompleteRoll(Player player) {
        UUID uuid = player.getUniqueId();
        if (!activeRolls.contains(uuid)) return;

        RolledCosmetic rolled = pendingRewards.remove(uuid);
        Location crateLoc = pendingLocations.remove(uuid);
        boolean duplicate = Boolean.TRUE.equals(pendingDuplicates.remove(uuid));
        activeRolls.remove(uuid);

        if (rolled != null && crateLoc != null) {

            player.sendTitle(
                    duplicate ? ChatColor.GOLD + "" + ChatColor.BOLD + "DUPLICATE!" : ChatColor.GREEN + "" + ChatColor.BOLD + "UNLOCKED!",
                    rolled.getTier().getColor() + ChatColor.translateAlternateColorCodes('&', rolled.getDisplayName())
            );

            if (duplicate) {
                player.playSound(player.getLocation(), Sound.LAVA_POP, 1.0f, 0.8f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lCRATE! &7You rolled the duplicate cosmetic: " + rolled.getTier().getColor() + rolled.getDisplayName() + " &7(Already owned!)."));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a&lREFUND! &7Since you already owned it, you have been refunded &a1,500 coins&7!"));
            } else {
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lCRATE! &7You rolled the cosmetic: " + rolled.getTier().getColor() + rolled.getDisplayName() + " &7(&aNEW!&7)."));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a&lUNLOCKED! &7You can now equip this cosmetic in &e/cosmetics&7!"));
            }

            spawnUnlockParticles(crateLoc, rolled.getTier());
        }
    }

    private RolledCosmetic rollCosmetic() {
        double rand = Math.random() * 100.0;
        CosmeticTier selectedTier;
        if (rand < 0.5) {
            selectedTier = CosmeticTier.MYTHIC;
        } else if (rand < 1.5) {
            selectedTier = CosmeticTier.LEGENDARY;
        } else if (rand < 3.0) {
            selectedTier = CosmeticTier.EPIC;
        } else if (rand < 15.0) {
            selectedTier = CosmeticTier.RARE;
        } else if (rand < 40.0) {
            selectedTier = CosmeticTier.UNCOMMON;
        } else {
            selectedTier = CosmeticTier.COMMON;
        }

        List<RolledCosmetic> candidates = getCosmeticsByTier(selectedTier);
        if (candidates.isEmpty()) {
            CosmeticTier[] tiers = CosmeticTier.values();
            int startIndex = selectedTier.ordinal();
            for (int i = startIndex; i >= 0; i--) {
                candidates = getCosmeticsByTier(tiers[i]);
                if (!candidates.isEmpty()) {
                    break;
                }
            }
            if (candidates.isEmpty()) {
                for (int i = startIndex + 1; i < tiers.length; i++) {
                    candidates = getCosmeticsByTier(tiers[i]);
                    if (!candidates.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            KillMessage defaultMsg = plugin.getCosmeticsManager().getKillMessage("default");
            return new RolledCosmetic(defaultMsg, "default", defaultMsg.getDisplayName(), CosmeticTier.COMMON, "Kill Message");
        }

        return candidates.get((int) (Math.random() * candidates.size()));
    }

    private List<RolledCosmetic> getCosmeticsByTier(CosmeticTier tier) {
        List<RolledCosmetic> list = new ArrayList<>();

        for (KillMessage km : plugin.getCosmeticsManager().getKillMessages().values()) {
            if (km.getId().equalsIgnoreCase("default")) continue;
            if (km.getTier() == tier) {
                list.add(new RolledCosmetic(km, km.getId(), km.getDisplayName(), km.getTier(), "Kill Message"));
            }
        }

        for (KillEffect ke : plugin.getCosmeticsManager().getKillEffects().values()) {
            if (ke.getId().equalsIgnoreCase("none")) continue;
            if (ke.getTier() == tier) {
                list.add(new RolledCosmetic(ke, ke.getId(), ke.getDisplayName(), ke.getTier(), "Kill Effect"));
            }
        }

        for (Trail trail : plugin.getCosmeticsManager().getTrails().values()) {
            if (trail.getId().equalsIgnoreCase("none")) continue;
            if (trail.getTier() == tier) {
                list.add(new RolledCosmetic(trail, trail.getId(), trail.getDisplayName(), trail.getTier(), "Trail"));
            }
        }

        return list;
    }

    private RolledCosmetic getRandomCosmeticFromPool() {
        List<RolledCosmetic> all = new ArrayList<>();
        for (CosmeticTier tier : CosmeticTier.values()) {
            all.addAll(getCosmeticsByTier(tier));
        }
        if (all.isEmpty()) return null;
        return all.get((int) (Math.random() * all.size()));
    }

    /**
     * Checks if the player has PURCHASED this cosmetic (i.e. it exists in their saved set).
     * Does NOT consider free/permission-based ownership — this is intentional for the crate
     * duplicate-detection logic.
     */
    public boolean isPurchased(PlayerStats stats, RolledCosmetic rc) {
        if (rc.getTypeName().equals("Kill Message")) {
            return stats.hasPurchasedKillMessage(rc.getId());
        } else if (rc.getTypeName().equals("Kill Effect")) {
            return stats.hasPurchasedKillEffect(rc.getId());
        } else {
            return stats.hasPurchasedTrail(rc.getId());
        }
    }

    /** @deprecated Use {@link #isPurchased(PlayerStats, RolledCosmetic)} for crate duplicate logic */
    public boolean isOwned(Player player, PlayerStats stats, RolledCosmetic rc) {
        if (rc.getTypeName().equals("Kill Message")) {
            return plugin.getCosmeticsGUI().isKillMessageOwned(player, stats, (KillMessage) rc.getCosmetic());
        } else if (rc.getTypeName().equals("Kill Effect")) {
            return plugin.getCosmeticsGUI().isKillEffectOwned(player, stats, (KillEffect) rc.getCosmetic());
        } else {
            return plugin.getCosmeticsGUI().isTrailOwned(player, stats, (Trail) rc.getCosmetic());
        }
    }

    public void awardCosmetic(PlayerStats stats, RolledCosmetic rc) {
        if (rc.getTypeName().equals("Kill Message")) {
            stats.addPurchasedKillMessage(rc.getId());
        } else if (rc.getTypeName().equals("Kill Effect")) {
            stats.addPurchasedKillEffect(rc.getId());
        } else {
            stats.addPurchasedTrail(rc.getId());
        }
    }

    private void spawnUnlockParticles(Location loc, CosmeticTier tier) {
        if (loc.getWorld() == null) return;
        Location center = loc.clone().add(0.5, 0.8, 0.5);
        org.bukkit.Effect particleEffect;
        int count;

        switch (tier) {
            case MYTHIC:
                particleEffect = org.bukkit.Effect.PORTAL;
                count = 60;
                break;
            case LEGENDARY:
                particleEffect = org.bukkit.Effect.FLAME;
                count = 45;
                break;
            case EPIC:
                particleEffect = org.bukkit.Effect.WITCH_MAGIC;
                count = 40;
                break;
            case RARE:
                particleEffect = org.bukkit.Effect.MAGIC_CRIT;
                count = 30;
                break;
            case UNCOMMON:
                particleEffect = org.bukkit.Effect.HAPPY_VILLAGER;
                count = 25;
                break;
            case COMMON:
            default:
                particleEffect = org.bukkit.Effect.LARGE_SMOKE;
                count = 20;
                break;
        }

        center.getWorld().spigot().playEffect(
                center,
                particleEffect,
                0, 0,
                0.5f, 0.5f, 0.5f,
                0.15f,
                count,
                32
        );
    }

    private ItemStack createCosmeticItemStack(RolledCosmetic rc) {
        Material mat;
        if (rc.getTypeName().equals("Kill Message")) {
            mat = Material.PAPER;
        } else if (rc.getTypeName().equals("Kill Effect")) {
            mat = Material.EYE_OF_ENDER;
        } else {
            mat = Material.NETHER_STAR;
        }

        ItemStack item = new ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(rc.getTier().getColor() + "" + ChatColor.BOLD + ChatColor.translateAlternateColorCodes('&', rc.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + rc.getTypeName());
            lore.add(ChatColor.GRAY + "Rarity: " + rc.getTier().getFormattedName());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, int data, String name) {
        ItemStack item = new ItemStack(mat, 1, (short) data);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat, 1);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void shutdown() {
        for (Hologram holo : holograms.values()) {
            if (holo != null) {
                DHAPI.removeHologram(holo.getName());
            }
        }
        holograms.clear();
        for (Location loc : locations) {
            cleanupExistingArmorStands(loc);
        }
    }

    public static class RolledCosmetic {
        private final Object cosmetic;
        private final String id;
        private final String displayName;
        private final CosmeticTier tier;
        private final String typeName;

        public RolledCosmetic(Object cosmetic, String id, String displayName, CosmeticTier tier, String typeName) {
            this.cosmetic = cosmetic;
            this.id = id;
            this.displayName = displayName;
            this.tier = tier;
            this.typeName = typeName;
        }

        public Object getCosmetic() { return cosmetic; }
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public CosmeticTier getTier() { return tier; }
        public String getTypeName() { return typeName; }
    }
}
