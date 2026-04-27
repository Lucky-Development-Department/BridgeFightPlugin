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

    // Constants from FireballListener
    private static final double EXPLOSION_RADIUS = 3.8;
    private static final double BASE_POWER = 1.35;
    private static final double LOOK_BIAS = 0.15;
    private static final double MOMENTUM_SCALE = 0.25;
    private static final double MAX_Y = 1.55;
    private static final double MAX_TOTAL = 2.4;
    private static final int CEILING_CHECK = 2;

    private static final double SAME_LEVEL_EPS = 0.35;
    private static final double LIFT_DISTANCE = 2.1;
    private static final double GUARANTEED_LIFT_MIN = 0.48;
    private static final double GUARANTEED_LIFT_MAX = 1.1;

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
            if (!(near instanceof Player)) continue;
            Player player = (Player) near;
            
            // Mark for void tracking
            fireballTracker.markPlayerHitByExplosion(player);
            
            // Damage credit
            if (shooter != null) {
                if (player.equals(shooter)) {
                    player.damage(damageSelf, shooter);
                } else {
                    player.damage(damageEnemy, shooter);
                }
            }

            Location playerLoc = player.getLocation();
            double distance = playerLoc.distance(explosionLoc);
            if (distance > EXPLOSION_RADIUS) continue;

            // Ceiling check
            boolean hasCeiling = false;
            for (int i = 1; i <= CEILING_CHECK; i++) {
                if (playerLoc.clone().add(0, i, 0).getBlock().getType().isSolid()) {
                    hasCeiling = true;
                    break;
                }
            }
            if (hasCeiling && explosionLoc.getY() >= playerLoc.getY()) continue;

            // --- Knockback Math (Minemen Style) ---
            double proximity = Math.pow(Math.max(0.0, 1.0 - (distance / EXPLOSION_RADIUS)), 2.5);
            double momentumFactor = 1.0 + (player.getVelocity().length() * MOMENTUM_SCALE);
            double force = BASE_POWER * (1.0 + 1.25 * proximity) * momentumFactor;

            Vector explosionDir = playerLoc.toVector().subtract(explosionLoc.toVector());
            if (explosionDir.lengthSquared() < 0.0001) explosionDir = playerLoc.getDirection().clone();
            explosionDir.normalize();

            Vector lookDir = playerLoc.getDirection().clone().normalize();
            Vector finalDir = explosionDir.clone().multiply(1.0 - LOOK_BIAS)
                    .add(lookDir.clone().multiply(LOOK_BIAS)).normalize();

            boolean explosionBelow = explosionLoc.getY() <= playerLoc.getY() + SAME_LEVEL_EPS;
            boolean grounded = player.isOnGround();

            // Vertical Lift
            double verticalBoost = 0.0;
            if (distance <= LIFT_DISTANCE && explosionBelow) {
                double liftScale = (1.0 - (distance / LIFT_DISTANCE)) * proximity;
                verticalBoost = GUARANTEED_LIFT_MIN + (GUARANTEED_LIFT_MAX - GUARANTEED_LIFT_MIN) * liftScale;
                if (grounded) verticalBoost *= 1.1;
                
                Vector up = new Vector(0, 1, 0);
                double blend = Math.max(0.2, Math.min(0.85, 0.3 + 0.7 * liftScale));
                finalDir = finalDir.clone().multiply(1.0 - blend).add(up.multiply(blend)).normalize();
            }

            Vector knock = finalDir.clone().multiply(force);
            knock.setY(knock.getY() + verticalBoost);

            // Head conversion (prevents being spiked into ground unless intended)
            double headY = playerLoc.getY() + player.getEyeHeight() * 0.7;
            if (explosionLoc.getY() > headY && knock.getY() < 0) {
                knock.setY(0);
                knock.normalize().multiply(force);
            }

            // Clamping
            if (knock.getY() > MAX_Y) knock.setY(MAX_Y);
            if (knock.length() > MAX_TOTAL) knock.multiply(MAX_TOTAL / knock.length());

            // Sprinting/Jumping modifiers
            double knockMultiplier = 1.0;
            if (player.isSprinting()) knockMultiplier *= 0.9;
            if (!player.isOnGround()) knockMultiplier *= 0.85;
            knock.multiply(knockMultiplier);

            // Apply Velocity
            if (grounded && verticalBoost > 0) {
                player.setVelocity(new Vector(0, 0.35, 0));
                Vector finalKnock = knock.clone();
                Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {
                    if (player.isOnline()) player.setVelocity(finalKnock);
                }, 1L);
            } else {
                player.setVelocity(knock);
            }
        }
    }
}
