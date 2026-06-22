package me.molfordan.bridgefightplugin.cosmetics.gui;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.cosmetics.CosmeticsManager;
import me.molfordan.bridgefightplugin.cosmetics.objects.KillEffect;
import me.molfordan.bridgefightplugin.cosmetics.objects.KillMessage;
import me.molfordan.bridgefightplugin.cosmetics.objects.Trail;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsGUI {

    private final BridgeFightPlugin plugin;
    private final CosmeticsManager cosmeticsManager;

    public CosmeticsGUI(BridgeFightPlugin plugin, CosmeticsManager cosmeticsManager) {
        this.plugin = plugin;
        this.cosmeticsManager = cosmeticsManager;
    }

    public static final String MAIN_TITLE = ChatColor.DARK_GRAY + "Cosmetics Menu";
    public static final String KILL_MESSAGE_TITLE = ChatColor.DARK_GRAY + "Kill Messages";
    public static final String KILL_EFFECT_TITLE = ChatColor.DARK_GRAY + "Kill Effects";
    public static final String TRAIL_TITLE = ChatColor.DARK_GRAY + "Trails";

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);

        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, 15, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.DIAMOND_SWORD, ChatColor.YELLOW + "Sword & Armor Choice", ChatColor.GRAY + "Click to select sword and armor kit"));
        inv.setItem(12, createItem(Material.PAPER, ChatColor.YELLOW + "Kill Messages", ChatColor.GRAY + "Click to select a kill message"));
        inv.setItem(14, createItem(Material.BLAZE_POWDER, ChatColor.YELLOW + "Kill Effects", ChatColor.GRAY + "Click to select a kill effect"));
        inv.setItem(16, createItem(Material.NETHER_STAR, ChatColor.YELLOW + "Trails", ChatColor.GRAY + "Click to select a trail"));
        inv.setItem(22, createItem(Material.GOLD_INGOT, ChatColor.GOLD + "Your Balance",
                ChatColor.GRAY + "Coins: " + ChatColor.YELLOW + plugin.getBalanceManager().getBalance(player)));

        player.openInventory(inv);
    }

    public void openKillMessages(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, KILL_MESSAGE_TITLE);
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        String selected = stats.getSelectedKillMessage();

        int slot = 0;
        for (KillMessage km : cosmeticsManager.getKillMessages().values()) {
            boolean isSelected = km.getId().equals(selected);
            boolean isOwned = km.getRequiredBalance() <= 0 || stats.hasPurchasedKillMessage(km.getId());
            boolean hasBalance = plugin.getBalanceManager().hasBalance(player, km.getRequiredBalance());
            boolean hasPermission = hasRequiredPermission(player, km.getPermission());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Message: " + ChatColor.translateAlternateColorCodes('&', km.getMessage()));
            addPriceLore(lore, km.getRequiredBalance(), isOwned, hasBalance);
            addPermissionLore(lore, km.getPermission(), hasPermission);
            lore.add("");
            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED");
            } else if (!hasPermission) {
                lore.add(ChatColor.RED + "No permission");
            } else if (!isOwned && !hasBalance) {
                lore.add(ChatColor.RED + "Not enough coins");
            } else if (!isOwned) {
                lore.add(ChatColor.YELLOW + "Click to buy");
            } else {
                lore.add(ChatColor.YELLOW + "Click to select");
            }
            inv.setItem(slot++, createItem(isSelected ? Material.MAP : Material.PAPER, 
                    ChatColor.translateAlternateColorCodes('&', km.getDisplayName()), lore));
        }

        inv.setItem(31, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    public void openKillEffects(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, KILL_EFFECT_TITLE);
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        String selected = stats.getSelectedKillEffect();

        int slot = 0;
        for (KillEffect ke : cosmeticsManager.getKillEffects().values()) {
            boolean isSelected = ke.getId().equals(selected);
            boolean isOwned = ke.getRequiredBalance() <= 0 || stats.hasPurchasedKillEffect(ke.getId());
            boolean hasBalance = plugin.getBalanceManager().hasBalance(player, ke.getRequiredBalance());
            boolean hasPermission = hasRequiredPermission(player, ke.getPermission());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Effect: " + ChatColor.WHITE + ke.getEffectType());
            addPriceLore(lore, ke.getRequiredBalance(), isOwned, hasBalance);
            addPermissionLore(lore, ke.getPermission(), hasPermission);
            lore.add("");
            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED");
            } else if (!hasPermission) {
                lore.add(ChatColor.RED + "No permission");
            } else if (!isOwned && !hasBalance) {
                lore.add(ChatColor.RED + "Not enough coins");
            } else if (!isOwned) {
                lore.add(ChatColor.YELLOW + "Click to buy");
            } else {
                lore.add(ChatColor.YELLOW + "Click to select");
            }
            inv.setItem(slot++, createItem(isSelected ? Material.EYE_OF_ENDER : Material.ENDER_PEARL, 
                    ChatColor.translateAlternateColorCodes('&', ke.getDisplayName()), lore));
        }

        inv.setItem(31, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    public void openTrails(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TRAIL_TITLE);
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        String selected = stats.getSelectedTrail();

        int slot = 0;
        for (Trail trail : cosmeticsManager.getTrails().values()) {
            boolean isSelected = trail.getId().equals(selected);
            boolean isOwned = trail.getRequiredBalance() <= 0 || stats.hasPurchasedTrail(trail.getId());
            boolean hasBalance = plugin.getBalanceManager().hasBalance(player, trail.getRequiredBalance());
            boolean hasPermission = hasRequiredPermission(player, trail.getPermission());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Particle: " + ChatColor.WHITE + trail.getParticle());
            addPriceLore(lore, trail.getRequiredBalance(), isOwned, hasBalance);
            addPermissionLore(lore, trail.getPermission(), hasPermission);
            lore.add("");
            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED");
            } else if (!hasPermission) {
                lore.add(ChatColor.RED + "No permission");
            } else if (!isOwned && !hasBalance) {
                lore.add(ChatColor.RED + "Not enough coins");
            } else if (!isOwned) {
                lore.add(ChatColor.YELLOW + "Click to buy");
            } else {
                lore.add(ChatColor.YELLOW + "Click to select");
            }
            inv.setItem(slot++, createItem(isSelected ? Material.FIREWORK : Material.SULPHUR, 
                    ChatColor.translateAlternateColorCodes('&', trail.getDisplayName()), lore));
        }

        inv.setItem(31, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && lore.length > 0) meta.setLore(java.util.Arrays.asList(lore));
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

    private void addPriceLore(List<String> lore, int price, boolean isOwned, boolean hasBalance) {
        if (isOwned) {
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Owned");
            return;
        }

        if (price <= 0) {
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN + "Free");
            return;
        }

        lore.add(ChatColor.GRAY + "Price: " + (hasBalance ? ChatColor.GREEN : ChatColor.RED) + price + " coins");
    }

    private void addPermissionLore(List<String> lore, String permission, boolean hasPermission) {
        if (permission == null || permission.trim().isEmpty()) return;

        String display = getPermissionDisplay(permission);
        if (display == null || display.trim().isEmpty()) return;

        lore.add(ChatColor.GRAY + "Require: " + (hasPermission ? ChatColor.GREEN : ChatColor.RED) + display);
    }

    private boolean hasRequiredPermission(Player player, String permission) {
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    private String getPermissionDisplay(String permission) {
        String trimmed = permission.trim();
        if (!trimmed.toLowerCase().startsWith("group.")) {
            return "";
        }

        String groupName = trimmed.substring("group.".length());
        String prefix = getLuckPermsGroupPrefix(groupName);
        if (prefix != null && !prefix.trim().isEmpty()) {
            String translated = prefix
                    .replace("<aqua>", "&b")
                    .replace("<red>", "&c")
                    .replace("</red>", "&b")
                    .replace("</aqua>", "")
                    .replace("<dark_aqua>", "&3")
                    .replace("<gold>", "&6")
                    .replace("<yellow>", "&e")
                    .replace("<green>", "&a")
                    .replace("<dark_green>", "&2")
                    .replace("<blue>", "&9")
                    .replace("<dark_red>", "&4")
                    .replace("<purple>", "&5")
                    .replace("<light_purple>", "&d")
                    .replace("<white>", "&f")
                    .replace("<gray>", "&7")
                    .replace("<dark_gray>", "&8")
                    .replace("<black>", "&0");
            return ChatColor.translateAlternateColorCodes('&', translated);
        }

        return "[" + groupName.toUpperCase() + "]";
    }

    private String getLuckPermsGroupPrefix(String groupName) {
        org.bukkit.plugin.RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) return null;

        Group group = provider.getProvider().getGroupManager().getGroup(groupName);
        if (group == null) return null;

        return group.getCachedData().getMetaData().getPrefix();
    }
}
