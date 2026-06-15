package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.bedfight.BedFightArenaManager;
import me.molfordan.bridgefightplugin.bedfight.BedFightSetupSession;
import me.molfordan.bridgefightplugin.object.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BedFightSetupListener implements Listener {

    private final BridgeFightPlugin plugin;
    private final BedFightArenaManager arenaManager;

    public BedFightSetupListener(BridgeFightPlugin plugin, BedFightArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent e) {
        BedFightSetupSession session = arenaManager.getSetupSession(e.getPlayer());
        if (session == null) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.STONE_AXE || !item.hasItemMeta() || 
            !"§cBedFight Wand".equals(item.getItemMeta().getDisplayName())) return;

        if (e.getClickedBlock() == null) return;

        Arena arena = session.getArena();
        Location loc = e.getClickedBlock().getLocation();

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            arena.setPos1(loc);
            e.getPlayer().sendMessage("§aPos1 set.");
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            arena.setPos2(loc);
            e.getPlayer().sendMessage("§aPos2 set.");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBedPlace(BlockPlaceEvent e) {
        BedFightSetupSession session = arenaManager.getSetupSession(e.getPlayer());
        if (session == null) return;

        ItemStack item = e.getItemInHand();
        if (item == null || item.getType() != Material.BED_BLOCK) return;

        String displayName = item.getItemMeta().getDisplayName();
        Arena arena = session.getArena();
        Location loc = e.getBlock().getLocation();

        if ("§cRed Bed".equals(displayName)) {
            arena.setRedBed(loc);
            e.getPlayer().sendMessage("§aRed Bed set.");
        } else if ("§9Blue Bed".equals(displayName)) {
            arena.setBlueBed(loc);
            e.getPlayer().sendMessage("§aBlue Bed set.");
        } else {
            return;
        }
        
        arenaManager.saveArena(arena);
    }
}
