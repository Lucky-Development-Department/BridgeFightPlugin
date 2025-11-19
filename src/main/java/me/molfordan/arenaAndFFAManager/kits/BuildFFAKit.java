package me.molfordan.arenaAndFFAManager.kits;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BuildFFAKit {

    private final Random random = new Random();
    private static final int MAX_WOOL_STACKS = 2;

    // ======================================================
    // STORED KIT ITEMS (used by giveKit() & getKitItems())
    // ======================================================
    private ItemStack sword;
    private ItemStack pickaxe;
    private ItemStack axe;
    private ItemStack shears;
    private ItemStack kbStick;
    private ItemStack pearl;
    private ItemStack woolStack;

    public void giveKit(Player player, Map<Integer, String> hotbarLayout) {

        // full clear
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // random team color
        boolean isRed = random.nextBoolean();
        Color teamColor = isRed ? Color.RED : Color.LIME;
        short woolColor = (short) (isRed ? 14 : 5);

        // ======================================================
        // CREATE ALL ITEMS AND STORE THEM IN CLASS FIELDS
        // ======================================================
        woolStack = new ItemStack(Material.WOOL, 64, woolColor);
        sword = createItem(Material.STONE_SWORD);
        pickaxe = createItem(Material.WOOD_PICKAXE, Enchantment.DIG_SPEED, 1);
        axe = createItem(Material.WOOD_AXE, Enchantment.DIG_SPEED, 1);
        shears = createItem(Material.SHEARS);
        kbStick = createItem(Material.STICK, Enchantment.KNOCKBACK, 1);
        pearl = new ItemStack(Material.ENDER_PEARL, 1);

        // Armor
        player.getInventory().setHelmet(createColoredArmor(Material.LEATHER_HELMET, teamColor));
        player.getInventory().setChestplate(createColoredArmor(Material.LEATHER_CHESTPLATE, teamColor));
        player.getInventory().setLeggings(createItem(Material.IRON_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 3));
        player.getInventory().setBoots(createItem(Material.IRON_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 3));

        // ======================================================
        // KIT ITEMS MAP FOR HOTBAR MANAGER
        // ======================================================
        Map<String, ItemStack> kitItems = new HashMap<>();
        kitItems.put("melee", sword);
        kitItems.put("pickaxe", pickaxe);
        kitItems.put("axe", axe);
        kitItems.put("shears", shears);
        kitItems.put("knockbackstick", kbStick);
        kitItems.put("blocks", woolStack);
        kitItems.put("pearl", pearl);
        kitItems.put("ender_pearl", pearl);

        // ======================================================
        // APPLY HOTBAR LAYOUT
        // ======================================================
        HotbarManager.applyLayout(player, hotbarLayout, kitItems);

        // Fallback for unplaced items
        if (!containsValue(hotbarLayout, "melee")) player.getInventory().addItem(sword);
        if (!containsValue(hotbarLayout, "pickaxe")) player.getInventory().addItem(pickaxe);
        if (!containsValue(hotbarLayout, "axe")) player.getInventory().addItem(axe);
        if (!containsValue(hotbarLayout, "shears")) player.getInventory().addItem(shears);
        if (!containsValue(hotbarLayout, "knockbackstick")) player.getInventory().addItem(kbStick);

        // ======================================================
        // GIVE PEARL (if player placed pearl category)
        // ======================================================
        boolean wantsPearl = containsValue(hotbarLayout, "pearl") || containsValue(hotbarLayout, "ender_pearl");
        if (!player.getInventory().contains(Material.ENDER_PEARL)) {
            // If player has pearl in layout, it will be handled by HotbarManager
            // If not, add it to their inventory
            if (!wantsPearl) {
                player.getInventory().addItem(pearl.clone());
            }
        }

        // ======================================================
        // GIVE BLOCKS
        // ======================================================
        int blocksInLayout = countBlocksInLayout(hotbarLayout);

// total stacks we want:
        int totalStacks = MAX_WOOL_STACKS;

// give stacks to layout-handled slots first
        for (int i = 0; i < blocksInLayout && i < totalStacks; i++) {
            // HotbarManager already placed them
            totalStacks--;
        }

// give the remaining stacks to inventory
        for (int i = 0; i < totalStacks; i++) {
            player.getInventory().addItem(woolStack.clone());
        }

        Bukkit.getScheduler().runTask(
                ArenaAndFFAManager.getPlugin(),
                () -> HotbarManager.resortInventory(player, ArenaAndFFAManager.getPlugin().getHotbarDataManager())
        );
    }

    // ======================================================
    // RETURN ALL KIT ITEMS FOR CATEGORY DETECTION
    // ======================================================
    public Map<String, ItemStack> getKitItems() {
        Map<String, ItemStack> map = new HashMap<>();

        map.put("melee", sword.clone());
        map.put("pickaxe", pickaxe.clone());
        map.put("axe", axe.clone());
        map.put("shears", shears.clone());
        map.put("knockbackstick", kbStick.clone());
        map.put("blocks", woolStack.clone());
        map.put("pearl", pearl.clone());
        map.put("ender_pearl", pearl.clone());

        return map;
    }

    // ======================================================
    // UTILITIES
    // ======================================================

    private int countBlocksInLayout(Map<Integer, String> layout) {
        int c = 0;
        for (String v : layout.values()) {
            if ("blocks".equalsIgnoreCase(v)) c++;
        }
        return c;
    }

    private boolean containsValue(Map<Integer, String> map, String val) {
        if (map == null) return false;
        for (String v : map.values()) {
            if (v != null && v.equalsIgnoreCase(val)) return true;
        }
        return false;
    }

    private ItemStack createColoredArmor(Material type, Color color) {
        ItemStack item = new ItemStack(type);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material type) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material type, Enchantment ench, int level) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench, level, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
