package me.molfordan.bridgefightplugin.queue;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class QueueListener implements Listener {
    private final BridgeFightPlugin plugin;

    public QueueListener(BridgeFightPlugin plugin) {
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
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        String title = event.getView().getTitle();

        if (title.equals(ChatColor.BLUE + "Select Queue Type")) {
            event.setCancelled(true);
            switch (event.getRawSlot()) {
                case 3: plugin.getQueueGUI().openSoloType(player); break;
                case 5: plugin.getQueueGUI().openDuoType(player); break;
                case 6: plugin.getQueueGUI().openPartyType(player); break;
            }
        } else if (title.equals(ChatColor.BLUE + "Select Solo Mode")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 3) joinQueue(player, QueueType.SOLO_UNRANKED);
            if (event.getRawSlot() == 5) joinQueue(player, QueueType.SOLO_RANKED);
        } else if (title.equals(ChatColor.BLUE + "Select Duo Mode")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 3) joinQueue(player, QueueType.DUO_UNRANKED);
            if (event.getRawSlot() == 5) joinQueue(player, QueueType.DUO_RANKED);
        } else if (title.equals(ChatColor.BLUE + "Select Party Mode")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 3) joinQueue(player, QueueType.PARTY_FIGHT);
            if (event.getRawSlot() == 5) joinQueue(player, QueueType.PARTY_SPLIT);
        }
    }

    private void joinQueue(Player player, QueueType type) {
        player.closeInventory();
        plugin.getMatchmakingService().addToQueue(player, type);
    }
}
