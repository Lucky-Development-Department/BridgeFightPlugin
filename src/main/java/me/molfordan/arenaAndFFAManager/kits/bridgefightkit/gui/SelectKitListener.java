package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
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

        String title = e.getView().getTitle();
        if (title == null) return;

        if (!title.contains("Select BridgeFight Kit")) return;

        plugin.getBridgeFightGUI().handleClick(player, e);
        PlatformRegion region = plugin.getPlatformManager().fromLocationIgnoreY(player.getLocation());
        if (region != null && region.getSpawn() != null) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            plugin.getKitManager().applyBridgeFightKit(player);
        }
    }
}
