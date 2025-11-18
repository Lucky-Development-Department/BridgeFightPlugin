package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoadWorldCommand implements CommandExecutor {

    private ConfigManager configManager;

    public LoadWorldCommand(ConfigManager configManager){
        this.configManager = configManager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (args.length == 0){
            sender.sendMessage(ChatColor.RED + "Not Enough Args!");
            return true;
        }

        String worldName = args[0];
        String worldType = args[1];

        configManager.loadWorld(worldName, WorldType.valueOf(worldType));

        if (worldName == null){
            sender.sendMessage(ChatColor.RED + "Failed to load the world!");
            return true;
        }

        if (worldType == null){
            sender.sendMessage(ChatColor.RED + "You might not typing the correct WorldType. (Available WorldType = NORMAL, FLAT, CUSTOMIZED, VERSION_1_1, LARGE_BIOMES, AMPLIFIED");
            return true;
        }






        return true;
    }
}
