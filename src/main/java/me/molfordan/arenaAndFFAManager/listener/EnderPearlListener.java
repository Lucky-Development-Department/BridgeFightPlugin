package me.molfordan.arenaAndFFAManager.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderPearlListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_SECONDS = 5;
    private static final long COOLDOWN_MILLIS = COOLDOWN_SECONDS * 1000;

    @EventHandler
    public void onPlayerUseEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(playerId)) {
            long lastUseTime = cooldowns.get(playerId);
            long timeRemaining = (lastUseTime + COOLDOWN_MILLIS) - currentTime;
            if (timeRemaining > 0) {
                event.setCancelled(true);
                double secondsRemaining = timeRemaining / 1000.0;
                String formattedTime = String.format("%.1f", secondsRemaining);
                player.sendMessage("§cYou must wait §e" + formattedTime + "s §cbefore using another Ender Pearl!");
                return;
            }
        }
        cooldowns.put(playerId, currentTime);
    }
}
