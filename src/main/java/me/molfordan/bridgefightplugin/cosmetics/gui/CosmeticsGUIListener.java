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
            } else if (item.getType() == Material.DIAMOND_SWORD) {
                new me.molfordan.bridgefightplugin.kits.bridgefightkit.SwordChoiceGUI(plugin).open(player);
            }
        } else if (title.equals(CosmeticsGUI.KILL_MESSAGE_TITLE)) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                cosmeticsGUI.openMain(player);
                return;
            }
            
            List<KillMessage> killMessages = cosmeticsGUI.getSortedKillMessages(player);
            if (event.getSlot() < 0 || event.getSlot() >= killMessages.size()) return;

            KillMessage km = killMessages.get(event.getSlot());
            if (km != null) {
                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                boolean isOwned = cosmeticsGUI.isKillMessageOwned(player, stats, km);

                if (!isOwned) {
                    if (km.getRequiredBalance() > 0) {
                        if (!purchaseCosmetic(player, km.getRequiredBalance())) return;
                        stats.addPurchasedKillMessage(km.getId());
                        plugin.getStatsManager().savePlayer(stats);
                        player.sendMessage(ChatColor.GREEN + "Purchased kill message: " + ChatColor.translateAlternateColorCodes('&', km.getDisplayName()));
                    } else {
                        cosmeticsGUI.sendMissingPermission(player, km.getPermission());
                        return;
                    }
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

            List<KillEffect> killEffects = cosmeticsGUI.getSortedKillEffects(player);
            if (event.getSlot() < 0 || event.getSlot() >= killEffects.size()) return;

            KillEffect ke = killEffects.get(event.getSlot());
            if (ke != null) {
                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                boolean isOwned = cosmeticsGUI.isKillEffectOwned(player, stats, ke);

                if (!isOwned) {
                    if (ke.getRequiredBalance() > 0) {
                        if (!purchaseCosmetic(player, ke.getRequiredBalance())) return;
                        stats.addPurchasedKillEffect(ke.getId());
                        plugin.getStatsManager().savePlayer(stats);
                        player.sendMessage(ChatColor.GREEN + "Purchased kill effect: " + ChatColor.translateAlternateColorCodes('&', ke.getDisplayName()));
                    } else {
                        cosmeticsGUI.sendMissingPermission(player, ke.getPermission());
                        return;
                    }
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

            List<Trail> trails = cosmeticsGUI.getSortedTrails(player);
            if (event.getSlot() < 0 || event.getSlot() >= trails.size()) return;

            Trail trail = trails.get(event.getSlot());
            if (trail != null) {
                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                boolean isOwned = cosmeticsGUI.isTrailOwned(player, stats, trail);

                if (!isOwned) {
                    if (trail.getRequiredBalance() > 0) {
                        if (!purchaseCosmetic(player, trail.getRequiredBalance())) return;
                        stats.addPurchasedTrail(trail.getId());
                        plugin.getStatsManager().savePlayer(stats);
                        player.sendMessage(ChatColor.GREEN + "Purchased trail: " + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName()));
                    } else {
                        cosmeticsGUI.sendMissingPermission(player, trail.getPermission());
                        return;
                    }
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
}
