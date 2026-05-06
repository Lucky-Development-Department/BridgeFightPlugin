package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.FireballTracker;
import me.molfordan.arenaAndFFAManager.manager.TNTTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

public class TNTListener implements Listener {

    private final TNTTracker tntTracker;
    private final FireballTracker fireballTracker;

    private static final double EXPLOSION_RADIUS = 8.0; // Vanilla TNT radius (size 4.0 * 2)

    private final double damageSelf = 0.5;
    private final double damageEnemy = 1.5;

    public TNTListener(FireballTracker fireballTracker, TNTTracker tntTracker) {
        this.fireballTracker = fireballTracker;
        this.tntTracker = tntTracker;
    }

    @EventHandler
    public void onTNTPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.TNT) {
            e.setCancelled(true);
            
            Player player = e.getPlayer();
            ItemStack item = player.getItemInHand();
            
            // Remove item from hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.setItemInHand(null);
            }

            Location loc = e.getBlock().getLocation().add(0.5, 0, 0.5);
            TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);
            tnt.setFuseTicks(40); // 2 seconds
            
            tntTracker.trackTNT(tnt, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTNTExplode(EntityExplodeEvent e) {
        if (e.getEntityType() == EntityType.PRIMED_TNT) {
            e.setCancelled(true);
            
            Location explosionLoc = e.getLocation();
            Player shooter = tntTracker.getTNTOwner((TNTPrimed) e.getEntity());
            tntTracker.removeTNT(e.getEntity().getUniqueId());

            // Visuals only explosion effects (no block damage, no vanilla knockback, no vanilla damage)
            explosionLoc.getWorld().playEffect(explosionLoc, org.bukkit.Effect.EXPLOSION_HUGE, 0);
            explosionLoc.getWorld().playSound(explosionLoc, org.bukkit.Sound.EXPLODE, 1.0F, 1.0F);

            applyKnockback(explosionLoc, shooter);

            e.getEntity().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTNTDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof TNTPrimed || e.getDamager().getType() == EntityType.PRIMED_TNT) {
            e.setCancelled(true);
        }
    }

    private void applyKnockback(Location explosionLoc, Player shooter) {
        World world = explosionLoc.getWorld();

        for (Entity near : world.getNearbyEntities(explosionLoc, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            // Player-specific logic (Tracking and Custom Damage)
            if (near instanceof Player) {
                Player player = (Player) near;
                fireballTracker.markPlayerHitByExplosion(player);
                
                if (shooter != null) {
                    if (player.equals(shooter)) {
                        player.damage(damageSelf, shooter);
                    } else {
                        player.damage(damageEnemy, shooter);
                    }
                }
            }

            Location entityLoc = near.getLocation();
            double distance = entityLoc.distance(explosionLoc);
            if (distance > EXPLOSION_RADIUS) continue;

            // --- Vanilla Knockback Formula ---
            double x = entityLoc.getX() - explosionLoc.getX();
            
            // For vertical lift calculation, vanilla uses eye level for the direction vector
            double eyeHeight = 0.0;
            if (near instanceof LivingEntity) {
                eyeHeight = ((LivingEntity) near).getEyeHeight();
            }
            
            double y = (entityLoc.getY() + eyeHeight) - explosionLoc.getY();
            double z = entityLoc.getZ() - explosionLoc.getZ();
            double d = Math.sqrt(x * x + y * y + z * z);

            if (d != 0.0) {
                x /= d;
                y /= d;
                z /= d;

                // Exposure (Simplified to 1.0 for Bukkit environment)
                double exposure = 1.0;
                double impact = (1.0 - (distance / EXPLOSION_RADIUS)) * exposure;

                // Vanilla knockback magnitude is 'impact'. We apply a base 2x multiplier.
                // Then we boost horizontal and reduce vertical as requested.
                double baseMultiplier = impact * 2.0;
                double horizontalBoost = 1.5;
                double verticalReduction = 0.5;

                Vector knockback = new Vector(
                        x * baseMultiplier * horizontalBoost,
                        y * baseMultiplier * verticalReduction,
                        z * baseMultiplier * horizontalBoost
                );

                // Add to current velocity (Vanilla behavior)
                near.setVelocity(near.getVelocity().add(knockback));
            }
        }
    }
}

