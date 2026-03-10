package me.molfordan.arenaAndFFAManager.region;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class RegionFlagListener implements Listener {

    private final CommandRegionManager manager;

    public RegionFlagListener(CommandRegionManager manager) {
        this.manager = manager;
    }

    private boolean isDenied(Player p, FlagType type) {
        return manager.isDenied(p, type);
    }

    private boolean isDenied(Location loc, FlagType type) {
        return manager.isDenied(loc, type);
    }

    private CommandRegion getRegion(Location loc) {
        for (CommandRegion r : manager.getRegions()) {
            if (r.isInside(loc)) return r;
        }
        return null;
    }

    // -------------------------
    // BUILD (break)
    // -------------------------
    /*
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (isDenied(e.getPlayer(), FlagType.BUILD)) {
            e.setCancelled(true);
        }
    }

    // -------------------------
    // BUILD (place)
    // -------------------------
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (isDenied(e.getPlayer(), FlagType.BUILD)) {
            e.setCancelled(true);
        }
    }

     */

    // -------------------------
    // PVP
    // -------------------------
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {

        if (!(e.getEntity() instanceof Player)) return;

        Player victim = (Player) e.getEntity();
        Player attacker = null;

        if (e.getDamager() instanceof Player)
            attacker = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile
                && ((Projectile) e.getDamager()).getShooter() instanceof Player)
            attacker = (Player) ((Projectile) e.getDamager()).getShooter();

        if (attacker == null) return;

        if (isDenied(victim, FlagType.PVP) || isDenied(attacker, FlagType.PVP)) {
            e.setCancelled(true);
        }
    }

    // -------------------------
    // ITEM PICKUP / DROP
    // -------------------------
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isDenied(e.getPlayer(), FlagType.ITEM_DROP)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        if (isDenied(e.getPlayer(), FlagType.ITEM_PICKUP)) {
            e.setCancelled(true);
        }
    }

    // -------------------------
    // ENTRY / EXIT (block movement)
    // -------------------------
    @EventHandler
    public void onMove(PlayerMoveEvent e) {

        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()) return;

        Player p = e.getPlayer();

        boolean wasInside = false;
        boolean isInsideNow = false;

        CommandRegion fromR = getRegion(e.getFrom());
        CommandRegion toR = getRegion(e.getTo());

        if (fromR != null) wasInside = true;
        if (toR != null) isInsideNow = true;

        // block entering
        if (!wasInside && isInsideNow) {
            if (isDenied(p, FlagType.ENTRY)) {
                e.setTo(e.getFrom());
                return;
            }

            String msg = toR.getFlag(FlagType.ENTRY_MESSAGE);
            if (msg != null && !msg.isEmpty()) {
                p.sendMessage(msg.replace("&", "§"));
            }
        }

        // block leaving
        if (wasInside && !isInsideNow) {
            if (isDenied(p, FlagType.EXIT)) {
                e.setTo(e.getFrom());
                return;
            }

            if (fromR != null) {
                String msg = fromR.getFlag(FlagType.EXIT_MESSAGE);
                if (msg != null && !msg.isEmpty()) {
                    p.sendMessage(msg.replace("&", "§"));
                }
            }
        }
    }

    // -------------------------
    // MOB SPAWNING
    // -------------------------
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (isDenied(e.getLocation(), FlagType.MOB_SPAWNING)) {
            e.setCancelled(true);
        }
    }

    // -------------------------
    // EXPLOSIONS
    // -------------------------
    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (isDenied(e.getLocation(), FlagType.TNT) ||
                isDenied(e.getLocation(), FlagType.CREEPER_EXPLOSION)) {
            e.blockList().clear();
        }
    }
}
