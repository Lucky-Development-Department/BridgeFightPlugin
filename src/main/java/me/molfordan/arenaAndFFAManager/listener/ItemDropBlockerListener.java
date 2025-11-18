package me.molfordan.arenaAndFFAManager.listener;



import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemDropBlockerListener implements Listener {

    private final ArenaManager arenaManager;

    public ItemDropBlockerListener(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getArenaByLocation(player.getLocation());

        if (arena != null && arena.getType() == ArenaType.FFABUILD) {
            event.setCancelled(true);
        }
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

