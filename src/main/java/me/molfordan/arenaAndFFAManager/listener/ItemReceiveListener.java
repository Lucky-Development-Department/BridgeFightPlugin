package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarSorter;
import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class ItemReceiveListener implements Listener {

    private final String buildFFAWorld;
    private final HotbarSorter sorter;

    public ItemReceiveListener(String buildFFAWorld, HotbarSorter sorter) {
        this.buildFFAWorld = buildFFAWorld;
        this.sorter = sorter;
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        if (!p.getWorld().getName().equalsIgnoreCase(buildFFAWorld)) return;

        ItemStack item = e.getItem().getItemStack().clone();

        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () ->
                sorter.sortToHotbar(p, item), 1);
    }

    // catches ANY inventory updates after /give or reward
    @EventHandler
    public void onInventoryUpdate(InventoryClickEvent e) {
        return; // REMOVE ALL SORTING HERE
    }
}

