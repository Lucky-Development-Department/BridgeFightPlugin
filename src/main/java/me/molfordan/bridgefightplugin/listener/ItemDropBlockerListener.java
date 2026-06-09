package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.object.enums.ArenaType;
import me.molfordan.bridgefightplugin.manager.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemDropBlockerListener implements Listener {

    private final ArenaManager arenaManager;
    private final BridgeFightPlugin plugin;

    public ItemDropBlockerListener(ArenaManager arenaManager, BridgeFightPlugin plugin) {
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
