package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.manager.DeathMessageManager;
import me.molfordan.arenaAndFFAManager.manager.PlatformManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

import java.util.UUID;

public class BridgeFightListener implements Listener {

    private final PlatformManager platformManager;
    private final ConfigManager configManager;
    private final ArenaAndFFAManager plugin;

    public BridgeFightListener(PlatformManager platformManager,
                               ConfigManager configManager, ArenaAndFFAManager plugin) {
        this.platformManager = platformManager;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    /**
     * Teleport when falling below platform
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        String world = configManager.getBridgeFightWorldName();
        if (world == null || !p.getWorld().getName().equals(world)) return;

        Location to = event.getTo();
        if (to == null) return;

        Arena arena = ArenaAndFFAManager.getPlugin().getArenaManager().getArenaByLocationIgnoreY(p.getLocation());
        if (arena == null || to.getY() > arena.getVoidLimit()) return;

        PlatformRegion region = platformManager.fromLocationIgnoreY(to);
        PlayerStats stats = ArenaAndFFAManager.getPlugin().getStatsManager().getStats(p.getUniqueId());

        if (region != null && region.getSpawn() != null) {
            p.teleport(region.getSpawn());
            stats.resetBridgeStreak();
            ArenaAndFFAManager.getPlugin().getStatsManager().savePlayerAsync(stats);

            // Add death message handling
            ArenaAndFFAManager.getPlugin().getDeathMessageManager().handleDeath(
                    p,
                    arena,
                    true,  // isVoidDeath = true
                    false  // isQuit = false
            );
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            plugin.getKitManager().applyBridgeFightKit(p);
            return;
        }

        //.setHealth(0);
    }

    /**
     * Zero damage but allow knockback
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        String world = configManager.getBridgeFightWorldName();
        if (world == null) return;

        Player victim = (Player) event.getEntity();
        if (!victim.getWorld().getName().equals(world)) return;


        Player damager = (Player) event.getDamager();

        if (!plugin.getDeathMessageManager().handleDuelHit(damager, victim)) {
            event.setCancelled(true);
        }

        event.setDamage(0.0);
    }



    @EventHandler
    public void onAnvilInteract(PlayerInteractEvent event){
        if (!(event.getPlayer() instanceof Player)) return;

        String world = configManager.getBridgeFightWorldName();
        if (world == null) return;

        Player player = (Player) event.getPlayer();
        if (!player.getWorld().getName().equals(world)) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK){
            if (event.getClickedBlock().getType() == Material.ANVIL){
                event.setCancelled(true);
            }
        }


    }
}
