package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemDropBlockerListener implements Listener {

    private final ArenaManager arenaManager;
    private final ArenaAndFFAManager plugin;

    public ItemDropBlockerListener(ArenaManager arenaManager, ArenaAndFFAManager plugin) {
        this.arenaManager = arenaManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();
        // Allow drops ONLY in BedFight worlds (bf_)
        if (player.getWorld().getName().startsWith("bf_")) {
            event.setCancelled(false);
            return;
        }

        String bridgefightWorld = plugin.getConfigManager().getBridgeFightWorldName();
        String buildffaWorld = plugin.getConfigManager().getBuildFFAWorldName();

        if (bridgefightWorld == null) return;
        if (buildffaWorld == null) return;

        if (player.getWorld().getName().equals(bridgefightWorld) || player.getWorld().getName().equals(buildffaWorld)){
            event.setCancelled(true);
            return;
        }



        // Otherwise block drops everywhere else

    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = arenaManager.getArenaByLocation(player.getLocation());

        if (arena != null && arena.getType() == ArenaType.FFABUILD) {
            event.getDrops().clear();
        }
    }
}
