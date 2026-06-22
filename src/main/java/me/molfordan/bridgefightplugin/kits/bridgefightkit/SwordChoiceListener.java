package me.molfordan.bridgefightplugin.kits.bridgefightkit;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
import me.molfordan.bridgefightplugin.object.enums.PlatformType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.ArrayList;
import java.util.List;

public class SwordChoiceListener implements Listener {

    private final BridgeFightPlugin plugin;

    public SwordChoiceListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getInventory().getTitle();
        if (!title.equals(SwordChoiceGUI.TITLE)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        SwordChoiceManager swordManager = plugin.getSwordChoiceManager();
        boolean hasVip = player.hasPermission("group.vip") || player.isOp();

        // 1. Sword Selection
        if (slot >= 10 && slot <= 14) {
            Material selectedSword = null;
            switch (slot) {
                case 10: selectedSword = Material.WOOD_SWORD; break;
                case 11: selectedSword = Material.STONE_SWORD; break;
                case 12: selectedSword = Material.GOLD_SWORD; break;
                case 13: selectedSword = Material.IRON_SWORD; break;
                case 14: selectedSword = Material.DIAMOND_SWORD; break;
            }

            if (selectedSword != null) {
                if (selectedSword == Material.WOOD_SWORD || hasVip) {
                    swordManager.setSelectedSword(player.getUniqueId(), selectedSword);
                    player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    applyKit(player);
                    new SwordChoiceGUI(plugin).open(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You need group.vip (or above) to use this sword!");
                    player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1.0f, 1.0f);
                }
            }
        }

        // 2. Sharpness Toggle
        else if (slot == 16) {
            boolean hasSharp = swordManager.hasSharpness(player.getUniqueId());
            swordManager.setSharpness(player.getUniqueId(), !hasSharp);
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            applyKit(player);
            new SwordChoiceGUI(plugin).open(player);
        }

        // 3. No Armor Choice
        else if (slot == 20) {
            swordManager.setSelectedArmorKit(player.getUniqueId(), "None");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            applyKit(player);
            new SwordChoiceGUI(plugin).open(player);
        }

        // 4. Armor Kit Selection
        else if (slot >= 21 && slot <= 26) {
            int index = slot - 21;
            List<Kit2> kits = new ArrayList<>(plugin.getBridgeFightKitManager().getAllKits());
            if (index >= 0 && index < kits.size()) {
                Kit2 kit = kits.get(index);
                if (!kit.hasPermission(player)) {
                    player.sendMessage(ChatColor.RED + "You need " + kit.getPermission() + " to use this armor kit!");
                    player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1.0f, 1.0f);
                    return;
                }
                swordManager.setSelectedArmorKit(player.getUniqueId(), kit.getName());
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                applyKit(player);
                new SwordChoiceGUI(plugin).open(player);
            }
        }

        // 5. Back Button
        else if (slot == 31) {
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            plugin.getCosmeticsGUI().openMain(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!event.getInventory().getTitle().equals(SwordChoiceGUI.TITLE)) return;

        Player player = (Player) event.getPlayer();
        applyKit(player);
    }

    /**
     * Applies the BridgeFight kit to the player based on their current sword/armor selections.
     * Only applies if the player is currently in an active BridgeFight game.
     */
    private void applyKit(Player player) {
        if (!plugin.getPlatformManager().isInPlatform(player)) {
            return;
        }
        plugin.getKitManager().applyBridgeFightKit(player);
    }
}
