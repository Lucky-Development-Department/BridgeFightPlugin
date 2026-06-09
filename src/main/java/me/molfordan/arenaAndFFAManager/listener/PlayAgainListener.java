package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayAgainListener implements Listener {
    private final ArenaAndFFAManager plugin;

    public PlayAgainListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    private boolean isPlayAgainItem(ItemStack item) {
        return item != null && item.getType() == Material.PAPER && 
               item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
               item.getItemMeta().getDisplayName().contains("Play Again (Right Click)");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isPlayAgainItem(item)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        player.performCommand("playagain");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isPlayAgainItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isPlayAgainItem(event.getCurrentItem()) || isPlayAgainItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }
}
