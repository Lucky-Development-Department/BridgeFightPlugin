package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QueueQuitListener implements Listener {
    private final BridgeFightPlugin plugin;

    public QueueQuitListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getMatchmakingService().removeFromQueue(event.getPlayer());
    }
}
