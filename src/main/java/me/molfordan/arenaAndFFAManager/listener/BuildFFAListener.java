package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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

        // Death detection: if health drops to 0.5 or below (exclude fall damage)
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL && 
            victim.getHealth() - event.getFinalDamage() <= 0.5) {
            event.setCancelled(true);
            processBuildFFADeath(victim, false);
        }
    }

    @org.bukkit.event.EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isInBuildFFAWorld(player)) return;

        Arena arena = ArenaAndFFAManager.getPlugin().getArenaManager().getArenaByLocationIgnoreY(player.getLocation());
        
        // If not directly inside an arena, find any FFABUILD arena in this world to get void limit
        if (arena == null) {
            for (Arena a : ArenaAndFFAManager.getPlugin().getArenaManager().getArenas()) {
                if (a.getWorldName().equals(player.getWorld().getName()) && a.getType() == me.molfordan.arenaAndFFAManager.object.enums.ArenaType.FFABUILD) {
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
        Arena arena = ArenaAndFFAManager.getPlugin().getArenaManager().getArenaByLocationIgnoreY(victim.getLocation());

        // Fallback for stats tracking if they drifted outside
        if (arena == null) {
            for (Arena a : ArenaAndFFAManager.getPlugin().getArenaManager().getArenas()) {
                if (a.getWorldName().equals(victim.getWorld().getName()) && a.getType() == me.molfordan.arenaAndFFAManager.object.enums.ArenaType.FFABUILD) {
                    arena = a;
                    break;
                }
            }
        }
        
        // Handle stats and messages via DeathMessageManager
        ArenaAndFFAManager.getPlugin().getDeathMessageManager().handleDeath(victim, arena, isVoid, false);

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
        
        victim.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        victim.sendMessage(ChatColor.RED + "You died!");
    }

    @org.bukkit.event.EventHandler
    public void onDrinkPotion(PlayerItemConsumeEvent event){
        Player player = event.getPlayer();
        if (event.getItem().getType() != Material.POTION) return;

        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () ->
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
