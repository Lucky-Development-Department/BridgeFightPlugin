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

        // Check by inventory title (Java 8 compatible)
        if (event.getView().getTitle().equals("§9Your Statistics") || event.getView().getTitle().startsWith("§6Stats: §e")) {
            event.setCancelled(true); // ⛔ block taking items
        }
    }
}
