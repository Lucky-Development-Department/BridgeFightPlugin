package me.molfordan.bridgefightplugin.hotbarmanager;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.kits.KitManager;
import me.molfordan.bridgefightplugin.manager.BedFightHotbarDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BedFightHotbarSession {

    private static final String TITLE = "§bBedFight Kit Editor";

    private static final Set<String> UNIQUE_CATEGORIES = new HashSet<>(
            Arrays.asList("melee", "pickaxe", "axe", "shears", "blocks")
    );

    private final BridgeFightPlugin plugin;
    private final Player player;
    private final BedFightHotbarDataManager dataManager;
    private final Inventory inventory;
    private final KitManager kitManager;

    private final Map<Integer, String> categories = new LinkedHashMap<>();
    private final Map<Integer, String> hotbarLayout = new HashMap<>();

    private int selectedSlot = 0;

    public BedFightHotbarSession(BridgeFightPlugin plugin, Player player, BedFightHotbarDataManager dataManager, KitManager kitManager) {
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
        putCategory(slot++, "melee");
        putCategory(slot++, "pickaxe");
        putCategory(slot++, "axe");
        putCategory(slot++, "shears");
        putCategory(slot++, "blocks");
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

    public void handleClick(int rawSlot, ItemStack current, ItemStack cursor, boolean shiftClick) {
        if (shiftClick) return;

        if (rawSlot == 3) {
            if (!isHotbarValid()) {
                player.sendMessage(ChatColor.RED + "Your hotbar is invalid!"
                        + ChatColor.GRAY + " (Duplicate unique items or exceeded slot limits)");
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
                return;
            }
            player.closeInventory();
            return;
        }

        if (rawSlot == 5) {
            applyDefaultLayout();
            saveHotbarLayoutToDB();
            drawHotbarLayout();
            return;
        }

        if (categories.containsKey(rawSlot)) {
            String cat = categories.get(rawSlot);
            hotbarLayout.put(selectedSlot, cat);
            saveHotbarLayoutToDB();
            drawHotbarLayout();
            return;
        }

        if (rawSlot >= 36 && rawSlot < 45) {
            selectedSlot = rawSlot - 36;
            drawDividers();
            drawCategories();
            return;
        }

        if (rawSlot >= 45 && rawSlot < 54) {
            int hotbarSlot = rawSlot - 45;
            if (cursor != null && cursor.getType() != Material.AIR) {
                String inferred = normalizeIdentifierToCategory(cursor.getType().name());
                if (UNIQUE_CATEGORIES.contains(inferred) && hotbarLayout.containsValue(inferred)) {
                    if (!inferred.equals(hotbarLayout.get(hotbarSlot))) {
                        player.sendMessage(ChatColor.RED + "That category is already used in another slot.");
                        return;
                    }
                }
                hotbarLayout.put(hotbarSlot, inferred);
                player.setItemOnCursor(null);
                saveHotbarLayoutToDB();
                drawHotbarLayout();
            } else {
                if (hotbarLayout.containsKey(hotbarSlot)) {
                    hotbarLayout.remove(hotbarSlot);
                    saveHotbarLayoutToDB();
                } else {
                    selectedSlot = hotbarSlot;
                    drawDividers();
                    drawCategories();
                }
                drawHotbarLayout();
            }
        }
    }

    public boolean isHotbarValid() {
        Set<String> seen = new HashSet<>();
        int blocksCount = 0;

        for (String id : hotbarLayout.values()) {
            if (id == null) continue;
            id = id.toLowerCase();
            if (UNIQUE_CATEGORIES.contains(id)) {
                if (seen.contains(id)) return false;
                seen.add(id);
            }
            if (id.equals("blocks")) {
                blocksCount++;
                if (blocksCount > 2) return false;
            }
        }
        return true;
    }

    private void loadHotbarLayoutFromDB() {
        hotbarLayout.clear();
        Map<Integer, String> loaded = dataManager.load(player.getUniqueId());
        if (loaded == null || loaded.isEmpty()) {
            applyDefaultLayout();
            return;
        }

        boolean hasValid = false;
        for (Map.Entry<Integer, String> e : loaded.entrySet()) {
            int slot = e.getKey();
            String val = e.getValue();
            if (slot < 0 || slot > 8 || val == null || val.trim().isEmpty()) continue;

            String normalized = normalizeIdentifierToCategory(val);
            if (normalized == null) continue;
            hotbarLayout.put(slot, normalized);
            hasValid = true;
        }
        if (!hasValid) applyDefaultLayout();
    }

    public void applyDefaultLayout() {
        hotbarLayout.clear();
        hotbarLayout.put(0, "melee");
        hotbarLayout.put(1, "blocks");
        hotbarLayout.put(2, "pickaxe");
        hotbarLayout.put(3, "axe");
        hotbarLayout.put(4, "shears");
        saveHotbarLayoutToDB(); // Ensure default is saved
    }

    public void saveHotbarLayoutToDB() {
        Map<Integer, String> toSave = new HashMap<>();
        for (Map.Entry<Integer, String> e : hotbarLayout.entrySet()) {
            Integer slot = e.getKey();
            String cat = e.getValue();
            if (slot == null || slot < 0 || slot > 8 || cat == null || cat.trim().isEmpty()) continue;
            toSave.put(slot, cat);
        }
        dataManager.save(player.getUniqueId(), toSave);
    }

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
            default: return buildItem(Material.STONE, ChatColor.WHITE + id);
        }
    }

    private String normalizeIdentifierToCategory(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return "blocks";
        String normalized = identifier.toLowerCase().trim();
        List<String> direct = Arrays.asList("blocks","melee","pickaxe","axe","shears");
        if (direct.contains(normalized)) return normalized;
        if (normalized.contains("pickaxe")) return "pickaxe";
        if (normalized.contains("axe")) return "axe";
        if (normalized.contains("shears")) return "shears";
        if (normalized.contains("sword")) return "melee";
        return "blocks";
    }

    private String categoryDisplayName(String identifier) {
        if (identifier == null) return "Unknown";
        String clean = identifier.trim().toLowerCase();
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    public void onClose() {}
}
