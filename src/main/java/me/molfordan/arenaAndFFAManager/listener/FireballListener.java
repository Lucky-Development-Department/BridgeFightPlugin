package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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
    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;
    private final double fireballHorizontal;
    private final double fireballVertical;
    private final ConfigManager config;

    private final double damageSelf;
    private final double damageEnemy;
    private final double damageTeammates;

    private static final int SLOWNESS_DURATION = 20;
    private static final int SLOWNESS_LEVEL = 1;
    private static final double EXPLOSION_RADIUS = 3.785;
    private static final double BASE_POWER = 1.3;
    private static final double LOOK_BIAS = 0.14;
    private static final double MOMENTUM_SCALE = 0.22;
    private static final double MAX_Y = 1.55;
    private static final double MAX_TOTAL = 2.4;
    private static final int CEILING_CHECK = 2;

    private static final double SAME_LEVEL_EPS = 0.30;
    private static final double LIFT_DISTANCE = 2.0;
    private static final double GUARANTEED_LIFT_MIN = 0.45;
    private static final double GUARANTEED_LIFT_MAX = 1.05;

    public FireballListener(ConfigManager config) {
        this.config = config;
        this.fireballExplosionSize = 3.5;
        this.fireballMakeFire = false;
        this.fireballHorizontal = 1.3;
        this.fireballVertical = 1.3;
        this.damageSelf = 0.5;
        this.damageEnemy = 2.0;
        this.damageTeammates = 0.0;
    }

    @EventHandler
    public void fireballHit(ProjectileHitEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        Location location = e.getEntity().getLocation();

        ProjectileSource projectileSource = e.getEntity().getShooter();
        if(!(projectileSource instanceof Player)) return;
        Player source = (Player) projectileSource;



        Vector vector = location.toVector();

        World world = location.getWorld();

        assert world != null;
        Collection<Entity> nearbyEntities = world
                .getNearbyEntities(location, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);
        for(Entity entity : nearbyEntities) {
            if(!(entity instanceof Player)) continue;
            Player player = (Player) entity;
            //if(!getAPI().getArenaUtil().isPlaying(player)) continue;


            Vector playerVector = player.getLocation().toVector();
            Vector normalizedVector = vector.subtract(playerVector).normalize();
            Vector horizontalVector = normalizedVector.multiply(fireballHorizontal);
            double y = normalizedVector.getY();
            if(y < 0 ) y += 1.5;
            if(y <= 0.5) {
                y = fireballVertical*1.5; // kb for not jumping
            } else {
                y = y*fireballVertical*1.5; // kb for jumping
            }
            player.setVelocity(horizontalVector.setY(y));

            /*

            LastHit lh = LastHit.getLastHit(player);
            if (lh != null) {
                lh.setDamager(source);
                lh.setTime(System.currentTimeMillis());
            } else {
                new LastHit(player, source, System.currentTimeMillis());
            }

             */

            if(player.equals(source)) {
                if(damageSelf > 0) {
                    player.damage(damageSelf); // damage shooter
                }

            } else {
                if(damageEnemy > 0) {
                    player.damage(damageEnemy); // damage enemies
                }
            }
        }
    }


    @EventHandler
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Fireball)) return;
        if(!(e.getEntity() instanceof Player)) return;

        //if(Arena.getArenaByPlayer((Player) e.getEntity()) == null) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void fireballPrime(ExplosionPrimeEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        ProjectileSource shooter = ((Fireball)e.getEntity()).getShooter();
        if(!(shooter instanceof Player)) return;
        Player player = (Player) shooter;

        //if(Arena.getArenaByPlayer(player) == null) return;

        e.setFire(fireballMakeFire);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;
        Fireball fireball = (Fireball) event.getEntity();

        // Shooter (may be null / non-player)
        Player shooter = null;
        if (fireball.getShooter() instanceof Player) {
            shooter = (Player) fireball.getShooter();
        }

        Location explosionLoc = fireball.getLocation().clone();
        World world = explosionLoc.getWorld();

        // Slightly offset forward for wall hits
        Vector flight = fireball.getVelocity().clone();
        if (flight.lengthSquared() > 0.0001) {
            flight.normalize();
            explosionLoc.add(flight.clone().multiply(0.6));
        }

        for (Entity near : fireball.getNearbyEntities(EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (!(near instanceof Player)) continue;
            Player player = (Player) near;
            Location playerLoc = player.getLocation();

            double distance = playerLoc.distance(explosionLoc);
            if (distance > EXPLOSION_RADIUS) continue;

            // --- Ceiling check ---
            boolean hasCeiling = false;
            for (int i = 1; i <= CEILING_CHECK; i++) {
                if (playerLoc.clone().add(0, i, 0).getBlock().getType().isSolid()) {
                    hasCeiling = true;
                    break;
                }
            }
            if (hasCeiling && explosionLoc.getY() >= playerLoc.getY()) continue;

            // --- Distance scaling ---
            double distanceScale = Math.max(0.0, 1.0 - (0.10 * distance));
            distanceScale = Math.max(0.5, distanceScale);
            double proximity = Math.pow(Math.max(0.0, 1.0 - (distance / EXPLOSION_RADIUS)), 2.8);
            double momentumFactor = 1.0 + (player.getVelocity().length() * MOMENTUM_SCALE);

            double force = BASE_POWER * (1.0 + 1.2 * proximity) * momentumFactor * distanceScale;

            // Direction vectors
            Vector explosionDir = playerLoc.toVector().subtract(explosionLoc.toVector());
            if (explosionDir.lengthSquared() < 0.0001)
                explosionDir = playerLoc.getDirection().clone();
            explosionDir.normalize();

            Vector lookDir = playerLoc.getDirection().clone().normalize();
            Vector finalDir = explosionDir.clone().multiply(1.0 - LOOK_BIAS)
                    .add(lookDir.clone().multiply(LOOK_BIAS)).normalize();

            // --- Pitch & lift ---
            double pitchDeg = playerLoc.getPitch();
            double pitchFactor = Math.sin(Math.toRadians(pitchDeg));
            double yDiff = playerLoc.getY() - explosionLoc.getY();
            boolean explosionBelowOrSame = explosionLoc.getY() <= playerLoc.getY() + SAME_LEVEL_EPS;
            boolean grounded = player.isOnGround();

            // Shooter upward factor: 0 when shooter not steep-up, 0..1 when between -45..-90
            double shooterUpness = 0.0;
            if (shooter != null) {
                double shPitch = shooter.getLocation().getPitch(); // -90 (up) .. 90 (down)
                if (shPitch <= -45.0) {
                    // map [-45 .. -90] -> [0 .. 1]
                    shooterUpness = Math.min(1.0, Math.max(0.0, (-shPitch - 45.0) / 45.0));
                }
            }

            // Guaranteed lift if explosion below
            boolean inLiftRange = distance <= LIFT_DISTANCE && explosionBelowOrSame;
            double guaranteedLift = 0.0;
            if (inLiftRange) {
                double closeFactor = 1.0 - (distance / LIFT_DISTANCE);
                closeFactor = Math.max(0.0, Math.min(1.0, closeFactor));
                double liftScale = closeFactor * proximity;
                guaranteedLift = GUARANTEED_LIFT_MIN + (GUARANTEED_LIFT_MAX - GUARANTEED_LIFT_MIN) * liftScale;
                if (grounded) guaranteedLift *= 1.05;
                Vector up = new Vector(0, 1, 0);
                double blend = Math.max(0.15, Math.min(0.9, 0.25 + 0.75 * liftScale));
                finalDir = finalDir.clone().multiply(1.0 - blend).add(up.multiply(blend)).normalize();
            }

            // --- Flat (wall) push ---
            boolean flatAim = Math.abs(pitchDeg) <= 15.0;
            boolean sameHeight = Math.abs(explosionLoc.getY() - playerLoc.getY()) < 0.75;
            boolean nearWall = distance <= 2.5;
            if (flatAim && sameHeight && nearWall && !inLiftRange) {
                Vector hor = explosionDir.clone().setY(0);
                if (hor.lengthSquared() < 0.0001) hor = lookDir.clone().setY(0);
                if (hor.lengthSquared() < 0.0001) continue;
                hor.normalize();
                double horizontalDelta = Math.min(MAX_TOTAL, force * 0.9 + 0.5 * proximity);
                Vector finalVel = hor.multiply(horizontalDelta);
                finalVel.setY(0);
                player.setVelocity(finalVel);
                continue;
            }

            // --- Knockback vector ---
            Vector knock = finalDir.clone().multiply(force);

            // --- Vertical shaping ---
            double verticalBoost = 0.0;
            if (explosionBelowOrSame && yDiff <= 1.5 && distance < 1.8) {
                verticalBoost += 0.6 * (1.0 - (distance / 1.8));
            } else if (explosionLoc.getY() < playerLoc.getY() - 0.4) {
                verticalBoost += 0.45 * proximity;
            } else if (explosionLoc.getY() > playerLoc.getY() + 1.0) {
                // normally add a small upward on high explosions, but reduce/flip if shooter shot up
                verticalBoost += 0.28 * proximity * (1.0 - shooterUpness);
            }

            verticalBoost += 0.38 * pitchFactor * proximity;
            verticalBoost += guaranteedLift;

            // If shooter shot steeply upward, add a downward component proportional to upness
            if (shooterUpness > 0.0 && explosionLoc.getY() > playerLoc.getY() - 0.2) {
                // compute downward strength: scale with shooterUpness & proximity & (1 - distance/EXPLOSION_RADIUS)
                double downStrength = 0.55 * shooterUpness * proximity * (1.0 - (distance / EXPLOSION_RADIUS));
                verticalBoost -= downStrength; // negative = downward
            }

            if (hasCeiling) verticalBoost = 0.0;
            knock.setY(knock.getY() + verticalBoost);

            // --- HEAD-level horizontal conversion ---
            double headY = playerLoc.getY() + player.getEyeHeight() * 0.6;

            // Only convert downward -> horizontal if shooter did NOT shoot steeply up.
            boolean shooterShotUp = shooterUpness > 0.0;
            if (explosionLoc.getY() > headY && knock.getY() < 0 && !shooterShotUp) {
                // convert downward to horizontal burst (existing behavior)
                knock.setY(0);
                Vector horizontal = knock.clone().setY(0);
                if (horizontal.lengthSquared() > 0) {
                    horizontal.normalize().multiply(knock.length());
                    knock = horizontal;
                }
            }
            // If shooterShotUp == true, we allow the negative Y to remain (downward knock)

            // --- Clamping ---
            if (knock.getY() > MAX_Y) knock.setY(MAX_Y);
            if (knock.length() > MAX_TOTAL) knock.multiply(MAX_TOTAL / knock.length());

            // --- Movement state multipliers ---
            boolean onGround = player.isOnGround();
            Vector vel = player.getVelocity();
            double horizontalSpeed = vel.clone().setY(0).length();

            boolean moving = horizontalSpeed > 0.08;
            boolean sprinting = player.isSprinting();
            boolean jumping = !onGround && vel.getY() > 0.05;
            boolean walking = moving && !sprinting;

            double knockMultiplier = 1.0;
            if (sprinting && !jumping) knockMultiplier = 0.9;
            else if (sprinting && jumping) knockMultiplier = 0.8;
            else if (walking) knockMultiplier = 0.75;

            knock.multiply(knockMultiplier);

            // --- Grounded pre-hop ---
            if (grounded && inLiftRange) {
                player.setVelocity(new Vector(0, 0.32, 0));
                Vector finalKnock = knock.clone();
                Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> player.setVelocity(finalKnock), 1L);
            } else {
                player.setVelocity(knock);
            }
        }

        world.createExplosion(explosionLoc.getX(), explosionLoc.getY(), explosionLoc.getZ(), 0F, false, false);
        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), fireball::remove, 1L);
    }

    @EventHandler
    public void onPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();
        Location to = event.getTo();

        if (!player.getWorld().getName().equals(config.getBuildFFAWorldName())) return;

        for (Entity entity : player.getWorld().getNearbyEntities(to, 1.5, 1.5, 1.5)) {
            if (!(entity instanceof Fireball)) continue;

            Fireball fireball = (Fireball) entity;
            Location loc = fireball.getLocation();
            fireball.remove();

            // horizontal launch only
            Vector facing = player.getLocation().getDirection().setY(0).normalize();

// use horizontal strength from config
            Vector kb = facing.multiply(Math.abs(fireballHorizontal) * 1.2);

            player.setVelocity(kb);

            loc.getWorld().createExplosion(
                    loc.getX(), loc.getY(), loc.getZ(),
                    (float) fireballExplosionSize,
                    false,
                    false
            );
            break;
        }
    }

    // Add this map at the top of your class to store cooldowns
