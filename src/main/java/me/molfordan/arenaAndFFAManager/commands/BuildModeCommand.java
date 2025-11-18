package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildModeCommand implements CommandExecutor {

    private ConfigManager configManager;

    public BuildModeCommand(ConfigManager configManager){
        this.configManager = configManager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;

        if (!player.isOp()) return true;

        configManager.toggleBuildMode(player.getUniqueId());
        String serverPrefix = configManager.getServerPrefix();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', serverPrefix));
        String message = configManager.isBuildMode(player.getUniqueId()) ? ChatColor.GREEN + "Build Mode Enabled" : ChatColor.RED + "Build Mode Disabled";
        player.sendMessage(message);

        return true;
    }
}
