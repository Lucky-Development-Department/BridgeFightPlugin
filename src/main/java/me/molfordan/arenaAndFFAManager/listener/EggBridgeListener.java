package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.task.EggBridgeTask;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class EggBridgeListener implements Listener {

    @EventHandler
    public void onEggThrow(ProjectileLaunchEvent event) {

        if (!(event.getEntity() instanceof Egg)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Egg egg = (Egg) event.getEntity();
        Player player = (Player) egg.getShooter();

        new EggBridgeTask(player, egg, ArenaAndFFAManager.getInstance());
    }
}