// Place this at the top of your class
    private final Map<UUID, Long> fireballCooldowns = new HashMap<>();
    private final double fireballSpeedMultiplier = 1.0; // Adjust as needed
    private final float fireballExplosionSizes = 2.0f; // Adjust as needed
    private final double fireballCooldownSeconds = 0.5; // Adjust as needed

    @EventHandler
    public void onFireballInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK &&
                e.getAction() != Action.RIGHT_CLICK_AIR) return;

        ItemStack inHand = e.getItem();
        if (inHand == null || inHand.getType() != Material.FIREBALL) return;

        final Player p = e.getPlayer();
        e.setCancelled(true);

        UUID uuid = p.getUniqueId();
        long now = System.currentTimeMillis();
        long last = fireballCooldowns.containsKey(uuid)
                ? fireballCooldowns.get(uuid)
                : 0L;

        if (now - last < fireballCooldownSeconds * 1000L) return;
        fireballCooldowns.put(uuid, now);

        // Launch fireball
        final Fireball fb = p.launchProjectile(Fireball.class);
        fb.setShooter(p);

        // === CONTROLLED SPEED (1.8.8 SAFE) ===
        final Vector direction = p.getEyeLocation().getDirection().normalize();
        final double speed = 1.0; // THIS NOW ACTUALLY CONTROLS SPEED

        fb.setVelocity(direction.clone().multiply(speed));

        // Lock speed every tick to cancel vanilla acceleration
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (fb.isDead() || !fb.isValid()) {
                    cancel();
                    return;
                }
                fb.setVelocity(direction.clone().multiply(speed));
            }
        }.runTaskTimer(ArenaAndFFAManager.getPlugin(), 1L, 1L);

        fb.setIsIncendiary(true);
        fb.setYield(fireballExplosionSizes);

        // Consume item (1.8.8)
        if (inHand.getAmount() > 1) {
            inHand.setAmount(inHand.getAmount() - 1);
        } else {
            p.getInventory().setItemInHand(null);
        }
    }

    @EventHandler
    public void onFireballPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Fireball) {
            event.setFire(fireballMakeFire);
            event.setRadius((float) fireballExplosionSize);
        }
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball &&
                event.getEntity().getWorld().getName().equals(config.getBuildFFAWorldName())) {
            event.blockList().clear();
        }
    }
}
