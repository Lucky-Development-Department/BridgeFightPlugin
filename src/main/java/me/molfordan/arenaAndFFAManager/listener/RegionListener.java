package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.region.CommandRegion;
import me.molfordan.arenaAndFFAManager.region.CommandRegionManager;
import me.molfordan.arenaAndFFAManager.region.FlagType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RegionListener implements Listener {

    private final CommandRegionManager manager;

    public RegionListener(CommandRegionManager manager) {
        this.manager = manager;
    }

    private boolean isWand(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.STONE_AXE) return false;
        if (!item.hasItemMeta()) return false;

        return "§aRegion Wand".equals(item.getItemMeta().getDisplayName());
    }

    @EventHandler
    public void onWand(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (!isWand(item)) return;

        if (e.getClickedBlock() == null) return;

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            manager.setPos1(e.getPlayer(), e.getClickedBlock().getLocation());
            e.setCancelled(true); // prevent breaking block
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            manager.setPos2(e.getPlayer(), e.getClickedBlock().getLocation());
            e.setCancelled(true); // prevent placing block
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        // Prevent wand from breaking blocks
        if (isWand(e.getPlayer().getItemInHand())) {
            e.setCancelled(true);
            return;
        }
        /*
        // REGION BUILD FLAG CHECK
        for (CommandRegion r : manager.getRegions()) {
            if (r.isInside(e.getBlock().getLocation())) {
                String flag = r.getFlag("build");
                if ("deny".equalsIgnoreCase(flag)) {
                    e.setCancelled(true);
                    //e.getPlayer().sendMessage("§cYou cannot build here.");
                }
            }
        }

         */
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {

        // Wand should never be blocked
        if (isWand(e.getPlayer().getItemInHand())) return;

        // Check only the block's location (NOT the player's location)
        for (CommandRegion r : manager.getRegions()) {

            // If the block is inside the region AND this region denies building
            if (r.isInside(e.getBlock().getLocation()) && r.isBuildDenied()) {
                e.setCancelled(true);
                return;
            }
        }
    }


}
