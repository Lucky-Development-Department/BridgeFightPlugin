package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PartyQueueGUIListener implements Listener {
    private final ArenaAndFFAManager plugin;

    public PartyQueueGUIListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Queue Selection")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Party Split")) {
            player.closeInventory();
            plugin.getMatchmakingService().addToQueue(player, QueueType.PARTY_SPLIT);
        } else if (name.contains("Party Match")) {
            player.closeInventory();
            me.molfordan.arenaAndFFAManager.gui.PartyListGUI.open(player, plugin);
        } else if (name.contains("Duo Queue")) {
            player.closeInventory();
            plugin.getMatchmakingService().addToQueue(player, QueueType.PARTY_DUO_QUEUE);
        }
    }
}
