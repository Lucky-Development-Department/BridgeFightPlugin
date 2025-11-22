package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class StatsGUIListener implements Listener {

    private final ArenaAndFFAManager plugin;

    public StatsGUIListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (event.getClickedInventory() == null) return;

        // Check by inventory title (Java 8 compatible)
        if (event.getView().getTitle().startsWith("§6Stats: §e")) {
            event.setCancelled(true); // ⛔ block taking items
        }
    }
}
