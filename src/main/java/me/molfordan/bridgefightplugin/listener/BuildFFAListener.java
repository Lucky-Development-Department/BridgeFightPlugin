package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.ConfigManager;
import me.molfordan.bridgefightplugin.kits.KitManager;
import me.molfordan.bridgefightplugin.object.Arena;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class BuildFFAListener implements Listener {

    private final ConfigManager configManager;
    private final KitManager kitManager;

    public BuildFFAListener(ConfigManager configManager, KitManager kitManager) {
        this.configManager = configManager;
        this.kitManager = kitManager;
    }

    private boolean isInBuildFFAWorld(Player player) {
        Location loc = configManager.getBuildFFALocation();
        if (loc == null) return false;
        World world = loc.getWorld();
        return player.getWorld().equals(world);
    }

    // ======================================
    // 1) DEATH & VOID HANDLING
    // ======================================
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!isInBuildFFAWorld(victim)) return;

        // Death detection: if health drops to 0.1 or below (exclude fall damage)
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL &&
            victim.getHealth() - event.getFinalDamage() <= 0.1) {

            // If this is a player-vs-player hit, tag the victim BEFORE cancelling so
            // handleDeath can resolve the killer even on one-shot / instant-kill hits
            // (e.g. Sharpness 100 sword) where no prior combat tag exists.
            if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.event.entity.EntityDamageByEntityEvent byEntity =
                        (org.bukkit.event.entity.EntityDamageByEntityEvent) event;
                Player damager = null;
                if (byEntity.getDamager() instanceof Player) {
                    damager = (Player) byEntity.getDamager();
                } else if (byEntity.getDamager() instanceof org.bukkit.entity.Projectile) {
                    org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) byEntity.getDamager();
                    if (proj.getShooter() instanceof Player) {
                        damager = (Player) proj.getShooter();
                    }
                }
                if (damager != null) {
                    me.molfordan.bridgefightplugin.object.Arena arena =
                            BridgeFightPlugin.getPlugin().getArenaManager().getArenaByLocationIgnoreY(victim.getLocation());
                    BridgeFightPlugin.getPlugin().getCombatManager().tag(victim, damager, arena);
                }
            }

            event.setCancelled(true);
            processBuildFFADeath(victim, false);
        }
    }

    @org.bukkit.event.EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isInBuildFFAWorld(player)) return;

        Arena arena = BridgeFightPlugin.getPlugin().getArenaManager().getArenaByLocationIgnoreY(player.getLocation());
        
        // If not directly inside an arena, find any FFABUILD arena in this world to get void limit
        if (arena == null) {
            for (Arena a : BridgeFightPlugin.getPlugin().getArenaManager().getArenas()) {
                if (a.getWorldName().equals(player.getWorld().getName()) && a.getType() == me.molfordan.bridgefightplugin.object.enums.ArenaType.FFABUILD) {
                    arena = a;
                    break;
                }
            }
        }
        
        if (arena == null) return;

        if (event.getTo().getY() <= arena.getVoidLimit()) {
            processBuildFFADeath(player, true);
        }
    }

    private void processBuildFFADeath(Player victim, boolean isVoid) {
        Arena arena = BridgeFightPlugin.getPlugin().getArenaManager().getArenaByLocationIgnoreY(victim.getLocation());

        // Fallback for stats tracking if they drifted outside
        if (arena == null) {
            for (Arena a : BridgeFightPlugin.getPlugin().getArenaManager().getArenas()) {
                if (a.getWorldName().equals(victim.getWorld().getName()) && a.getType() == me.molfordan.bridgefightplugin.object.enums.ArenaType.FFABUILD) {
                    arena = a;
                    break;
                }
            }
        }
        
        // Handle stats and messages via DeathMessageManager
        BridgeFightPlugin.getPlugin().getDeathMessageManager().handleDeath(victim, arena, isVoid, false);
        BridgeFightPlugin.getPlugin().getEnderPearlListener().removeCooldown(victim);

        // Reset player state
        victim.setHealth(20.0);
        victim.setFoodLevel(20);
        victim.setFireTicks(0);
        victim.setFallDistance(0);
        
        Location spawn = configManager.getBuildFFALocation();
        if (spawn != null) {
            victim.teleport(spawn);
        }

        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);
        
        // Re-apply kit
        kitManager.applyBuildFFAKit(victim);

        // Cancel any active pearls to prevent landing after death
        BridgeFightPlugin.getPlugin().getEnderPearlListener().cancelPearls(victim);
        
        victim.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        victim.sendMessage(ChatColor.RED + "You died!");
    }

    @org.bukkit.event.EventHandler
    public void onDrinkPotion(PlayerItemConsumeEvent event){
        Player player = event.getPlayer();
        if (event.getItem().getType() != Material.POTION) return;

        Bukkit.getScheduler().runTaskLater(BridgeFightPlugin.getPlugin(), () ->
                minusAmount(event.getPlayer(), new ItemStack(Material.GLASS_BOTTLE), 1), 5L);
    }

    public void minusAmount(Player p, ItemStack i, int amount) {
        if (i.getAmount() - amount <= 0) {
            p.getInventory().removeItem(i);
            return;
        }
        i.setAmount(i.getAmount() - amount);
        p.updateInventory();
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInBuildFFAWorld(player)) return;

        // 1. Check if the player is sneaking and holding an item
        // If they are, we DON'T cancel the event so they can place the block.
        if (player.isSneaking() && event.getItem() != null && event.getItem().getType().isBlock()) {
            return;
        }

        // 2. Otherwise, cancel interaction for the specified containers
        if (event.getClickedBlock() != null) {
            Material type = event.getClickedBlock().getType();
            if (type == Material.CHEST ||
                    type == Material.ENDER_CHEST ||
                    type == Material.TRAPPED_CHEST ||
                    type == Material.HOPPER ||
                    type == Material.WORKBENCH) { // You can combine both methods here
                event.setCancelled(true);
            }
        }
    }

}
