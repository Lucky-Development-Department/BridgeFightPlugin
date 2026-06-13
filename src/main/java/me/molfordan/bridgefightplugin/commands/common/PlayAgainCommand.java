package me.molfordan.bridgefightplugin.commands.common;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public class PlayAgainCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public PlayAgainCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        long endTime = plugin.getBedFightManager().getGameEndTime(player.getUniqueId());
        
        if (System.currentTimeMillis() - endTime > 60000) {
            player.sendMessage(ChatColor.RED + "The play again period has expired!");
            return true;
        }

        if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in a queue!");
            return true;
        }

        MetadataValue meta = player.getMetadata("lastQueueType").stream().findFirst().orElse(null);
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "Could not find your last queue!");
            return true;
        }

        try {
            QueueType type = QueueType.valueOf(meta.asString());
            
            plugin.getLogger().info("DEBUG: PlayAgain for " + player.getName() + " with type: " + type.name());
            
            // Clean up from current session but stay in world
            plugin.getBedFightManager().removePlayerFromSession(player);
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            
            plugin.getMatchmakingService().addToQueue(player, type);
            player.removeMetadata("lastQueueType", plugin);
            plugin.getLogger().info("DEBUG: PlayAgain successfully added " + player.getName() + " to queue.");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Could not determine last queue type.");
        }

        return true;
    }
}
