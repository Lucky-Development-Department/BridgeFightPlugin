package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.object.enums.ArenaType;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class BedFightCommand implements CommandExecutor {
    private final BridgeFightPlugin plugin;

    public BedFightCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("bedfight.admin")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "setup":
                return handleSetup(player, args);
            case "finish":
                return handleFinish(player);
            case "start":
                return handleStart(player, args);
            case "reload":
                plugin.getBedFightArenaManager().loadArenas();
                player.sendMessage(ChatColor.GREEN + "Reloaded BedFight arenas.");
                return true;
            default:
                return handleSet(player, sub, args);
        }
    }

    private boolean handleSetup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bedfight setup <worldName>");
            return true;
        }

        String worldName = args[1];
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists()) {
            player.sendMessage(ChatColor.RED + "World folder '" + worldName + "' not found in root directory.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Loading world '" + worldName + "'...");
        World world = Bukkit.createWorld(new WorldCreator(worldName));
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Failed to load world.");
            return true;
        }

        Arena arena = new Arena(worldName, world);
        arena.setType(ArenaType.BEDFIGHT);
        plugin.getBedFightArenaManager().startSetupSession(player, worldName, arena);

        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Started setup session for '" + worldName + "'.");
        player.sendMessage(ChatColor.YELLOW + "Use /bedfight <redspawn|bluespawn|redbed|bluebed|pos1|pos2|voidlimit|buildlimit> to configure.");
        return true;
    }

    private boolean handleSet(Player player, String sub, String[] args) {
        BedFightSetupSession session = plugin.getBedFightArenaManager().getSetupSession(player);
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not in a setup session. Use /bedfight setup <worldName>");
            return true;
        }

        Arena arena = session.getArena();
        Location loc = player.getLocation();

        switch (sub) {
            case "redspawn":
                arena.setRedSpawn(loc);
                player.sendMessage(ChatColor.RED + "Red Spawn set.");
                break;
            case "bluespawn":
                arena.setBlueSpawn(loc);
                player.sendMessage(ChatColor.BLUE + "Blue Spawn set.");
                break;
            case "redbed":
                arena.setRedBed(loc);
                player.sendMessage(ChatColor.RED + "Red Bed set.");
                break;
            case "bluebed":
                arena.setBlueBed(loc);
                player.sendMessage(ChatColor.BLUE + "Blue Bed set.");
                break;
            case "center":
                arena.setCenter(loc);
                player.sendMessage(ChatColor.GREEN + "Center set.");
                break;
            case "pos1":
                arena.setPos1(loc);
                player.sendMessage(ChatColor.GREEN + "Pos1 set.");
                break;
            case "pos2":
                arena.setPos2(loc);
                player.sendMessage(ChatColor.GREEN + "Pos2 set.");
                break;
            case "voidlimit":
                if (args.length < 2) return false;
                arena.setVoidLimit(Integer.parseInt(args[1]));
                player.sendMessage(ChatColor.GREEN + "Void limit set to " + args[1]);
                break;
            case "buildlimit":
                if (args.length < 2) return false;
                arena.setBuildLimitY(Integer.parseInt(args[1]));
                player.sendMessage(ChatColor.GREEN + "Build limit set to " + args[1]);
                break;
            default:
                sendUsage(player);
        }
        return true;
    }

    private boolean handleFinish(Player player) {
        BedFightSetupSession session = plugin.getBedFightArenaManager().getSetupSession(player);
        if (session == null) {
            player.sendMessage(ChatColor.RED + "No active setup session.");
            return true;
        }

        Arena arena = session.getArena();
        String name = session.getArenaName();

        // 1. Save Metadata
        arena.setFinished(true);
        plugin.getBedFightArenaManager().saveArena(arena);

        // 2. Slime-ify World
        player.sendMessage(ChatColor.YELLOW + "Converting world to Slime format...");
        World world = Bukkit.getWorld(name);
        if (world != null) {
            world.save();
            player.teleport(plugin.getConfigManager().getLobbyLocation());
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    Bukkit.unloadWorld(name, true);
                    File worldFolder = new File(Bukkit.getWorldContainer(), name);
                    plugin.getBedFightArenaManager().getSlimeAdapter().importWorld(worldFolder, name);
                    player.sendMessage(ChatColor.GREEN + "Arena '" + name + "' finished and converted to Slime!");
                    plugin.getBedFightArenaManager().loadArenas();
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error during Slime conversion: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 20L);
        }

        plugin.getBedFightArenaManager().removeSetupSession(player);
        return true;
    }

    private boolean handleStart(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /bedfight start <arena> <player1> <player2>");
            return true;
        }
        Arena arena = plugin.getBedFightArenaManager().getArena(args[1]);
        Player p1 = Bukkit.getPlayer(args[2]);
        Player p2 = Bukkit.getPlayer(args[3]);

        if (arena == null || p1 == null || p2 == null) {
            player.sendMessage(ChatColor.RED + "Invalid arena or players.");
            return true;
        }

        java.util.Set<UUID> redTeam = new java.util.HashSet<>(java.util.Collections.singletonList(p1.getUniqueId()));
        java.util.Set<UUID> blueTeam = new java.util.HashSet<>(java.util.Collections.singletonList(p2.getUniqueId()));

        plugin.getBedFightManager().startMatch(arena, QueueType.DUEL, redTeam, blueTeam);
        return true;
    }

    private void sendUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "--- BedFight Setup Commands ---");
        p.sendMessage(ChatColor.YELLOW + "/bedfight setup <worldName>");
        p.sendMessage(ChatColor.YELLOW + "/bedfight <redspawn|bluespawn|redbed|bluebed|center|pos1|pos2|voidlimit|buildlimit>");
        p.sendMessage(ChatColor.YELLOW + "/bedfight finish");
        p.sendMessage(ChatColor.YELLOW + "/bedfight start <arena> <p1> <p2>");
        p.sendMessage(ChatColor.YELLOW + "/bedfight reload");
    }
}
