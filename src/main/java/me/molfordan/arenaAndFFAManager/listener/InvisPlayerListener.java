package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.*;

public class InvisPlayerListener implements Listener {

    private final Set<UUID> invisiblePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> revealTasks = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    private final ArenaAndFFAManager plugin;

    private final boolean onlyOnGround = false;
    private final boolean disableWhileSneaking = true;

    public InvisPlayerListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                tickFootsteps();
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void sendArmorState(Player target, Player viewer, boolean hide) {
        PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;

        ItemStack[] armor = target.getInventory().getArmorContents();

        ItemStack helmet = hide ? new ItemStack(Material.AIR) : armor[3];
        ItemStack chest  = hide ? new ItemStack(Material.AIR) : armor[2];
        ItemStack legs   = hide ? new ItemStack(Material.AIR) : armor[1];
        ItemStack boots  = hide ? new ItemStack(Material.AIR) : armor[0];

        conn.sendPacket(new PacketPlayOutEntityEquipment(target.getEntityId(), 4, CraftItemStack.asNMSCopy(helmet)));
        conn.sendPacket(new PacketPlayOutEntityEquipment(target.getEntityId(), 3, CraftItemStack.asNMSCopy(chest)));
        conn.sendPacket(new PacketPlayOutEntityEquipment(target.getEntityId(), 2, CraftItemStack.asNMSCopy(legs)));
        conn.sendPacket(new PacketPlayOutEntityEquipment(target.getEntityId(), 1, CraftItemStack.asNMSCopy(boots)));
    }

    private void updateArmorForEveryone(Player target, boolean hide) {
        for (Player viewer : target.getWorld().getPlayers()) {
            if (!viewer.equals(target)) {
                sendArmorState(target, viewer, hide);
            }
        }
    }

    public void hideArmorFor(Player p) {
        invisiblePlayers.add(p.getUniqueId());
        updateArmorForEveryone(p, true);
    }

    public void revealArmorFor(Player p) {
        invisiblePlayers.remove(p.getUniqueId());
        lastLocations.remove(p.getUniqueId());
        updateArmorForEveryone(p, false);

        BukkitTask t = revealTasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    @EventHandler
    public void onDrinkPotion(final PlayerItemConsumeEvent event) {
        final Player p = event.getPlayer();
        final ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getType() != Material.POTION) return;

        try {
            Potion potion = Potion.fromItemStack(item);
            if (potion.getType() != PotionType.INVISIBILITY) return;
        } catch (Throwable ignored) {
            return;
        }

        hideArmorFor(p);

        BukkitTask prev = revealTasks.remove(p.getUniqueId());
        if (prev != null) prev.cancel();

        new BukkitRunnable() {
            @Override
            public void run() {
                int duration = 0;

                Collection<PotionEffect> effects = p.getActivePotionEffects();
                for (PotionEffect eff : effects) {
                    if (eff.getType().equals(PotionEffectType.INVISIBILITY)) {
                        duration = eff.getDuration();
                        break;
                    }
                }

                if (duration <= 0) duration = 20 * 3;

                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        revealArmorFor(p);
                    }
                }.runTaskLater(plugin, duration);

                revealTasks.put(p.getUniqueId(), task);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player viewer = e.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uid : invisiblePlayers) {
                    Player invis = Bukkit.getPlayer(uid);
                    if (invis != null && !invis.equals(viewer)) {
                        sendArmorState(invis, viewer, true);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player viewer = e.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uid : invisiblePlayers) {
                    Player invis = Bukkit.getPlayer(uid);
                    if (invis != null && !invis.equals(viewer)) {
                        sendArmorState(invis, viewer, true);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();

        if (!invisiblePlayers.contains(victim.getUniqueId())) return;

        revealArmorFor(victim);
        lastLocations.remove(victim.getUniqueId());
        victim.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void tickFootsteps() {
        for (UUID uid : new HashSet<>(invisiblePlayers)) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;

            if (!p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                lastLocations.remove(uid);
                continue;
            }

            if (onlyOnGround && (!p.isOnGround() || p.getFallDistance() < 0.0F))
                continue;
            if (disableWhileSneaking && p.isSneaking())
                continue;

            Location current = p.getLocation();
            Location last = lastLocations.get(uid);

            if (last == null || hasMoved(last, current)) {

                Vector dir = current.getDirection().normalize();
                Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.25);
                Vector left = right.clone().multiply(-1);

                Location leftFoot = current.clone().add(left).add(0, 0.05, 0);
                Location rightFoot = current.clone().add(right).add(0, 0.05, 0);

                for (Player viewer : p.getWorld().getPlayers()) {
                    if (!viewer.equals(p)) {
                        viewer.playEffect(leftFoot, org.bukkit.Effect.FOOTSTEP, 0);
                        viewer.playEffect(rightFoot, org.bukkit.Effect.FOOTSTEP, 0);
                    }
                }

                lastLocations.put(uid, current.clone());
            }
        }
    }

    private boolean hasMoved(Location from, Location to) {
        return from.getWorld() == to.getWorld() &&
                (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ());
    }
}
