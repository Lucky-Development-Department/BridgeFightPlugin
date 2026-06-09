package me.molfordan.arenaAndFFAManager.queue;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QueueCommand implements CommandExecutor {
    private final ArenaAndFFAManager plugin;

    public QueueCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
                plugin.getMatchmakingService().removeFromQueue(player);
                player.sendMessage(ChatColor.RED + "You have left the queue.");
            } else {
                player.sendMessage(ChatColor.RED + "You are not in a queue.");
            }
            return true;
        }

        // Default behavior: Open queue menu
        plugin.getMatchmakingService().openQueueGUI(player);
        return true;
    }
}
