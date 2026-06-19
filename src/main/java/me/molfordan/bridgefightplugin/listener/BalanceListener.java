package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.event.BridgeFightKillEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BalanceListener implements Listener {

    private final BridgeFightPlugin plugin;

    public BalanceListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKill(BridgeFightKillEvent event) {
        // Check if the coins/balance system is enabled in config
        if (!plugin.getConfig().getBoolean("coins.enabled", true)) {
            return;
        }

        Player killer = event.getKiller();
        if (killer == null || !killer.isOnline()) {
            return;
        }

        // Retrieve reward details from config
        int coinsToReward = plugin.getConfig().getInt("coins.per-kill", 10);
        String messageTemplate = plugin.getConfig().getString("coins.kill-message", "&a+{coins} coins!");

        // Add coins to killer's balance
        plugin.getBalanceManager().addBalance(killer.getUniqueId(), coinsToReward);

        // Format and send kill message to the killer
        if (messageTemplate != null && !messageTemplate.isEmpty()) {
            String message = messageTemplate.replace("{coins}", String.valueOf(coinsToReward));
            killer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
