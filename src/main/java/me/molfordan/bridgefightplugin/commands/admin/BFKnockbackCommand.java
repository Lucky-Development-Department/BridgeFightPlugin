package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BFKnockbackCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public BFKnockbackCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenamap.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getKnockbackConfig().reload();
            sender.sendMessage(ChatColor.GREEN + "Knockback configuration reloaded!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /bfknockback reload");
        return true;
    }
}
