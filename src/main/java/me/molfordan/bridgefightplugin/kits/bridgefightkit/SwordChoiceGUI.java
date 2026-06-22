package me.molfordan.bridgefightplugin.kits.bridgefightkit;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SwordChoiceGUI {

    private final BridgeFightPlugin plugin;
    private final SwordChoiceManager swordChoiceManager;

    public static final String TITLE = ChatColor.DARK_GRAY + "Sword & Armor Selection";

    public SwordChoiceGUI(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.swordChoiceManager = plugin.getSwordChoiceManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, 15, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        Material currentSword = swordChoiceManager.getSelectedSword(player.getUniqueId());
        boolean hasSharp = swordChoiceManager.hasSharpness(player.getUniqueId());
        String currentArmorKit = swordChoiceManager.getSelectedArmorKit(player.getUniqueId());

        boolean hasVip = player.hasPermission("group.vip") || player.isOp();

        // 1. Swords
        inv.setItem(10, createSwordItem(Material.WOOD_SWORD, "Wood Sword", currentSword == Material.WOOD_SWORD, true));
        inv.setItem(11, createSwordItem(Material.STONE_SWORD, "Stone Sword", currentSword == Material.STONE_SWORD, hasVip));
        inv.setItem(12, createSwordItem(Material.GOLD_SWORD, "Gold Sword", currentSword == Material.GOLD_SWORD, hasVip));
        inv.setItem(13, createSwordItem(Material.IRON_SWORD, "Iron Sword", currentSword == Material.IRON_SWORD, hasVip));
        inv.setItem(14, createSwordItem(Material.DIAMOND_SWORD, "Diamond Sword", currentSword == Material.DIAMOND_SWORD, hasVip));

        // 2. Sharpness Toggle
        List<String> sharpLore = new ArrayList<>();
        sharpLore.add(ChatColor.GRAY + "Sharpness I Enchantment");
        sharpLore.add("");
        sharpLore.add(ChatColor.GRAY + "Status: " + (hasSharp ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        sharpLore.add("");
        sharpLore.add(ChatColor.YELLOW + "Click to toggle");
        inv.setItem(16, createItem(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Sharpness Option", sharpLore));

        // 3. Armor Option Title / Divider
        inv.setItem(18, createItem(Material.IRON_CHESTPLATE, ChatColor.YELLOW + "Armor Kits:"));

        // No Armor Choice
        List<String> noArmorLore = new ArrayList<>();
        noArmorLore.add(ChatColor.GRAY + "Wear no armor during battles");
        noArmorLore.add("");
        if (currentArmorKit.equalsIgnoreCase("None")) {
            noArmorLore.add(ChatColor.GREEN + "SELECTED");
        } else {
            noArmorLore.add(ChatColor.YELLOW + "Click to select");
        }
        inv.setItem(20, createItem(Material.BARRIER, ChatColor.RED + "No Armor", noArmorLore));

        // Available Armor Kits
        int slot = 21;
        for (Kit2 kit : plugin.getBridgeFightKitManager().getAllKits()) {
            if (slot > 26) break; // Limit to slots 21-26
            boolean hasKitAccess = kit.hasPermission(player);
            List<String> kitLore = new ArrayList<>();
            kitLore.add(ChatColor.GRAY + "Armor: " + ChatColor.WHITE + kit.getDisplayName());
            kitLore.add("");
            if (!hasKitAccess) {
                kitLore.add(ChatColor.RED + "LOCKED - Requires VIP");
            } else if (currentArmorKit.equalsIgnoreCase(kit.getName())) {
                kitLore.add(ChatColor.GREEN + "SELECTED");
            } else {
                kitLore.add(ChatColor.YELLOW + "Click to select");
            }
            inv.setItem(slot++, createItem(Material.LEATHER_CHESTPLATE, ChatColor.GREEN + kit.getDisplayName(), kitLore));
        }

        // Back Button
        inv.setItem(31, createItem(Material.ARROW, ChatColor.YELLOW + "Back to Cosmetics"));

        player.openInventory(inv);
    }

    private ItemStack createSwordItem(Material material, String name, boolean isSelected, boolean hasAccess) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isSelected ? ChatColor.GREEN : ChatColor.YELLOW) + name);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Unbreakable Weapon");
        lore.add("");
        if (!hasAccess) {
            lore.add(ChatColor.RED + "LOCKED - Requires VIP");
        } else if (isSelected) {
            lore.add(ChatColor.GREEN + "SELECTED");
        } else {
            lore.add(ChatColor.YELLOW + "Click to select");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, int data, String name) {
        ItemStack item = new ItemStack(mat, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
