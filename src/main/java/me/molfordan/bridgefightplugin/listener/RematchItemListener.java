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

public class RematchItemListener implements Listener {
    private final BridgeFightPlugin plugin;

    public RematchItemListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isRematchItem(ItemStack item) {
        return item != null && item.getType() == Material.PAPER && 
               item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
               item.getItemMeta().getDisplayName().contains("Rematch (Right Click)");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isRematchItem(item)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        player.performCommand("rematch");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isRematchItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isRematchItem(event.getCurrentItem()) || isRematchItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }
}
