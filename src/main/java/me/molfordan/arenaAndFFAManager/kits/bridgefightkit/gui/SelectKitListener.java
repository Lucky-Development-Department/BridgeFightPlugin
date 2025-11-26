package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SelectKitListener implements Listener {

    private final ArenaAndFFAManager plugin;

    public SelectKitListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();

        if (!e.getView().getTitle().equals("Select BridgeFight Kit")) return;

        plugin.getBridgeFightGUI().handleClick(player, e);
    }
}
