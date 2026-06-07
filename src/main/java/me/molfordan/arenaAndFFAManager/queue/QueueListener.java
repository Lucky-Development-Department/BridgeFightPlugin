package me.molfordan.arenaAndFFAManager.queue;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class QueueListener implements Listener {
    private final ArenaAndFFAManager plugin;

    public QueueListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BED) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        if (item.getItemMeta().getDisplayName().contains("Leave Queue")) {
            event.setCancelled(true);
            player.performCommand("queue leave");
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        org.bukkit.inventory.Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        String title = event.getView().getTitle();

        if (title.equals(org.bukkit.ChatColor.BLUE + "Select Queue Type")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 3) {
                plugin.getQueueGUI().openSoloType(player);
            }
        } else if (title.equals(org.bukkit.ChatColor.BLUE + "Select Solo Mode")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 3) {
                player.closeInventory();
                plugin.getQueueManager().addSoloUnranked(player);
            }
        }
    }
}
