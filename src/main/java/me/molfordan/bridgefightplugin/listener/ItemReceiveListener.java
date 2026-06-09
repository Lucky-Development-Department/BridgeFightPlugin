package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.hotbarmanager.HotbarSorter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class ItemReceiveListener implements Listener {

    private final String buildFFAWorld;
    private final HotbarSorter sorter;

    public ItemReceiveListener(String buildFFAWorld, HotbarSorter sorter) {
        this.buildFFAWorld = buildFFAWorld;
        this.sorter = sorter;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(PlayerPickupItemEvent e) {
        // If it's a BedFight world, allow normal pickup behavior
        if (e.getPlayer().getWorld().getName().startsWith("bf_")) {
            Bukkit.getLogger().info("Debug Pickup: ItemReceiveListener ignored for BedFight world.");
            return;
        }

        // Only sort if it's the BuildFFA world
        Player p = e.getPlayer();
        if (!p.getWorld().getName().equalsIgnoreCase(buildFFAWorld)) return;

        ItemStack item = e.getItem().getItemStack().clone();

        Bukkit.getScheduler().runTaskLater(BridgeFightPlugin.getPlugin(), () ->
                sorter.sortToHotbar(p, item), 1);
    }



    // catches ANY inventory updates after /give or reward
    @EventHandler
    public void onInventoryUpdate(InventoryClickEvent e) {
        return; // REMOVE ALL SORTING HERE
    }
}

