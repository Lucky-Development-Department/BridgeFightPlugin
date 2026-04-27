package me.molfordan.arenaAndFFAManager.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    public InvisPlayerListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerPacketListener();

        new BukkitRunnable() {
            @Override
            public void run() {
                tickFootsteps();
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void registerPacketListener() {
        if (ProtocolLibrary.getProtocolManager() == null) return;

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event == null || event.getPacket() == null) return;

                try {
                    if (event.getPlayer() == null) return;
                } catch (Exception e) {
                    return;
                }

                try {
                    PacketContainer packet = event.getPacket();
                    if (packet.getIntegers().size() < 1) return;
                    
                    int entityId = packet.getIntegers().read(0);
                    Player receiver = event.getPlayer();

                    Player victim = null;
                    for (Player p : receiver.getWorld().getPlayers()) {
                        if (p.getEntityId() == entityId) {
                            victim = p;
                            break;
                        }
                    }

                    if (victim != null && !victim.equals(receiver)) {
                        if (invisiblePlayers.contains(victim.getUniqueId())) {
                            if (packet.getIntegers().size() < 2) return;
                            int slot = packet.getIntegers().read(1);
                            
                            // Slots 1-4 are armor slots in 1.8
                            if (slot >= 1 && slot <= 4) {
                                if (packet.getItemModifier().size() > 0) {
                                    packet.getItemModifier().write(0, new ItemStack(Material.AIR));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public void hideArmor(Player victim, Player receiver) {
        if (victim.equals(receiver) || victim == null || receiver == null || !receiver.isOnline()) return;
        try {
            CraftPlayer craftReceiver = (CraftPlayer) receiver;
            if (craftReceiver.getHandle() == null || craftReceiver.getHandle().playerConnection == null) return;
            
            PlayerConnection conn = craftReceiver.getHandle().playerConnection;
            int id = victim.getEntityId();
            
            ItemStack air = new ItemStack(Material.AIR);
            for (int slot = 1; slot <= 4; slot++) {
                conn.sendPacket(new PacketPlayOutEntityEquipment(id, slot, CraftItemStack.asNMSCopy(air)));
            }
        } catch (Throwable ignored) {}
    }

    public void showArmor(Player victim, Player receiver) {
        if (victim.equals(receiver) || victim == null || receiver == null || !receiver.isOnline()) return;
        try {
            CraftPlayer craftReceiver = (CraftPlayer) receiver;
            if (craftReceiver.getHandle() == null || craftReceiver.getHandle().playerConnection == null) return;
            
            PlayerConnection conn = craftReceiver.getHandle().playerConnection;
            int id = victim.getEntityId();
            ItemStack[] armor = victim.getInventory().getArmorContents();
            
            conn.sendPacket(new PacketPlayOutEntityEquipment(id, 4, CraftItemStack.asNMSCopy(armor[3])));
            conn.sendPacket(new PacketPlayOutEntityEquipment(id, 3, CraftItemStack.asNMSCopy(armor[2])));
            conn.sendPacket(new PacketPlayOutEntityEquipment(id, 2, CraftItemStack.asNMSCopy(armor[1])));
            conn.sendPacket(new PacketPlayOutEntityEquipment(id, 1, CraftItemStack.asNMSCopy(armor[0])));
        } catch (Throwable ignored) {}
    }

    private void updateArmorForEveryone(Player target, boolean hide) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (hide) {
                hideArmor(target, viewer);
            } else {
                showArmor(target, viewer);
            }
        }
    }

    public void hideArmorFor(Player p) {
        invisiblePlayers.add(p.getUniqueId());
        // Run multiple times to ensure visibility is overridden
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateArmorForEveryone(p, true), 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateArmorForEveryone(p, true), 5L);
    }

    public void revealArmorFor(Player p) {
        if (!invisiblePlayers.contains(p.getUniqueId())) return;
        
        invisiblePlayers.remove(p.getUniqueId());
        lastLocations.remove(p.getUniqueId());
        updateArmorForEveryone(p, false);

        BukkitTask t = revealTasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrinkPotion(final PlayerItemConsumeEvent event) {
        final Player p = event.getPlayer();
        final ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                ItemStack inHand = p.getItemInHand();
                if (inHand != null && inHand.getType() == Material.GLASS_BOTTLE) {
                    if (inHand.getAmount() > 1) {
                        inHand.setAmount(inHand.getAmount() - 1);
                    } else {
                        p.setItemInHand(null);
                    }
                    p.updateInventory();
                }
            }
        }, 1L);

        boolean isInvis = false;
        try {
            Potion potion = Potion.fromItemStack(item);
            if (potion != null && potion.getType() == PotionType.INVISIBILITY) {
                isInvis = true;
            }
        } catch (Throwable ignored) {}

        if (!isInvis && item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta) {
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            for (PotionEffect effect : meta.getCustomEffects()) {
                if (effect.getType().equals(PotionEffectType.INVISIBILITY)) {
                    isInvis = true;
                    break;
                }
            }
            if (!isInvis && meta.hasDisplayName() && meta.getDisplayName().toLowerCase().contains("invisibility")) {
                isInvis = true;
            }
        }

        if (!isInvis) return;

        hideArmorFor(p);

        BukkitTask prev = revealTasks.remove(p.getUniqueId());
        if (prev != null) prev.cancel();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;

                int duration = 0;
                for (PotionEffect eff : p.getActivePotionEffects()) {
                    if (eff.getType().equals(PotionEffectType.INVISIBILITY)) {
                        duration = eff.getDuration();
                        break;
                    }
                }

                if (duration <= 0) duration = 20 * 45; 

                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (p.isOnline() && !p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            revealArmorFor(p);
                        }
                    }
                }.runTaskLater(plugin, duration + 5L);

                revealTasks.put(p.getUniqueId(), task);
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player viewer = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!viewer.isOnline()) return;
                for (UUID uid : invisiblePlayers) {
                    Player invis = Bukkit.getPlayer(uid);
                    if (invis != null && !invis.equals(viewer)) {
                        hideArmor(invis, viewer);
                    }
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player viewer = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!viewer.isOnline()) return;
                for (UUID uid : invisiblePlayers) {
                    Player invis = Bukkit.getPlayer(uid);
                    if (invis != null && !invis.equals(viewer)) {
                        hideArmor(invis, viewer);
                    }
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();

        // Check for self-damage (standard)
        if (e.getDamager().equals(victim)) return;

        // Check for self-damage from fireballs using our tracker
        if (e.getDamager() instanceof org.bukkit.entity.Fireball) {
            org.bukkit.entity.Fireball fb = (org.bukkit.entity.Fireball) e.getDamager();
            Player shooter = plugin.getFireballTracker().getFireballOwner(fb);
            if (victim.equals(shooter)) {
                return; // Self-damage from own fireball
            }
        }

        if (e.getDamager() instanceof org.bukkit.entity.TNTPrimed){
            org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) e.getDamager();
            Player tntOwner = plugin.getTNTTracker().getTNTOwner(tnt);
            if (victim.equals(tntOwner)) {
                return; // Self-damage from own tnt
            }
        }

        if (!invisiblePlayers.contains(victim.getUniqueId())) return;
        
        revealArmorFor(victim);
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

            boolean onlyOnGround = true;
            boolean disableWhileSneaking = false;
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
