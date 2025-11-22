package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnfrozenCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public UnfrozenCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!(sender.isOp() || sender.hasPermission("arenamap.freeze"))) return true;

        if (args.length == 0) {
            sender.sendMessage("Usage: /freeze <player>");
            return true;
        }

        Player staff = (Player) sender;

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is not online.");
            return true;
        }

        if (target.equals(staff)) {
            sender.sendMessage(ChatColor.RED + "You cannot freeze yourself.");
            return true;
        }

        if (!plugin.getFrozenManager().isFrozen(target)) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not frozen.");
            return true;
        }

        plugin.getFrozenManager().unfreezePlayer(target, staff);


        return true;
    }
}
