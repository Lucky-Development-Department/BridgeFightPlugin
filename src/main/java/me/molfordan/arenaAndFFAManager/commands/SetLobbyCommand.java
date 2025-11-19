package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobbyCommand implements CommandExecutor {

    private final ConfigManager configManager;

    // 1. Add constructor to properly initialize the ConfigManager
    public SetLobbyCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) return true;

        // Check if the command sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set the lobby location.");
            return true;
        }

        Player player = (Player) sender;

        // Check for permission (Assuming a permission like arena.setlobby)
        if (!(player.hasPermission("arenamap.admin") || player.isOp())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) return true;

        Location loc = player.getLocation();

        switch (args[0]){
            case "spawn":
                configManager.setLobbyLocation(loc);

                // 3. Send success feedback
                player.sendMessage(ChatColor.GREEN + "Lobby spawn location set successfully at:");
                player.sendMessage(ChatColor.YELLOW + "  World: " + loc.getWorld().getName());
                player.sendMessage(String.format(
                        ChatColor.YELLOW + "  Coords: X:%.2f, Y:%.2f, Z:%.2f",
                        loc.getX(), loc.getY(), loc.getZ()
                ));
                player.sendMessage(String.format(
                        ChatColor.YELLOW + "  Direction: Yaw:%.1f, Pitch:%.1f",
                        loc.getYaw(), loc.getPitch()
                ));

                configManager.saveConfig();

                break;
            case "buildffa":
                configManager.setBuildFFALocation(loc);

                // 3. Send success feedback
                player.sendMessage(ChatColor.GREEN + "Lobby spawn location set successfully at:");
                player.sendMessage(ChatColor.YELLOW + "  World: " + loc.getWorld().getName());
                player.sendMessage(String.format(
                        ChatColor.YELLOW + "  Coords: X:%.2f, Y:%.2f, Z:%.2f",
                        loc.getX(), loc.getY(), loc.getZ()
                ));
                player.sendMessage(String.format(
                        ChatColor.YELLOW + "  Direction: Yaw:%.1f, Pitch:%.1f",
                        loc.getYaw(), loc.getPitch()
                ));

                configManager.saveConfig();
                break;
            case "bridgefight":
                configManager.setBridgeFightLocation(loc);

                // 3. Send success feedback
                player.sendMessage(ChatColor.GREEN + "Lobby spawn location set successfully at:");
                player.sendMessage(ChatColor.YELLOW + "  World: " + loc.getWorld().getName());
                player.sendMessage(String.format(
                        ChatColor.YELLOW + "  Coords: X:%.2f, Y:%.2f, Z:%.2f",
                        loc.getX(), loc.getY(), loc.getZ()
                ));
                player.sendMessage(String.format(
                        ChatColor.YELLOW + "  Direction: Yaw:%.1f, Pitch:%.1f",
                        loc.getYaw(), loc.getPitch()
                ));

                configManager.saveConfig();
                break;
        }

        // 2. Use the ConfigManager to save the complete location
        return true;
    }
}