package me.molfordan.arenaAndFFAManager.hotbarmanager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * HotbarSession: Kinawn-style 54-slot GUI in a session wrapper.
 * - Categories start at slot 9 (row 2)
 * - Reset (slot 5) / Close (slot 3)
 * - Divider row 36-44 selects hotbar slot 0..8
 * - Hotbar preview 45-53
 * - Uses HotbarDataManager to load/save YAML (0..8 -> category)
 */
public class HotbarSession {

    private static final String TITLE = "§eHotbar Manager";

    private static final Set<String> UNIQUE_CATEGORIES = new HashSet<>(
            Arrays.asList("melee", "pickaxe", "axe", "shears", "compass")
    );

    private final ArenaAndFFAManager plugin;
    private final Player player;
    private final HotbarDataManager dataManager;
    private final Inventory inventory;
    private final KitManager kitManager;

    // categories GUI slot -> identifier
    private final Map<Integer, String> categories = new LinkedHashMap<>();

    // hotbar layout: 0..8 -> identifier
    private final Map<Integer, String> hotbarLayout = new HashMap<>();

    private int selectedSlot = 0;

    public HotbarSession(ArenaAndFFAManager plugin, Player player, HotbarDataManager dataManager, KitManager kitManager) {
        this.plugin = plugin;
        this.player = player;
        this.dataManager = dataManager;
        this.inventory = Bukkit.createInventory(null, 54, TITLE);
        this.kitManager = kitManager;

        initCategoryPositions();
        loadHotbarLayoutFromDB();
        drawAll();
    }

    private void initCategoryPositions() {
        int slot = 9;
        putCategory(slot++, "blocks");
        putCategory(slot++, "melee");
        putCategory(slot++, "pickaxe");
        putCategory(slot++, "axe");
        putCategory(slot++, "shears");
       // putCategory(slot++, "ranged");
        putCategory(slot++, "speed");
        putCategory(slot++, "invisibility");
        putCategory(slot++, "jump");

        slot = 18;
        //putCategory(slot++, "tnt");
        //putCategory(slot++, "ladder");
        putCategory(slot++, "golden_apple");
       // putCategory(slot++, "fireball");
        putCategory(slot++, "knockbackstick");
        //putCategory(slot++, "respawn_item");
        putCategory(slot++, "ender_pearl");
        //putCategory(slot++, "water_bucket");

        // optional extras
        putCategory(27, "snowball");
        //putCategory(28, "compass");
    }

    private void putCategory(int guiSlot, String id) {
        categories.put(guiSlot, id);
    }

    public Inventory getInventory() {
        return inventory;
    }

    private void drawAll() {
        inventory.clear();
        drawTopButtons();
        drawCategories();
        drawDividers();
        drawHotbarLayout();
        // open inventory if not already open (session manager handles showing)
    }

    private void drawTopButtons() {
        inventory.setItem(3, buildItem(Material.ARROW, ChatColor.YELLOW + "Close"));
        inventory.setItem(5, buildItem(Material.BARRIER, ChatColor.RED + "Reset to defaults"));
    }

    private void drawCategories() {
        for (Map.Entry<Integer, String> e : categories.entrySet()) {
            ItemStack icon = getCategoryIcon(e.getValue());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(categoryDisplayName(e.getValue()));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Click to place into selected slot: " + ChatColor.AQUA + (selectedSlot + 1));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(e.getKey(), icon);
        }
    }

    private void drawDividers() {
        for (int i = 0; i < 9; i++) {
            int index = 36 + i;
            boolean sel = (i == selectedSlot);
            inventory.setItem(index, buildItem(Material.STAINED_GLASS_PANE, sel ? (short)5 : (short)7, sel ? ChatColor.GREEN + "Selected" : ChatColor.GRAY + "Slot " + (i + 1)));
        }
    }

