package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ArenaConfigReloadCommand implements CommandExecutor {


    private ConfigManager configManager;

    public ArenaConfigReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender == null) return true;


        if (!sender.isOp()) {
            sender.sendMessage("You must be an operator to use this command.");
            return true;
        }

        configManager.reloadConfig();

        sender.sendMessage(ChatColor.GREEN + "Config reloaded.");

        return true;
    }
}
