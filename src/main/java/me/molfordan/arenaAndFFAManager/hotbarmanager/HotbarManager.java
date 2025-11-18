package me.molfordan.arenaAndFFAManager.hotbarmanager;

import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotbarManager {

    /**
     * Apply layout using REAL kit items (enchanted)
     * Enforces 1 item per category (except blocks)
     */
    /* OLD
    public static void applyLayout(Player player,
                                   Map<Integer, String> layout,
                                   Map<String, ItemStack> kitItems) {

        // Clear ONLY HOTBAR (0–8)
        for (int i = 0; i <= 8; i++)
            player.getInventory().setItem(i, null);

        if (layout == null || layout.isEmpty())
            return;

        // Track usage per category
        Map<String, Integer> usage = new HashMap<>();

        for (Map.Entry<Integer, String> entry : layout.entrySet()) {

            int slot = entry.getKey();
            String category = entry.getValue();

            if (slot < 0 || slot > 8) continue;
            if (category == null) continue;

            category = category.toLowerCase();

            // Check if item exists in provided kit
            ItemStack item = kitItems.get(category);
            if (item == null) continue;

            // Count usage
            int used = usage.getOrDefault(category, 0);

            // Allow only 1 of each tool
            if (!category.equals("blocks") && used >= 1)
                continue;

            // Blocks can be up to 4 (handled in BuildFFAKit)
            if (category.equals("blocks") && used >= 2)
                continue;

            // Place the item
            player.getInventory().setItem(slot, item.clone());

            // Increase usage count
            usage.put(category, used + 1);
        }
    }

     */
    public static void applyLayout(Player player,
                                   Map<Integer, String> layout,
                                   Map<String, ItemStack> kitItems) {

        if (layout == null || layout.isEmpty()) return;

        // Clear ONLY hotbar slots (0–8)
        for (int i = 0; i <= 8; i++) {
            player.getInventory().setItem(i, null);
        }

        // Track usage per category
        Map<String, Integer> usage = new HashMap<>();

        for (Map.Entry<Integer, String> entry : layout.entrySet()) {

            int slot = entry.getKey();
            if (slot < 0 || slot > 8) continue;

            String category = entry.getValue();
            if (category == null) continue;
            category = category.toLowerCase();

            ItemStack item = kitItems.get(category);
            if (item == null) continue;

            int used = usage.getOrDefault(category, 0);

            // Enforce category limits
            if (!category.equals("blocks")) {
                // All categories except blocks → only 1 allowed
                if (used >= 1) continue;
            } else {
                // Blocks → max 2 stacks
                if (used >= 2) continue;
            }

            // Place the item
            player.getInventory().setItem(slot, item.clone());

            // Count usage
            usage.put(category, used + 1);
        }
    }



    public static String getCategoryFromItem(ItemStack item) {
        if (item == null) return null;

        Material m = item.getType();

        switch (m) {
            case WOOD_SWORD:
            case STONE_SWORD:
            case IRON_SWORD:
            case DIAMOND_SWORD:
                return "melee";

            case WOOD_PICKAXE:
            case STONE_PICKAXE:
            case IRON_PICKAXE:
            case DIAMOND_PICKAXE:
                return "pickaxe";

            case WOOD_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case DIAMOND_AXE:
                return "axe";

            case SHEARS:
                return "shears";

            case STICK:
                return "knockbackstick";

            case ENDER_PEARL:
                return "ender_pearl";

            case SNOW_BALL:
                return "snowball";
            case GOLDEN_APPLE:
                return "golden_apple";

            case WOOL:
            case STAINED_CLAY:
            case HARD_CLAY:
            case SANDSTONE:
                return "blocks";

            default:
                return null;
        }
    }

    public static void resortInventory(Player p, HotbarDataManager dataManager) {

        Map<Integer, String> layout = dataManager.load(p.getUniqueId());
        if (layout == null || layout.isEmpty()) return;

        ItemStack[] contents = p.getInventory().getContents();

        // NEW: Keep non-kit items untouched
        List<ItemStack> nonKitItems = new ArrayList<>();

        // Kit items categorized
        Map<String, List<ItemStack>> categorized = new HashMap<>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;

            String cat = getCategoryFromItem(item);

            if (cat == null) {
                // Non-kit items (like your 64 apples) — keep them separate
                nonKitItems.add(item.clone());
                continue;
            }

            categorized.computeIfAbsent(cat, k -> new ArrayList<>()).add(item.clone());
        }

        // Clear hotbar slots
        for (int i = 0; i <= 8; i++) {
            p.getInventory().setItem(i, null);
        }

        // HOTBAR placement
        for (int slot = 0; slot <= 8; slot++) {
            String expectedCat = layout.get(slot);
            if (expectedCat == null) continue;

            List<ItemStack> list = categorized.get(expectedCat);
            if (list == null || list.isEmpty()) continue;

            ItemStack next = list.remove(0);
            p.getInventory().setItem(slot, next);
        }

        // Clear inventory (9..35)
        for (int i = 9; i < 36; i++) {
            p.getInventory().setItem(i, null);
        }

        // Fill leftover kit items
        int index = 9;
        for (List<ItemStack> list : categorized.values()) {
            for (ItemStack item : list) {
                if (index >= 36) break;
                p.getInventory().setItem(index++, item);
            }
        }

        // Finally fill non-kit items (your 64 apples etc)
        for (ItemStack item : nonKitItems) {
            if (index >= 36) break;
            p.getInventory().setItem(index++, item);
        }
    }





}