    private void drawHotbarLayout() {
        for (int i = 0; i < 9; i++) {
            int display = 45 + i;
            String id = hotbarLayout.get(i);
            if (id != null) {
                ItemStack icon = getCategoryIcon(id);
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(categoryDisplayName(id));
                    meta.setLore(Collections.singletonList(ChatColor.GRAY + "Hotbar slot " + (i + 1)));
                    icon.setItemMeta(meta);
                }
                inventory.setItem(display, icon);
            } else {
                inventory.setItem(display, buildItem(Material.STAINED_GLASS_PANE, (short)7, ChatColor.DARK_GRAY + "Empty"));
            }
        }
    }

    /**
     * Called from listener with rawSlot and click context.
     * We deliberately keep signature small: player is known.
     */
    public void handleClick(int rawSlot, ItemStack current, ItemStack cursor, boolean shiftClick) {
        if (shiftClick) return; // deny shift-click interactions

        // Close

        // Validate hotbar
        // Close button
        if (rawSlot == 3) {

            // Validate full layout (unique, block/max pearl rule)
            if (!isHotbarValid()) {
                player.sendMessage(ChatColor.RED + "Your hotbar is invalid!"
                        + ChatColor.GRAY + " (Duplicate unique items or exceeded slot limits)");

                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
                return;
            }

            player.closeInventory();

            String lobbyWorldName = plugin.getConfigManager().getLobbyWorldName();
            String bridgeFightWorldName = plugin.getConfigManager().getBridgeFightWorldName();

            // Apply kit AFTER GUI closes (your giveKillRewards will handle ensureItem)
            if (player.getWorld().getName().equals(lobbyWorldName)) return;
            if (player.getWorld().getName().equals(bridgeFightWorldName)) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                HotbarManager.resortInventory(player, plugin.getHotbarDataManager());
            });
            return;
        }

        // Reset
        if (rawSlot == 5) {
            applyDefaultLayout();
            saveHotbarLayoutToDB();
            drawHotbarLayout();
            return;
        }

        // If clicked a category icon (placed starting at slot 9)
        if (categories.containsKey(rawSlot)) {
            String cat = categories.get(rawSlot);
            hotbarLayout.put(selectedSlot, cat);
            saveHotbarLayoutToDB();
            drawHotbarLayout();
            return;
        }



        // Divider row: select hotbar slot
        if (rawSlot >= 36 && rawSlot < 45) {
            selectedSlot = rawSlot - 36;
            drawDividers();
            drawCategories();
            return;
        }

        // Hotbar preview row
        if (rawSlot >= 45 && rawSlot < 54) {
            int hotbarSlot = rawSlot - 45;

            // If item on cursor: infer category and place
            if (cursor != null && cursor.getType() != Material.AIR) {
                String inferred = normalizeIdentifierToCategory(cursor.getType().name());

                if (UNIQUE_CATEGORIES.contains(inferred) && hotbarLayout.containsValue(inferred)) {
                    if (!inferred.equals(hotbarLayout.get(hotbarSlot))) {
                        player.sendMessage(ChatColor.RED + "That category is already used in another slot.");
                        return;
                    }
                }

                hotbarLayout.put(hotbarSlot, inferred);
                // consume the icon (we don't remove player's real items, this is purely GUI icon)
                player.setItemOnCursor(null);
                saveHotbarLayoutToDB();
                drawHotbarLayout();
                return;
            } else {
                // no cursor -> toggle clear or select
                if (hotbarLayout.containsKey(hotbarSlot)) {
                    hotbarLayout.remove(hotbarSlot);
                    saveHotbarLayoutToDB();
                } else {
                    selectedSlot = hotbarSlot;
                    drawDividers();
                    drawCategories();
                }
                drawHotbarLayout();
                return;
            }
        }
    }

    // ----- Persistence via HotbarDataManager -----
    private void loadHotbarLayoutFromDB() {
        hotbarLayout.clear();
        if (dataManager == null) {
            applyDefaultLayout();
            return;
        }
        Map<Integer, String> loaded = dataManager.load(player.getUniqueId());
        if (loaded == null || loaded.isEmpty()) {
            applyDefaultLayout();
            return;
        }

        boolean hasValid = false;
        for (Map.Entry<Integer, String> e : loaded.entrySet()) {
            int key = e.getKey();
            String val = e.getValue();
            if (val == null || val.trim().isEmpty()) continue;
            int slot;
            if (key >= 36 && key <= 44) slot = key - 36;
            else if (key >= 0 && key <= 8) slot = key;
            else continue;

            String normalized = normalizeIdentifierToCategory(val);
            if (normalized == null) continue;
            hotbarLayout.put(slot, normalized);
            hasValid = true;
        }
        if (!hasValid) applyDefaultLayout();
    }

    private void applyDefaultLayout() {
        hotbarLayout.clear();
        hotbarLayout.put(0, "melee");
        hotbarLayout.put(1, "pickaxe");
        hotbarLayout.put(2, "axe");
        hotbarLayout.put(3, "shears");
        hotbarLayout.put(4, "knockbackstick");
        hotbarLayout.put(5, "ender_pearl");
        hotbarLayout.put(6, "blocks");
        hotbarLayout.put(7, "blocks");
        hotbarLayout.put(8, "snowball");
    }

    public boolean isHotbarValid() {
        Set<String> seen = new HashSet<>();
        int blocksCount = 0;
        int pearlCount = 0;

        for (String id : hotbarLayout.values()) {
            if (id == null) continue;
            id = id.toLowerCase();

            // Unique categories
            if (UNIQUE_CATEGORIES.contains(id)) {
                if (seen.contains(id)) {
                    return false;
                }
                seen.add(id);
            }

            // blocks max 2
            if (id.equals("blocks")) {
                blocksCount++;
                if (blocksCount > 2) return false;
            }

            // ender_pearl max 2
            if (id.equals("ender_pearl")) {
                pearlCount++;
                if (pearlCount > 2) return false;
            }
        }
        return true;
    }

    private void saveHotbarLayoutToDB() {
        if (dataManager == null) return;
        Map<Integer, String> toSave = new HashMap<>();
        for (Map.Entry<Integer, String> e : hotbarLayout.entrySet()) {
            Integer slot = e.getKey();
            String cat = e.getValue();
            if (slot == null || slot < 0 || slot > 8) continue;
            if (cat == null || cat.trim().isEmpty()) continue;
            toSave.put(slot, cat);
        }
        dataManager.save(player.getUniqueId(), toSave);
    }

    // ----- Utilities: item builders, icons, mapping -----
    private ItemStack buildItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack buildItem(Material mat, short data, String name) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack getCategoryIcon(String id) {
        if (id == null) return buildItem(Material.STONE, ChatColor.WHITE + "Unknown");
        String k = id.toLowerCase(Locale.ROOT);
        switch (k) {
            case "blocks": return buildItem(Material.WOOL, (short)0, ChatColor.WHITE + "Blocks");
            case "melee": return buildItem(Material.IRON_SWORD, ChatColor.WHITE + "Melee");
            case "pickaxe": return buildItem(Material.IRON_PICKAXE, ChatColor.WHITE + "Pickaxe");
            case "axe": return buildItem(Material.IRON_AXE, ChatColor.WHITE + "Axe");
            case "shears": return buildItem(Material.SHEARS, ChatColor.WHITE + "Shears");
            case "ranged": return buildItem(Material.BOW, ChatColor.WHITE + "Ranged");
            case "speed": return createPotion("Speed II", PotionEffectType.SPEED, 45, 1);
            case "invisibility": return createPotion("Invisibility", PotionEffectType.INVISIBILITY, 30, 0);
            case "jump": return createPotion("Jump Boost", PotionEffectType.JUMP, 45, 4);
            case "golden_apple": return buildItem(Material.GOLDEN_APPLE, ChatColor.WHITE + "Golden Apple");
            case "fireball": return buildItem(Material.FIREBALL, ChatColor.WHITE + "Fireball");
            case "tnt": return buildItem(Material.TNT, ChatColor.WHITE + "TNT");
            case "ladder": return buildItem(Material.LADDER, ChatColor.WHITE + "Ladder");
            case "respawn_item": return buildItem(Material.TRIPWIRE_HOOK, ChatColor.WHITE + "Respawn Item");
            case "ender_pearl": return buildItem(Material.ENDER_PEARL, ChatColor.WHITE + "Ender Pearl");
            case "water_bucket": return buildItem(Material.WATER_BUCKET, ChatColor.WHITE + "Water Bucket");
            case "knockbackstick": {
                ItemStack stick = buildItem(Material.STICK, ChatColor.WHITE + "KB Stick");
                ItemMeta sm = stick.getItemMeta();
                if (sm != null) {
                    sm.addEnchant(Enchantment.KNOCKBACK, 1, true);
                    stick.setItemMeta(sm);
                }
                return stick;
            }
            case "compass": return buildItem(Material.COMPASS, ChatColor.WHITE + "Compass");
            case "snowball": return buildItem(Material.SNOW_BALL, ChatColor.WHITE + "Snowballs");
            default: return buildItem(Material.STONE, ChatColor.WHITE + id);
        }
    }

    private ItemStack createPotion(String name, PotionEffectType effectType, int seconds, int amp) {
        ItemStack potion = new ItemStack(Material.POTION, 1);
        PotionMeta pm = (PotionMeta) potion.getItemMeta();
        if (pm != null) {
            pm.setDisplayName(ChatColor.WHITE + name);
            pm.addCustomEffect(new PotionEffect(effectType, seconds * 20, amp), true);
            pm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);
            potion.setItemMeta(pm);
        }
        // legacy data for 1.8 to show correct color
        if (effectType.equals(PotionEffectType.SPEED)) potion.setDurability((short)8194);
        else if (effectType.equals(PotionEffectType.INVISIBILITY)) potion.setDurability((short)8206);
        else if (effectType.equals(PotionEffectType.JUMP)) potion.setDurability((short)8203);
        return potion;
    }

    private String normalizeIdentifierToCategory(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return "blocks";
        String normalized = identifier.toLowerCase().trim();
        // direct list
        List<String> direct = Arrays.asList("blocks","melee","armor","tools","ranged","potions","speed","invisibility","jump",
                "tnt","fireball","golden_apple","knockbackstick","respawn_item","ender_pearl","water_bucket","compass",
                "pickaxe","axe","shears","ladder","snowball");
        if (direct.contains(normalized)) return normalized;
        // heuristics
        if (normalized.contains("pickaxe")) return "pickaxe";
        if (normalized.contains("axe")) return "axe";
        if (normalized.contains("shears")) return "shears";
        if (normalized.contains("speed")) return "speed";
        if (normalized.contains("invisibil") || normalized.contains("invisibility")) return "invisibility";
        if (normalized.contains("jump")) return "jump";
        if (normalized.contains("sword") || normalized.contains("stick")) return "melee";
        if (normalized.contains("bow") || normalized.contains("arrow") || normalized.contains("trident")) return "ranged";
        if (normalized.contains("potion") || normalized.contains("brewing") || normalized.contains("pearl")) return "potions";
        if (normalized.contains("tnt")) return "tnt";
        if (normalized.contains("fireball")) return "fireball";
        if (normalized.contains("golden_apple")) return "golden_apple";
        if (normalized.contains("tripwire_hook")) return "respawn_item";
        if (normalized.contains("ender_pearl")) return "ender_pearl";
        if (normalized.contains("water_bucket")) return "water_bucket";
        return "blocks";
    }

    private String categoryDisplayName(String identifier) {
        if (identifier == null) return "Unknown";
        if ("speed".equalsIgnoreCase(identifier)) return "Speed II Potion (45s)";
        if ("jump".equalsIgnoreCase(identifier)) return "Jump Boost V (45s)";
        if ("invisibility".equalsIgnoreCase(identifier)) return "Invisibility (30s)";
        if ("tnt".equalsIgnoreCase(identifier)) return "TNT";
        if ("knockbackstick".equalsIgnoreCase(identifier)) return "Stick (KnockBack I)";
        if ("respawn_item".equalsIgnoreCase(identifier)) return "Respawn Item";
        if ("ender_pearl".equalsIgnoreCase(identifier)) return "Ender Pearl";
        if ("water_bucket".equalsIgnoreCase(identifier)) return "Water Bucket";
        String clean = identifier.trim().toLowerCase();
        if (clean.contains("_")) {
            String[] parts = clean.split("_");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
            return sb.toString().trim();
        }
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    public void onClose() {
        // nothing extra — data already saved on save button actions
    }
}
