package me.molfordan.arenaAndFFAManager.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class MasterPickupDebugger implements Listener {
    /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (event.getPlayer().getWorld().getName().startsWith("bf_")) {
            Bukkit.getLogger().info("MasterPickupDebugger: Player " + event.getPlayer().getName() + 
                " attempting to pickup " + event.getItem().getItemStack().getType() + 
                ". Event Cancelled: " + event.isCancelled());
        }
    }

     */
}
