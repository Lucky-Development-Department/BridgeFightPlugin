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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            plugin.getQueueManager().leaveQueue(player);
        } else {
            plugin.getQueueGUI().openMain(player);
        }
        return true;
    }
}
