package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class StatsGUIListener implements Listener {

    private final BridgeFightPlugin plugin;

    public StatsGUIListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        // Check by inventory title using the ChatColor codes from StatsGUI
        String title = event.getView().getTitle();
        if (title.startsWith("§6Stats: §e")) {
            event.setCancelled(true);
        }
    }
}
