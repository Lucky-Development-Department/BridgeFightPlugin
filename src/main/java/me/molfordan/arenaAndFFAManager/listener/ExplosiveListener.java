package me.molfordan.arenaAndFFAManager.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosiveListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onExplode(EntityExplodeEvent event) {
        // This applies to all explosives (TNT, Fireballs, Creepers, etc.)
        // We only allow blocks with specific metadata to be destroyed.
        event.blockList().removeIf(block -> 
            !block.hasMetadata("player_blocks") && 
            !block.hasMetadata("egg_bridge_block")
        );
        
        // If there are still blocks left in the list, we don't cancel the event,
        // so Bukkit handles the actual block removal and visuals.
        // If we want to ensure blocks ARE removed even if the event was cancelled elsewhere,
        // we might need a different approach, but removeIf is standard for EntityExplodeEvent.
    }
}
