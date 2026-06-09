package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ConfigReloadCommand implements CommandExecutor {

    private final ConfigManager configManager;

    public ConfigReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("arenamap.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Handle both /arenaconfig reload and a direct /configreload if registered
        if (label.equalsIgnoreCase("arenaconfig")) {
            if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ChatColor.RED + "Usage: /arenaconfig reload");
                return true;
            }
        }

        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
        return true;
    }
}
