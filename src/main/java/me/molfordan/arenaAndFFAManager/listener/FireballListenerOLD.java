package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.Material.FIREBALL;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class FireballListenerOLD implements Listener {

    private final ConfigManager config;

    private final double explosionSize;
    private final double speedMultiplier;
    private final boolean makeFire;

    private final double horizontal;
    private final double vertical;
    private final double maxY;

    private final long cooldown;
    private final double damageSelf;
    private final double damageEnemy;

    private final double fireballExplosionSize = 3.0;

    private final double fireballHorizontal = 2.0;
    private final double fireballVertical = 3.0;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public FireballListenerOLD(ArenaAndFFAManager plugin, ConfigManager config) {
        this.config = config;

        this.explosionSize = config.getFireballExplosionSize();
        this.speedMultiplier = config.getFireballSpeedMultiplier();
        this.makeFire = config.isFireballMakeFire();

        this.horizontal = config.getFireballHorizontalKnockback() * -1;
        this.vertical = config.getFireballVerticalKnockback();
        this.maxY = config.getFireballMaxY();

        this.cooldown = (long) (config.getFireballCooldown() * 1000);
        this.damageSelf = config.getFireballDamageSelf();
        this.damageEnemy = config.getFireballDamageEnemy();
    }

    // ================= USE =================

    @EventHandler
    public void onFireballUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().getName().equals(config.getBuildFFAWorldName())) return;
        if (event.getAction() != RIGHT_CLICK_AIR && event.getAction() != RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != FIREBALL) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < cooldown) {
            event.setCancelled(true);
            return;
        }

        cooldowns.put(player.getUniqueId(), now);
        event.setCancelled(true);

        Location eye = player.getEyeLocation().clone();
        Vector direction = eye.getDirection().normalize();

        Fireball fireball = player.getWorld().spawn(
                eye.add(direction.clone().multiply(0.6)),
                Fireball.class
        );

        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setVelocity(direction.clone().multiply(speedMultiplier));
        fireball.setIsIncendiary(false);
        fireball.setYield(0F);

        // velocity lock (1.8 drift fix)
        Vector lockedDir = direction.clone();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!fireball.isValid() || fireball.isDead()) {
                    cancel();
                    return;
                }
                fireball.setVelocity(lockedDir.clone().multiply(speedMultiplier));
            }
        }.runTaskTimer(ArenaAndFFAManager.getPlugin(), 1L, 1L);

        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            player.getInventory().setItemInHand(null);
        }
    }

    // ================= FIREBALL KNOCKBACK =================

    @EventHandler
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;

        Fireball fireball = (Fireball) event.getEntity();
        ProjectileSource source = fireball.getShooter();

        if (!(source instanceof Player)) return;
        Player shooter = (Player) source;

        if (!shooter.getWorld().getName().equals(config.getBuildFFAWorldName())) return;

        applyExplosion(fireball.getLocation(), shooter);
    }

    // ================= PEARL → PLAYER HITBOX =================

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
            Vector kb = facing.multiply(Math.abs(horizontal) * 1.2);

            player.setVelocity(kb);

            loc.getWorld().createExplosion(
                    loc.getX(), loc.getY(), loc.getZ(),
                    (float) explosionSize,
                    false,
                    false
            );
            break;
        }
    }

    // ================= SHARED EXPLOSION =================
    /*
    private void applyExplosion(Location loc, Player shooter) {
        World world = loc.getWorld();
        if (world == null) return;

        Vector center = loc.toVector();

        for (Entity entity : world.getNearbyEntities(loc, explosionSize, explosionSize, explosionSize)) {
            if (!(entity instanceof Player)) continue;

            Player victim = (Player) entity;

            // from explosion → player
            Vector diff = victim.getLocation().toVector().subtract(center);

            // horizontal only
            Vector horizontalDir = diff.clone().setY(0);
            if (horizontalDir.lengthSquared() > 0) {
                horizontalDir.normalize();
            }

            Vector kb = horizontalDir.multiply(horizontal);

            // always-up FB jump (classic)
            double y = Math.min(vertical * 1.5, maxY);
            kb.setY(y);

            victim.setVelocity(kb);

            if (victim.equals(shooter)) {
                if (damageSelf >= 0) victim.damage(damageSelf);
            } else {
                if (damageEnemy >= 0) victim.damage(damageEnemy, shooter);
            }
        }
    }

     */

    private void applyExplosion(Location explosionLoc, Player shooter) {
        World world = explosionLoc.getWorld();
        if (world == null) return;

        Vector explosionVec = explosionLoc.toVector();

        for (Entity entity : world.getNearbyEntities(
                explosionLoc,
                fireballExplosionSize,
                fireballExplosionSize,
                fireballExplosionSize)) {

            if (!(entity instanceof Player)) continue;
            Player victim = (Player) entity;

            Location victimLoc = victim.getLocation();
            Vector victimVec = victimLoc.toVector();

            double distance = victimLoc.distance(explosionLoc);
            if (distance > fireballExplosionSize) continue;

            // -------------------------------
            // 1. Distance scaling (Hypixel-like)
            // -------------------------------
            double proximity = Math.max(0.0,
                    1.0 - (distance / fireballExplosionSize));
            proximity = Math.pow(proximity, 1.8); // strong near, soft far

            double power = proximity;

            // -------------------------------
            // 2. Direction: explosion -> player
            // -------------------------------
            Vector dir = victimVec.clone().subtract(explosionVec);
            if (dir.lengthSquared() < 0.0001) continue;
            dir.normalize();

            // -------------------------------
            // 3. Horizontal knockback (stable)
            // -------------------------------
            Vector horizontal = dir.clone().setY(0);
            if (horizontal.lengthSquared() > 0) {
                horizontal.normalize().multiply(fireballHorizontal * power);
            }

            // -------------------------------
            // 4. Vertical shaping (Hypixel feel)
            // -------------------------------
            double vertical = fireballVertical * power;

            boolean explosionBelow =
                    explosionLoc.getY() <= victimLoc.getY() + 0.2;

            if (explosionBelow) {
                // Fireball jump
                vertical += 0.35 * proximity;
            } else {
                // Explosion above → reduce lift
                vertical *= 0.6;
            }

            // Shooter pitch influence (Hypixel)
            if (shooter != null) {
                float pitch = shooter.getLocation().getPitch();
                if (pitch <= -45f) { // shooter aiming up
                    double upness = Math.min(1.0, (-pitch - 45f) / 45f);
                    vertical -= 0.4 * upness * proximity;
                }
            }

            // Clamp vertical
            vertical = Math.max(-1.2, Math.min(1.55, vertical));

            // -------------------------------
            // 5. Final velocity
            // -------------------------------
            Vector velocity = horizontal.clone().setY(vertical);

            // Clamp total velocity
            double maxTotal = 2.4;
            if (velocity.length() > maxTotal) {
                velocity.multiply(maxTotal / velocity.length());
            }

            victim.setVelocity(velocity);

            // -------------------------------
            // 6. Damage handling (your logic)
            // -------------------------------
            if (victim.equals(shooter)) {
                if (damageSelf > 0) victim.damage(damageSelf);
            } else {
                if (damageEnemy > 0) victim.damage(damageEnemy, shooter);
            }
        }
    }





    // ================= SAFETY =================

    @EventHandler
    public void onFireballDirectHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fireball && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireballPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Fireball) {
            event.setFire(makeFire);
            event.setRadius((float) explosionSize);
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
