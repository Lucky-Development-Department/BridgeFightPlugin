package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.manager.TeleportPendingManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeleportCancelListener implements Listener {

    private final TeleportPendingManager pending;

    public TeleportCancelListener(TeleportPendingManager pending) {
        this.pending = pending;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!pending.isWaiting(e.getPlayer())) return;

        // If player moved, cancel teleport
        if (pending.hasMoved(e.getPlayer())) {
            pending.remove(e.getPlayer());
            e.getPlayer().sendMessage(ChatColor.RED + "Teleport cancelled because you moved!");
        }
    }
}
