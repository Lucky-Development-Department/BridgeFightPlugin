package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaBypassCommand implements CommandExecutor {
    private final ArenaManager manager;

    public ArenaBypassCommand(ArenaManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("arenamap.admin") || !sender.isOp()) return true;

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            String status = manager.isBypassing(player.getUniqueId())
                    ? ChatColor.GREEN + "ENABLED"
                    : ChatColor.RED + "DISABLED";
            player.sendMessage(ChatColor.GOLD + "Arena bypass status: " + status);
            return true;
        }

        manager.toggleBypass(player.getUniqueId());

        String status = manager.isBypassing(player.getUniqueId())
                ? ChatColor.GREEN + "ENABLED"
                : ChatColor.RED + "DISABLED";





        player.sendMessage(ChatColor.GOLD + "Arena bypass: " + status);
        return true;
    }
}