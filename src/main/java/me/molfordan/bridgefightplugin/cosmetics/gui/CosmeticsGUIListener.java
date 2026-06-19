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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsGUIListener implements Listener {

    private final BridgeFightPlugin plugin;
    private final CosmeticsGUI cosmeticsGUI;
    private final CosmeticsManager cosmeticsManager;

    public CosmeticsGUIListener(BridgeFightPlugin plugin, CosmeticsGUI cosmeticsGUI, CosmeticsManager cosmeticsManager) {
        this.plugin = plugin;
        this.cosmeticsGUI = cosmeticsGUI;
        this.cosmeticsManager = cosmeticsManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        if (title.equals(CosmeticsGUI.MAIN_TITLE)) {
            event.setCancelled(true);
            if (item.getType() == Material.PAPER) {
                cosmeticsGUI.openKillMessages(player);
            } else if (item.getType() == Material.BLAZE_POWDER) {
                cosmeticsGUI.openKillEffects(player);
            } else if (item.getType() == Material.NETHER_STAR) {
                cosmeticsGUI.openTrails(player);
            }
        } else if (title.equals(CosmeticsGUI.KILL_MESSAGE_TITLE)) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                cosmeticsGUI.openMain(player);
                return;
            }
            
            List<KillMessage> killMessages = new ArrayList<>(cosmeticsManager.getKillMessages().values());
            if (event.getSlot() < 0 || event.getSlot() >= killMessages.size()) return;

            KillMessage km = killMessages.get(event.getSlot());
            if (km != null) {
                if (!hasRequiredPermission(player, km.getPermission())) {
                    sendMissingPermission(player, km.getPermission());
                    return;
                }

                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                if (!stats.hasPurchasedKillMessage(km.getId())) {
                    if (!purchaseCosmetic(player, km.getRequiredBalance())) return;
                    stats.addPurchasedKillMessage(km.getId());
                    player.sendMessage(ChatColor.GREEN + "Purchased kill message: " + ChatColor.translateAlternateColorCodes('&', km.getDisplayName()));
                }

                stats.setSelectedKillMessage(km.getId());
                plugin.getStatsManager().savePlayer(stats);
                player.sendMessage(ChatColor.GREEN + "Selected kill message: " + ChatColor.translateAlternateColorCodes('&', km.getDisplayName()));
                cosmeticsGUI.openKillMessages(player);
            }
        } else if (title.equals(CosmeticsGUI.KILL_EFFECT_TITLE)) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                cosmeticsGUI.openMain(player);
                return;
            }

            List<KillEffect> killEffects = new ArrayList<>(cosmeticsManager.getKillEffects().values());
            if (event.getSlot() < 0 || event.getSlot() >= killEffects.size()) return;

            KillEffect ke = killEffects.get(event.getSlot());
            if (ke != null) {
                if (!hasRequiredPermission(player, ke.getPermission())) {
                    sendMissingPermission(player, ke.getPermission());
                    return;
                }

                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                if (!stats.hasPurchasedKillEffect(ke.getId())) {
                    if (!purchaseCosmetic(player, ke.getRequiredBalance())) return;
                    stats.addPurchasedKillEffect(ke.getId());
                    player.sendMessage(ChatColor.GREEN + "Purchased kill effect: " + ChatColor.translateAlternateColorCodes('&', ke.getDisplayName()));
                }

                stats.setSelectedKillEffect(ke.getId());
                plugin.getStatsManager().savePlayer(stats);

                // Update kill effect cache
                if (plugin.getDeathMessageManager() != null) {
                    plugin.getDeathMessageManager().updateKillEffectCache(player);
                }

                player.sendMessage(ChatColor.GREEN + "Selected kill effect: " + ChatColor.translateAlternateColorCodes('&', ke.getDisplayName()));
                cosmeticsGUI.openKillEffects(player);
            }
        } else if (title.equals(CosmeticsGUI.TRAIL_TITLE)) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                cosmeticsGUI.openMain(player);
                return;
            }

            List<Trail> trails = new ArrayList<>(cosmeticsManager.getTrails().values());
            if (event.getSlot() < 0 || event.getSlot() >= trails.size()) return;

            Trail trail = trails.get(event.getSlot());
            if (trail != null) {
                if (!hasRequiredPermission(player, trail.getPermission())) {
                    sendMissingPermission(player, trail.getPermission());
                    return;
                }

                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                if (!stats.hasPurchasedTrail(trail.getId())) {
                    if (!purchaseCosmetic(player, trail.getRequiredBalance())) return;
                    stats.addPurchasedTrail(trail.getId());
                    player.sendMessage(ChatColor.GREEN + "Purchased trail: " + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName()));
                }

                stats.setSelectedTrail(trail.getId());
                plugin.getStatsManager().savePlayer(stats);
                
                // Update trail cache
                if (plugin.getCosmeticsListener() != null) {
                    plugin.getCosmeticsListener().updatePlayerTrailCache(player);
                }

                player.sendMessage(ChatColor.GREEN + "Selected trail: " + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName()));
                cosmeticsGUI.openTrails(player);
            }
        }
    }

    private boolean purchaseCosmetic(Player player, int price) {
        if (price <= 0) return true;

        if (plugin.getBalanceManager().removeBalance(player, price)) {
            return true;
        }

        int currentBalance = plugin.getBalanceManager().getBalance(player);
        player.sendMessage(ChatColor.RED + "You need " + price + " coins to buy this cosmetic. "
                + ChatColor.GRAY + "Your balance: " + currentBalance + " coins.");
        return false;
    }

    private boolean hasRequiredPermission(Player player, String permission) {
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    private void sendMissingPermission(Player player, String permission) {
        player.sendMessage(ChatColor.RED + "You do not have permission to select this cosmetic. "
                + ChatColor.GRAY + "Required: " + getPermissionDisplay(permission));
    }

    private String getPermissionDisplay(String permission) {
        if (permission == null || permission.trim().isEmpty()) return "Special Access";

        String trimmed = permission.trim();
        if (!trimmed.toLowerCase().startsWith("group.")) {
            return "Special Access";
        }

        String groupName = trimmed.substring("group.".length());
        String prefix = getLuckPermsGroupPrefix(groupName);
        if (prefix != null && !prefix.trim().isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', prefix);
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
