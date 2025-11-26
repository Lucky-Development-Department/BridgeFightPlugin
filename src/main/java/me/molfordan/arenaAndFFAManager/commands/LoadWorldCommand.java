package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoadWorldCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final ArenaAndFFAManager plugin;

    public LoadWorldCommand(ConfigManager configManager, ArenaAndFFAManager plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be OP to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /loadworld <name> <type>");
            sender.sendMessage(ChatColor.YELLOW + "Types: NORMAL, FLAT, VOID");
            return true;
        }

        String worldName = args[0];
        String worldTypeInput = args[1].toUpperCase();

        World world;

        // Handle VOID world loader
        if (worldTypeInput.equals("VOID")) {
            world = configManager.loadVoidWorld(worldName);

            if (world == null) {
                try {
                    configManager.createVoidWorld(worldName);
                    sender.sendMessage(ChatColor.GREEN + "No World Found, Created New Void World: " + worldName);
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Failed to load VOID world: " + worldName);
                    e.printStackTrace();
                }

            } else {
                sender.sendMessage(ChatColor.GREEN + "Successfully loaded VOID world: " + worldName);
            }

            plugin.getRegionManager().clearRegions();
            plugin.getRegionManager().loadAllFromConfig();
            plugin.getArenaManager().unloadArenas();
            plugin.getArenaManager().loadArenas();

            return true;
        }

        // Handle normal world types
        try {
            WorldType type = WorldType.valueOf(worldTypeInput);

            configManager.loadWorld(worldName, type);

            if (worldName == null) {
                sender.sendMessage(ChatColor.RED + "Failed to load world: " + worldName);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Successfully loaded world: " + worldName + " (" + type.name() + ")");

            }

        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid world type: " + worldTypeInput);
            sender.sendMessage(ChatColor.YELLOW + "Available: NORMAL, FLAT, VOID, AMPLIFIED, LARGE_BIOMES, CUSTOMIZED");
        }

        return true;
    }
}
