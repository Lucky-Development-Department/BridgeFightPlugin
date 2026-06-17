package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class RematchCommand implements CommandExecutor {
    private final BridgeFightPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RematchCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Cooldown check
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Please wait " + (timeLeft / 1000 + 1) + " seconds before using this command again.");
                return true;
            }
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 3000); // 3 second cooldown

        BedFightSession session = plugin.getBedFightManager().getSession(player);

        if (session == null || session.getQueueType() != QueueType.DUEL) {
            player.sendMessage(ChatColor.RED + "You must be in a Duel match to use this command.");
            return true;
        }

        // Logic to initiate rematch
        // 1. Check if both players are still in the session
        Set<UUID> allPlayers = session.getAllPlayers();
        if (allPlayers.size() != 2) {
            player.sendMessage(ChatColor.RED + "Both players must be present for a rematch.");
            return true;
        }

        // Check timeout
        UUID duelId = session.getMatchWorld().getUID();
        if (!plugin.getDuelScoreManager().isRematchAllowed(duelId)) {
            player.sendMessage(ChatColor.RED + "The rematch window (30 seconds) has expired.");
            return true;
        }

        // Find opponent in the session
        Player opponent = null;
        for (UUID uuid : allPlayers) {
            if (!uuid.equals(player.getUniqueId())) {
                opponent = org.bukkit.Bukkit.getPlayer(uuid);
                break;
            }
        }

        if (opponent == null || !opponent.isOnline()) {
            player.sendMessage(ChatColor.RED + "Your opponent is not online.");
            return true;
        }

        // Logic: Check if opponent already requested a rematch
        if (plugin.getDuelScoreManager().hasRematchRequest(opponent.getUniqueId(), player.getUniqueId())) {
            // Accept the duel
            player.performCommand("duel accept " + opponent.getName());
            plugin.getDuelScoreManager().removeRematchRequest(opponent.getUniqueId());
        } else if (plugin.getDuelScoreManager().hasRematchRequest(player.getUniqueId(), opponent.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You have already sent a rematch request to " + opponent.getName() + "!");
        } else {
            // Send new duel request
            plugin.getDuelScoreManager().setRematchRequest(player.getUniqueId(), opponent.getUniqueId());
            player.performCommand("duel " + opponent.getName());
            player.sendMessage(ChatColor.GREEN + "Rematch request sent to " + opponent.getName() + "!");
        }
        
        return true;
    }
}
