package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkit.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class CustomKitBaseGUI {

    private final ArenaAndFFAManager plugin;

    public CustomKitBaseGUI(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public static final String TITLE = ChatColor.DARK_GRAY + "Custom Kit Editor";

    private static ItemStack item(Material mat, int dataValue, String name, String... lore) {
        ItemStack it = new ItemStack(mat, 1, (short) dataValue); // <- DATA VALUE APPLIED
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null && lore.length > 0)
            meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null && lore.length > 0)
            meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack glowingItem(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null && lore.length > 0)
            meta.setLore(Arrays.asList(lore));
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(meta);
        return it;
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // reusable items
        ItemStack black = item(Material.STAINED_GLASS_PANE, 15, " ");
        ItemStack gray = item(Material.STAINED_GLASS_PANE, 7, ChatColor.GRAY + "Empty Armor Slot");
        ItemStack white = item(Material.STAINED_GLASS_PANE, 0, ChatColor.WHITE + "Empty Weapon Slot");

        // place border black panes
        int[] blackSlots = {
                0,1,2,3,5,7,8,
                9,11,
                18,20,
                27,29,
                36,38,
                44,45,47,48,50,52
        };
        for (int s : blackSlots) inv.setItem(s, black);

        // gray armor placeholder slots
        inv.setItem(10, gray);
        inv.setItem(19, gray);
        inv.setItem(28, gray);
        inv.setItem(37, gray);

        // weapon placeholder slot
        inv.setItem(46, white);

        // weapon selector
        inv.setItem(4, item(Material.STONE_SWORD,
                ChatColor.YELLOW + "Weapon Selection",
                ChatColor.GRAY + "Click to choose your sword"));

        // armor selector
        inv.setItem(6, item(Material.LEATHER_CHESTPLATE, 14,
                ChatColor.YELLOW + "Armor Selection",
                ChatColor.GRAY + "Click to choose your armor set"));

        // enchants
        inv.setItem(26, item(Material.ENCHANTED_BOOK,
                ChatColor.AQUA + "Sharpness",
                ChatColor.GRAY + "Toggle sharpness for your weapon"));

        inv.setItem(35, item(Material.ENCHANTED_BOOK,
                ChatColor.AQUA + "Protection",
                ChatColor.GRAY + "Toggle protection for your armor"));

        // anvil (kit name)
        inv.setItem(17, item(Material.ANVIL,
                ChatColor.GREEN + "Set Kit Name",
                ChatColor.GRAY + "Click to rename your kit"));

        // cancel
        inv.setItem(49, item(Material.STAINED_GLASS_PANE, 14,
                ChatColor.RED + "Cancel",
                ChatColor.GRAY + "Discard changes"));

        // confirm
        inv.setItem(51, item(Material.STAINED_GLASS_PANE, 5,
                ChatColor.GREEN + "Save Kit",
                ChatColor.GRAY + "Confirm and save"));

        // previous page (slot 8)
        inv.setItem(8, item(Material.ARROW,
                ChatColor.YELLOW + "Previous Page"));

        // next page (slot 53)
        inv.setItem(53, item(Material.ARROW,
                ChatColor.YELLOW + "Next Page"));

        // empty custom item slots → leave them empty

        player.openInventory(inv);
    }
}
