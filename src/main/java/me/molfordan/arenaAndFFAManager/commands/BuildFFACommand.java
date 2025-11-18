package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildFFACommand implements CommandExecutor {

    private final ConfigManager configManager;

    public BuildFFACommand(ConfigManager configManager){
        this.configManager = configManager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!(sender instanceof Player)) return true;

        String prefix = configManager.getServerPrefix();

        Player player = (Player) sender;

        Location buildFFALoc = configManager.getBuildFFALocation();
        String worldName = configManager.getBuildFFAWorldName();

        if (buildFFALoc == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cFailed to send you to the BuildFFA, Please Contact Admins."));
            return true;
        }

        if (worldName == null) return true;

        if (player.getWorld().getName().equals(worldName)){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cYou are already connected to this server"));
            return true;
        }


        player.teleport(buildFFALoc);
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " sending you to BuildFFA...."));

        return true;
    }
}
