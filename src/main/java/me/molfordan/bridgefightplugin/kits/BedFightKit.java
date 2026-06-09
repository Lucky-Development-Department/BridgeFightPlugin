package me.molfordan.bridgefightplugin.kits;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.hotbarmanager.HotbarManager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.HashMap;
import java.util.Map;

public class BedFightKit {

    public void apply(Player player, String team) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Armor
        Color color = team.equalsIgnoreCase("RED") ? Color.RED : Color.BLUE;
        player.getInventory().setHelmet(createLeatherArmor(Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(createLeatherArmor(Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(createLeatherArmor(Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(createLeatherArmor(Material.LEATHER_BOOTS, color));

        // Create Items
        ItemStack sword = createTool(Material.WOOD_SWORD);
        ItemStack pickaxe = createEnchantedTool(Material.WOOD_PICKAXE, Enchantment.DIG_SPEED, 1);
        ItemStack axe = createEnchantedTool(Material.WOOD_AXE, Enchantment.DIG_SPEED, 1);
        ItemStack shears = createUnbreakableItem(Material.SHEARS);
        ItemStack wool = new ItemStack(Material.WOOL, 64, team.equalsIgnoreCase("RED") ? (short) 14 : (short) 11);

        // Map categories
        Map<String, ItemStack> kitItems = new HashMap<>();
        kitItems.put("melee", sword);
        kitItems.put("pickaxe", pickaxe);
        kitItems.put("axe", axe);
        kitItems.put("shears", shears);
        kitItems.put("blocks", wool);

        // Load and Apply Layout
        Map<Integer, String> layout = BridgeFightPlugin.getPlugin().getBedFightHotbarDataManager().load(player.getUniqueId());
        if (layout == null || layout.isEmpty()) {
            // Apply defaults if no layout saved
            player.getInventory().setItem(0, sword);
            player.getInventory().setItem(1, wool);
            player.getInventory().setItem(2, pickaxe);
            player.getInventory().setItem(3, axe);
            player.getInventory().setItem(4, shears);
        } else {
            HotbarManager.applyLayout(player, layout, kitItems);

            // Fallback for missing essential tools in layout
            if (!containsValue(layout, "melee")) player.getInventory().addItem(sword);
            if (!containsValue(layout, "pickaxe")) player.getInventory().addItem(pickaxe);
            if (!containsValue(layout, "axe")) player.getInventory().addItem(axe);
            if (!containsValue(layout, "shears")) player.getInventory().addItem(shears);
            if (!containsValue(layout, "blocks")) player.getInventory().addItem(wool);
        }
    }

    private boolean containsValue(Map<Integer, String> map, String val) {
        for (String v : map.values()) {
            if (v != null && v.equalsIgnoreCase(val)) return true;
        }
        return false;
    }

    private ItemStack createLeatherArmor(Material mat, Color color) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTool(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnchantedTool(Material mat, Enchantment ench, int level) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench, level, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUnbreakableItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
