package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.commands.admin.ToggleBridgeEggCommand;
import me.molfordan.bridgefightplugin.task.EggBridgeTask;
import me.molfordan.bridgefightplugin.BridgeFightPlugin;
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

        // Check if bridge eggs are enabled
        if (!ToggleBridgeEggCommand.isBridgeEggEnabled()) {
            event.setCancelled(true);
            return;
        }

        Egg egg = (Egg) event.getEntity();
        Player player = (Player) egg.getShooter();

        new EggBridgeTask(player, egg, BridgeFightPlugin.getInstance());
    }
}
