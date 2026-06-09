package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PartyItemListener implements Listener {
    private final BridgeFightPlugin plugin;

    public PartyItemListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPartyItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name != null && (name.contains("Party Queue") || name.contains("Leave Party"));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (item.getType() == Material.DIAMOND_SWORD && item.getItemMeta().getDisplayName().contains("Party Queue")) {
            me.molfordan.bridgefightplugin.gui.PartyQueueGUI.open(player);
        } else if (item.getType() == Material.BED && item.getItemMeta().getDisplayName().contains("Leave Party")) {
            player.performCommand("bfparty leave");
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isPartyItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isPartyItem(event.getCurrentItem()) || isPartyItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }
}
