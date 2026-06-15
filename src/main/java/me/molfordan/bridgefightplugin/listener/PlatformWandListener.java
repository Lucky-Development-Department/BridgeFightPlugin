package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.manager.PlatformManager;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlatformWandListener implements Listener {

    private final PlatformManager platformManager;

    public PlatformWandListener(PlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    private String getPlatformName(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_AXE || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta.getDisplayName() != null && meta.getDisplayName().startsWith("§aPlatform Wand: ")) {
            return meta.getDisplayName().substring("§aPlatform Wand: ".length());
        }
        return null;
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        String platName = getPlatformName(item);
        if (platName == null) return;

        if (e.getClickedBlock() == null) return;

        PlatformRegion region = platformManager.getPlatform(platName);
        
        // Ensure type is set
        String lowerName = platName.toLowerCase();
        if (region.getType() == null) {
            if (lowerName.startsWith("boxingplat")) {
                region.setType(me.molfordan.bridgefightplugin.object.enums.PlatformType.BOXINGPLAT);
            } else if (lowerName.startsWith("bigplat")) {
                region.setType(me.molfordan.bridgefightplugin.object.enums.PlatformType.BIGPLAT);
            } else if (lowerName.startsWith("plat")) {
                region.setType(me.molfordan.bridgefightplugin.object.enums.PlatformType.PLAT);
            }
        }
        
        if (region == null) {
            e.getPlayer().sendMessage("§cPlatform §e" + platName + " §cnot found.");
            return;
        }

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            region.setPos1(e.getClickedBlock().getLocation());
            platformManager.savePlatformPos(platName, "pos1", e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage("§aPos1 set for §e" + platName + "§a.");
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            region.setPos2(e.getClickedBlock().getLocation());
            platformManager.savePlatformPos(platName, "pos2", e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage("§aPos2 set for §e" + platName + "§a.");
            e.setCancelled(true);
        }
    }
}
