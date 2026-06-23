package me.molfordan.bridgefightplugin.cosmetics.gui;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.cosmetics.CosmeticsManager;
import me.molfordan.bridgefightplugin.cosmetics.objects.CosmeticTier;
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
        for (KillMessage km : getSortedKillMessages(player)) {
            boolean isSelected = km.getId().equals(selected);
            boolean isOwned = isKillMessageOwned(player, stats, km);
            boolean hasBalance = plugin.getBalanceManager().hasBalance(player, km.getRequiredBalance());
            boolean hasPermission = hasRequiredPermission(player, km.getPermission());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rarity: " + km.getTier().getFormattedName());
            lore.add(ChatColor.GRAY + "Message: " + ChatColor.translateAlternateColorCodes('&', km.getMessage()));
            addPriceLore(lore, km.getRequiredBalance(), isOwned, hasBalance, hasPermission, km.getPermission());
            addPermissionLore(lore, km.getPermission(), hasPermission, km.getRequiredBalance());
            lore.add("");

            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED");
            } else if (isOwned) {
                lore.add(ChatColor.YELLOW + "Click to select");
            } else {
                if (km.getRequiredBalance() > 0) {
                    if (hasBalance) {
                        lore.add(ChatColor.YELLOW + "Click to buy");
                    } else {
                        lore.add(ChatColor.RED + "Not enough coins");
                    }
                } else {
                    lore.add(ChatColor.RED + "Requires rank");
                }
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
        for (KillEffect ke : getSortedKillEffects(player)) {
            boolean isSelected = ke.getId().equals(selected);
            boolean isOwned = isKillEffectOwned(player, stats, ke);
            boolean hasBalance = plugin.getBalanceManager().hasBalance(player, ke.getRequiredBalance());
            boolean hasPermission = hasRequiredPermission(player, ke.getPermission());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rarity: " + ke.getTier().getFormattedName());
            lore.add(ChatColor.GRAY + "Effect: " + ChatColor.WHITE + ke.getEffectType());
            addPriceLore(lore, ke.getRequiredBalance(), isOwned, hasBalance, hasPermission, ke.getPermission());
            addPermissionLore(lore, ke.getPermission(), hasPermission, ke.getRequiredBalance());
            lore.add("");

            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED");
            } else if (isOwned) {
                lore.add(ChatColor.YELLOW + "Click to select");
            } else {
                if (ke.getRequiredBalance() > 0) {
                    if (hasBalance) {
                        lore.add(ChatColor.YELLOW + "Click to buy");
                    } else {
                        lore.add(ChatColor.RED + "Not enough coins");
                    }
                } else {
                    lore.add(ChatColor.RED + "Requires rank");
                }
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
        for (Trail trail : getSortedTrails(player)) {
            boolean isSelected = trail.getId().equals(selected);
            boolean isOwned = isTrailOwned(player, stats, trail);
            boolean hasBalance = plugin.getBalanceManager().hasBalance(player, trail.getRequiredBalance());
            boolean hasPermission = hasRequiredPermission(player, trail.getPermission());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rarity: " + trail.getTier().getFormattedName());
            lore.add(ChatColor.GRAY + "Particle: " + ChatColor.WHITE + trail.getParticle());
            addPriceLore(lore, trail.getRequiredBalance(), isOwned, hasBalance, hasPermission, trail.getPermission());
            addPermissionLore(lore, trail.getPermission(), hasPermission, trail.getRequiredBalance());
            lore.add("");

            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED");
            } else if (isOwned) {
                lore.add(ChatColor.YELLOW + "Click to select");
            } else {
                if (trail.getRequiredBalance() > 0) {
                    if (hasBalance) {
                        lore.add(ChatColor.YELLOW + "Click to buy");
                    } else {
                        lore.add(ChatColor.RED + "Not enough coins");
                    }
                } else {
                    lore.add(ChatColor.RED + "Requires rank");
                }
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

    private void addPriceLore(List<String> lore, int price, boolean isOwned, boolean hasBalance, boolean hasPermission, String permission) {
        if (isOwned) {
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Owned");
            return;
        }

        boolean hasPermissionCheck = permission != null && !permission.trim().isEmpty();
        if (price <= 0) {
            if (hasPermissionCheck && !hasPermission) {
                lore.add(ChatColor.GRAY + "Status: " + ChatColor.RED + "Locked (Rank Exclusive)");
            } else {
                lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN + "Free");
            }
            return;
        }

        lore.add(ChatColor.GRAY + "Price: " + (hasBalance ? ChatColor.GREEN : ChatColor.RED) + price + " coins");
    }

    private void addPermissionLore(List<String> lore, String permission, boolean hasPermission, int price) {
        if (permission == null || permission.trim().isEmpty()) return;

        String display = getPermissionDisplay(permission);
        if (display == null || display.trim().isEmpty()) return;

        if (hasPermission) {
            if (permission.trim().toLowerCase().startsWith("group.")) {
                lore.add(ChatColor.GRAY + "Unlocked via: " + ChatColor.GREEN + display);
            }
        } else {
            if (price > 0) {
                if (permission.trim().toLowerCase().startsWith("group.")) {
                    lore.add(ChatColor.GRAY + "Free with: " + ChatColor.YELLOW + display);
                }
            } else {
                lore.add(ChatColor.GRAY + "Requires: " + ChatColor.RED + display);
            }
        }
    }

    public boolean hasRequiredPermission(Player player, String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }

        String trimmed = permission.trim();
        if (player.hasPermission(trimmed)) {
            return true;
        }

        // Handle equivalent group permissions
        if (trimmed.equalsIgnoreCase("group.lucky")) {
            return player.hasPermission("group.spark");
        } else if (trimmed.equalsIgnoreCase("group.spark")) {
            return player.hasPermission("group.lucky");
        } else if (trimmed.equalsIgnoreCase("group.supreme")) {
            return player.hasPermission("group.aura");
        } else if (trimmed.equalsIgnoreCase("group.aura")) {
            return player.hasPermission("group.supreme");
        }

        return false;
    }

    public boolean isKillMessageOwned(Player player, PlayerStats stats, KillMessage km) {
        if (stats.hasPurchasedKillMessage(km.getId())) {
            return true;
        }
        if (km.getPermission() != null && !km.getPermission().trim().isEmpty()) {
            if (hasRequiredPermission(player, km.getPermission())) {
                return true;
            }
        }
        if ((km.getPermission() == null || km.getPermission().trim().isEmpty()) && km.getRequiredBalance() <= 0) {
            return true;
        }
        return false;
    }

    public boolean isKillEffectOwned(Player player, PlayerStats stats, KillEffect ke) {
        if (stats.hasPurchasedKillEffect(ke.getId())) {
            return true;
        }
        if (ke.getPermission() != null && !ke.getPermission().trim().isEmpty()) {
            if (hasRequiredPermission(player, ke.getPermission())) {
                return true;
            }
        }
        if ((ke.getPermission() == null || ke.getPermission().trim().isEmpty()) && ke.getRequiredBalance() <= 0) {
            return true;
        }
        return false;
    }

    public boolean isTrailOwned(Player player, PlayerStats stats, Trail trail) {
        if (stats.hasPurchasedTrail(trail.getId())) {
            return true;
        }
        if (trail.getPermission() != null && !trail.getPermission().trim().isEmpty()) {
            if (hasRequiredPermission(player, trail.getPermission())) {
                return true;
            }
        }
        if ((trail.getPermission() == null || trail.getPermission().trim().isEmpty()) && trail.getRequiredBalance() <= 0) {
            return true;
        }
        return false;
    }

    public void sendMissingPermission(Player player, String permission) {
        player.sendMessage(ChatColor.RED + "You do not have permission to select this cosmetic. "
                + ChatColor.GRAY + "Required: " + getPermissionDisplay(permission));
    }

    private String getPermissionDisplay(String permission) {
        if (permission == null || permission.trim().isEmpty()) return "";

        String trimmed = permission.trim();
        if (!trimmed.toLowerCase().startsWith("group.")) {
            return ChatColor.RED + trimmed;
        }

        String groupName = trimmed.substring("group.".length());

        List<String> groupsToTry = new ArrayList<>();
        groupsToTry.add(groupName);
        if (groupName.equalsIgnoreCase("lucky")) {
            groupsToTry.add("spark");
        } else if (groupName.equalsIgnoreCase("spark")) {
            groupsToTry.add("lucky");
        } else if (groupName.equalsIgnoreCase("supreme")) {
            groupsToTry.add("aura");
        } else if (groupName.equalsIgnoreCase("aura")) {
            groupsToTry.add("supreme");
        }

        List<String> formattedPrefixes = new ArrayList<>();
        for (String gName : groupsToTry) {
            String prefix = getLuckPermsGroupPrefix(gName);
            if (prefix != null && !prefix.trim().isEmpty()) {
                String translated = prefix
                        .replace("<bold>", "&l")
                        .replace("<b>", "&l")
                        .replace("</bold>", "")
                        .replace("</b>", "")
                        .replace("<italic>", "&o")
                        .replace("<i>", "&o")
                        .replace("</italic>", "")
                        .replace("</i>", "")
                        .replace("<underlined>", "&n")
                        .replace("<u>", "&n")
                        .replace("</underlined>", "")
                        .replace("</u>", "")
                        .replace("<strikethrough>", "&m")
                        .replace("<st>", "&m")
                        .replace("</strikethrough>", "")
                        .replace("</st>", "")
                        .replace("<obfuscated>", "&k")
                        .replace("<obf>", "&k")
                        .replace("</obfuscated>", "")
                        .replace("</obf>", "")
                        .replace("<reset>", "&r")
                        .replace("</reset>", "")
                        .replace("</color>", "")
                        .replace("<aqua>", "&b")
                        .replace("</aqua>", "&b")
                        .replace("<red>", "&c")
                        .replace("</red>", "&c")
                        .replace("<dark_aqua>", "&3")
                        .replace("</dark_aqua>", "&3")
                        .replace("<gold>", "&6")
                        .replace("</gold>", "&6")
                        .replace("<yellow>", "&e")
                        .replace("</yellow>", "&e")
                        .replace("<green>", "&a")
                        .replace("</green>", "&a")
                        .replace("<dark_green>", "&2")
                        .replace("</dark_green>", "&2")
                        .replace("<blue>", "&9")
                        .replace("</blue>", "&9")
                        .replace("<dark_blue>", "&1")
                        .replace("</dark_blue>", "&1")
                        .replace("<dark_red>", "&4")
                        .replace("</dark_red>", "&4")
                        .replace("<purple>", "&5")
                        .replace("</purple>", "&5")
                        .replace("<dark_purple>", "&5")
                        .replace("</dark_purple>", "&5")
                        .replace("<light_purple>", "&d")
                        .replace("</light_purple>", "&d")
                        .replace("<pink>", "&d")
                        .replace("</pink>", "&d")
                        .replace("<white>", "&f")
                        .replace("</white>", "&f")
                        .replace("<gray>", "&7")
                        .replace("</gray>", "&7")
                        .replace("<dark_gray>", "&8")
                        .replace("</dark_gray>", "&8")
                        .replace("<black>", "&0")
                        .replace("</black>", "&0")
                        .replaceAll("<[^>]*>", ""); // strip any remaining unrecognised MiniMessage tags (e.g. <color:#RRGGBB>, </color>)
                formattedPrefixes.add(ChatColor.translateAlternateColorCodes('&', translated));
            } else {
                formattedPrefixes.add("[" + gName.toUpperCase() + "]");
            }
        }

        return String.join(ChatColor.GRAY + " or ", formattedPrefixes);
    }

    private String getLuckPermsGroupPrefix(String groupName) {
        org.bukkit.plugin.RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) return null;

        Group group = provider.getProvider().getGroupManager().getGroup(groupName);
        if (group == null) return null;

        return group.getCachedData().getMetaData().getPrefix();
    }

    public List<KillMessage> getSortedKillMessages(Player player) {
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        List<KillMessage> list = new ArrayList<>(cosmeticsManager.getKillMessages().values());
        list.sort((km1, km2) -> {
            int tierCompare = Integer.compare(km1.getTier().ordinal(), km2.getTier().ordinal());
            if (tierCompare != 0) return tierCompare;
            boolean owned1 = isKillMessageOwned(player, stats, km1);
            boolean owned2 = isKillMessageOwned(player, stats, km2);
            if (owned1 != owned2) return owned1 ? -1 : 1;
            return 0;
        });
        return list;
    }

    public List<KillEffect> getSortedKillEffects(Player player) {
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        List<KillEffect> list = new ArrayList<>(cosmeticsManager.getKillEffects().values());
        list.sort((ke1, ke2) -> {
            int tierCompare = Integer.compare(ke1.getTier().ordinal(), ke2.getTier().ordinal());
            if (tierCompare != 0) return tierCompare;
            boolean owned1 = isKillEffectOwned(player, stats, ke1);
            boolean owned2 = isKillEffectOwned(player, stats, ke2);
            if (owned1 != owned2) return owned1 ? -1 : 1;
            return 0;
        });
        return list;
    }

    public List<Trail> getSortedTrails(Player player) {
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        List<Trail> list = new ArrayList<>(cosmeticsManager.getTrails().values());
        list.sort((t1, t2) -> {
            int tierCompare = Integer.compare(t1.getTier().ordinal(), t2.getTier().ordinal());
            if (tierCompare != 0) return tierCompare;
            boolean owned1 = isTrailOwned(player, stats, t1);
            boolean owned2 = isTrailOwned(player, stats, t2);
            if (owned1 != owned2) return owned1 ? -1 : 1;
            return 0;
        });
        return list;
    }
}
