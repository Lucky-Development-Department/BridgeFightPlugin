package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.manager.FireballTracker;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.utils.FireballUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FireballListener implements Listener {
    private final ConfigManager config;
    private final FireballTracker fireballTracker;

    private final double damageSelf = 0.5;
    private final double damageEnemy = 2.0;

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

    private final Map<UUID, Long> fireballCooldowns = new HashMap<>();
    private final float fireballExplosionSizes = 2.3f;
    private final double fireballCooldownSeconds = 0.5;

    public FireballListener(ConfigManager config, FireballTracker fireballTracker) {
        this.config = config;
        this.fireballTracker = fireballTracker;
    }

    @EventHandler
    public void onFireballLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof Fireball) {
            Fireball fb = (Fireball) e.getEntity();
            if (fb.getShooter() instanceof Player) {
                fireballTracker.trackFireball(fb, (Player) fb.getShooter());
            }
        }
    }

    @EventHandler
    public void onFireballInteract(PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        long now = System.currentTimeMillis();

        // --- Left Click: Toss/Redirect Fireball ---
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            Fireball nearest = null;
            double nearestDist = 3.5; // Slightly reduced reach

            Vector playerLook = p.getEyeLocation().getDirection().normalize();

            for (Entity entity : p.getNearbyEntities(nearestDist, nearestDist, nearestDist)) {
                if (entity instanceof Fireball) {
                    Location fbLoc = entity.getLocation();
                    Vector toFireball = fbLoc.toVector().subtract(p.getEyeLocation().toVector()).normalize();
                    
                    // Sensitivity check: Player must be looking roughly towards the fireball (Dot product > 0.45)
                    if (playerLook.dot(toFireball) > 0.45) {
                        double dist = fbLoc.distance(p.getEyeLocation());
                        if (dist < nearestDist) {
                            nearest = (Fireball) entity;
                            nearestDist = dist;
                        }
                    }
                }
            }

            if (nearest != null) {
                // Toss straight where the player is aiming
                Vector targetDir = p.getEyeLocation().getDirection().normalize();

                // Update ownership for damage credit
                nearest.setShooter(p);
                fireballTracker.trackFireball(nearest, p);
                
                // Set both direction and velocity for perfectly straight flight
                // Fireballs in Bukkit use 'direction' (acceleration) to maintain their path
                FireballUtil.setDirection(nearest, p.getEyeLocation().getDirection());
                nearest.setVelocity(targetDir.multiply(0.9));
            }
            return;
        }

        // --- Right Click: Shoot Fireball ---
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK &&
                e.getAction() != Action.RIGHT_CLICK_AIR) return;

        ItemStack inHand = e.getItem();
        if (inHand == null || inHand.getType() != Material.FIREBALL) return;

        e.setCancelled(true);

        long last = fireballCooldowns.getOrDefault(uuid, 0L);
        if (now - last < fireballCooldownSeconds * 1000L) return;
        fireballCooldowns.put(uuid, now);

        // Launch fireball
        final Fireball fb = p.launchProjectile(Fireball.class);
        fb.setShooter(p);
        
        fireballTracker.trackFireball(fb, p);

        final Vector direction = p.getEyeLocation().getDirection().normalize();
        final double speed = 0.98;
        FireballUtil.setDirection(fb, p.getEyeLocation().getDirection());

        fb.setVelocity(direction.clone().multiply(speed));

        // Use a wrapper to allow direction updates when punched/redirected
        final Vector[] activeDir = new Vector[]{ direction.clone() };

        // Lock speed every tick
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (fb.isDead() || !fb.isValid()) {
                    cancel();
                    return;
                }
                
                // Check if someone punched it or redirected it
                Vector currentVel = fb.getVelocity();
                if (currentVel.lengthSquared() > 0.1) {
                    Vector currentDir = currentVel.clone().normalize();
                    if (currentDir.distanceSquared(activeDir[0]) > 0.001) {
                        activeDir[0] = currentDir;
                    }
                }

                fb.setVelocity(activeDir[0].clone().multiply(speed));
            }
        }.runTaskTimer(ArenaAndFFAManager.getPlugin(), 1L, 1L);

        fb.setIsIncendiary(true);
        fb.setYield(fireballExplosionSizes);

        if (inHand.getAmount() > 1) {
            inHand.setAmount(inHand.getAmount() - 1);
        } else {
            p.getInventory().setItemInHand(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFireballPunch(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        if (!(e.getDamager() instanceof Player)) return;

        Fireball fb = (Fireball) e.getEntity();
        Player puncher = (Player) e.getDamager();

        // Update ownership when punched
        fb.setShooter(puncher);
        fireballTracker.trackFireball(fb, puncher);
        FireballUtil.setDirection(fb, puncher.getEyeLocation().getDirection());
        
        // Let vanilla handling or the runnable handle the velocity update
        // We just ensure the damage event isn't cancelled for the fireball entity
        e.setCancelled(false);
        e.setDamage(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;
        Fireball fireball = (Fireball) event.getEntity();

        Player shooter = null;
        if (fireball.getShooter() instanceof Player) {
            shooter = (Player) fireball.getShooter();
        }

        Location explosionLoc = fireball.getLocation().clone();
        World world = explosionLoc.getWorld();

        // Offset forward slightly for better wall logic
        Vector flight = fireball.getVelocity().clone();
        if (flight.lengthSquared() > 0.0001) {
            flight.normalize();
            explosionLoc.add(flight.multiply(0.6));
        }

        for (Entity near : world.getNearbyEntities(explosionLoc, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (!(near instanceof Player)) continue;
            Player player = (Player) near;
            
            // Mark for void tracking
            fireballTracker.markPlayerHitByFireball(player, fireball);
            
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

        // Visuals only explosion (no block damage)
        world.createExplosion(explosionLoc.getX(), explosionLoc.getY(), explosionLoc.getZ(), 0F, false, true);
        
        fireball.remove();
    }

    @EventHandler
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Fireball)) return;
        if(!(e.getEntity() instanceof Player)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void fireballPrime(ExplosionPrimeEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        e.setFire(false);
    }
}
