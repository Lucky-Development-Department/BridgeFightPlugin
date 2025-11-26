package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.manager.BridgeFightBanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class BridgeFightBanListener implements Listener {

    private final BridgeFightBanManager banManager;

    public BridgeFightBanListener(BridgeFightBanManager banManager) {
        this.banManager = banManager;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!banManager.isPlayerBanned(player.getUniqueId())) return;

        String msg = event.getMessage().toLowerCase();

        // block /plat and /bigplat
        if (msg.startsWith("/plat") || msg.startsWith("/bigplat")) {
            event.setCancelled(true);
            player.sendMessage("§cYou are banned from BridgeFight and cannot use this command.");
        }
    }
}
