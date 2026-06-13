package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PartyQueueGUIListener implements Listener {
    private final BridgeFightPlugin plugin;

    public PartyQueueGUIListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Queue Selection")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        Player player = (Player) event.getWhoClicked();
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Party Split")) {
            player.closeInventory();
            plugin.getMatchmakingService().addToQueue(player, QueueType.PARTY_SPLIT);
        } else if (name.contains("Party Match")) {
            player.closeInventory();
            me.molfordan.bridgefightplugin.gui.PartyListGUI.open(player, plugin);
        } else if (name.contains("Duo Queue")) {
            player.closeInventory();
            plugin.getMatchmakingService().addToQueue(player, QueueType.PARTY_DUO_QUEUE);
        }
    }
}
