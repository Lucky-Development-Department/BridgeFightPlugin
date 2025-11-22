package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.object.SerializableBlockState;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ArenaCommand implements CommandExecutor {
    private final ArenaManager manager;
    private final ConfigManager configManager;

    public ArenaCommand(ArenaManager manager, ConfigManager configManager) {
        this.manager = manager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.isOp()) return true;

        boolean isConsole = !(sender instanceof Player);

        // --- Allow console ONLY for /arenamap <arena> restore ---
        if (isConsole) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("restore")) {
                String arenaName = args[0];
                Arena arena = manager.getArenaByName(arenaName);
                if (arena == null) {
                    sender.sendMessage(ChatColor.RED + "Arena not found: " + arenaName);
                    return true;
                }

                int restoredCount = 0;
                for (String worldName : new String[]{"BuildFFA", "Arenas_1", "Arenas_2"}) {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;
                    for (Map.Entry<String, SerializableBlockState> e : arena.getOriginalBlocksMap().entrySet()) {
                        Location loc = stringToLocation(world, e.getKey());
                        if (loc != null) {
                            e.getValue().apply(loc.getBlock());
                            restoredCount++;
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Restored " + restoredCount +
                        " blocks for arena '" + arenaName + "' in all arena worlds.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Only the 'restore' subcommand can be used from console.");
            return true;
        }

        // --- Player-only commands below ---
        Player player = (Player) sender;

        if (!player.hasPermission("arenamap.admin") || !player.isOp()) return true;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String arg0 = args[0].toLowerCase();

        if (arg0.equals("list")) {
            listArenas(player);
            return true;
        }

        if (arg0.equals("reload")) {
            manager.loadArenas();
            configManager.reloadConfig();
            configManager.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Reloaded all arenas.");
            return true;
        }

        if (arg0.equals("save")) {
            manager.saveAllArenas();
            player.sendMessage(ChatColor.GREEN + "Saved all arenas.");
            return true;
        }

        if (arg0.equals("info")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("world")) {
                World world = player.getWorld();
                player.sendMessage(ChatColor.GOLD + "--- World Info ---");
                player.sendMessage(ChatColor.YELLOW + "World Name: " + ChatColor.WHITE + world.getName());
                player.sendMessage(ChatColor.YELLOW + "Environment: " + ChatColor.WHITE + world.getEnvironment().name());
                player.sendMessage(ChatColor.YELLOW + "World UUID: " + ChatColor.WHITE + world.getUID());
                return true;
            }

            Arena currentArena = manager.getArenaByLocation(player.getLocation());
            if (currentArena == null) {
                player.sendMessage(ChatColor.RED + "You are not currently standing in any arena.");
            } else {
                sendArenaInfo(player, currentArena);
            }
            return true;
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String arenaName = args[0];
        String sub = args[1].toLowerCase();

        if (sub.equals("create")) return handleCreate(player, arenaName);

        Arena arena = manager.getArenaByName(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena not found: " + arenaName);
            return true;
        }

        switch (sub) {
            case "pos1":
                arena.setPos1(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set Pos1 at your current location.");
                break;
            case "pos2":
                arena.setPos2(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set Pos2 at your current location.");
                break;
            case "center":
                arena.setCenter(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set center at your current location.");
                break;
            case "teleport":
                if (arena.getCenter() == null) {
                    player.sendMessage(ChatColor.RED + "Center is not set.");
                    return true;
                }
                player.teleport(arena.getCenter());
                player.sendMessage(ChatColor.GREEN + "Teleported to arena center.");
                return true;
            case "capture":
                if (arena.getType() != ArenaType.FFABUILD) {
                    player.sendMessage(ChatColor.RED + "This arena is not of type FFABUILD.");
                    return true;
                }
                if (arena.getPos1() == null || arena.getPos2() == null) {
                    player.sendMessage(ChatColor.RED + "Both Pos1 and Pos2 must be set before capturing.");
                    return true;
                }
                arena.captureCurrentState();
                manager.saveArena(arena);
                player.sendMessage(ChatColor.GREEN + "Captured arena blocks inside the defined region.");
                break;
            case "restore":
                int restoredCount = 0;
                for (String worldName : new String[]{"BuildFFA", "Arenas_1", "Arenas_2"}) {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;
                    for (Map.Entry<String, SerializableBlockState> e : arena.getOriginalBlocksMap().entrySet()) {
                        Location loc = stringToLocation(world, e.getKey());
                        if (loc != null) {
                            e.getValue().apply(loc.getBlock());
                            restoredCount++;
                        }
                    }
                }
                player.sendMessage(ChatColor.GREEN + "Restored " + restoredCount +
                        " blocks for arena '" + arena.getName() + "' in all arena worlds.");
                return true;
            case "remove":
                if (manager.removeArena(arenaName)) {
                    player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' removed successfully.");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to remove arena '" + arenaName + "'.");
                }
                return true;
            case "info":
                sendArenaInfo(player, arena);
                return true;
            case "finish":
                return handleFinish(player, arena);
            case "type":
                return setArenaType(player, arena, args);
            case "buildlimit":
                return setBuildLimit(player, arena, args);
            case "voidlimit":
                return setVoidLimit(player, arena, args);
            case "clone":
                String[] worlds = {"BuildFFA", "Arenas_1", "Arenas_2"};
                for (int i = 0; i < worlds.length; i++) {
                    World w = Bukkit.getWorld(worlds[i]);
                    if (w == null) continue;
                    String newName = arena.getName() + "_" + i;
                    Arena clone = manager.cloneArena(arena, newName, w);
                    if (clone != null) {
                        clone.setFinished(true);
                        manager.saveArena(clone);
                        player.sendMessage(ChatColor.GREEN + "Cloned arena as '" + newName + "' in world " + w.getName());
                    }
                }
                return true;
        }

        manager.saveArena(arena);
        player.sendMessage(ChatColor.GREEN + "Arena '" + arena.getName() + "' updated.");
        return true;
    }

    private boolean handleCreate(Player player, String name) {
        if (manager.createArena(name, player) != null) {
            player.sendMessage(ChatColor.GREEN + "Created arena '" + name + "'. Now set pos1, pos2, center and type.");
        } else {
            player.sendMessage(ChatColor.RED + "An arena with that name already exists.");
        }
        return true;
    }

    private boolean handleFinish(Player player, Arena arena) {
        if (arena.getPos1() == null || arena.getPos2() == null || arena.getCenter() == null || arena.getType() == null) {
            player.sendMessage(ChatColor.RED + "Arena is incomplete. Please set pos1, pos2, center, and type.");
            return true;
        }
        if (arena.getType() == ArenaType.FFABUILD && arena.getOriginalBlocksMap().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No blocks captured for FFABUILD arena. Use /arenamap <arena> capture first.");
            return true;
        }
        arena.setFinished(true);
        player.sendMessage(ChatColor.GREEN + "Marked arena as finished.");
        return true;
    }

    private boolean setArenaType(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arenamap <arena> type <ffa|ffabuild|duel>");
            return false;
        }
        ArenaType type = ArenaType.fromString(args[2]);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Invalid type. Use ffa, ffabuild, or duel.");
            return false;
        }
        arena.setType(type);
        player.sendMessage(ChatColor.GREEN + "Arena type set to " + type.name());
        return true;
    }

    private boolean setBuildLimit(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arenamap <arena> buildlimit <Y>");
            return false;
        }
        try {
            int y = Integer.parseInt(args[2]);
            arena.setBuildLimitY(y);
            player.sendMessage(ChatColor.GREEN + "Build limit Y set to " + y);
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid Y value.");
            return false;
        }
    }

    private boolean setVoidLimit(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arenamap <arena> voidlimit <Y>");
            return false;
        }
        try {
            int y = Integer.parseInt(args[2]);
            arena.setVoidLimit(y);
            player.sendMessage(ChatColor.GREEN + "Void limit Y set to " + y);
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid Y value.");
            return false;
        }
    }

    private void sendArenaInfo(Player player, Arena arena) {
        player.sendMessage(ChatColor.GOLD + "--- Arena Info: " + arena.getName() + " ---");
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + arena.getType());
        player.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.WHITE + arena.getWorldName());
        player.sendMessage(ChatColor.YELLOW + "Pos1: " + ChatColor.WHITE + formatLocation(arena.getPos1()));
        player.sendMessage(ChatColor.YELLOW + "Pos2: " + ChatColor.WHITE + formatLocation(arena.getPos2()));
        player.sendMessage(ChatColor.YELLOW + "Center: " + ChatColor.WHITE + formatLocation(arena.getCenter()));
        player.sendMessage(ChatColor.YELLOW + "Build Limit Y: " + ChatColor.WHITE + arena.getBuildLimitY());
        player.sendMessage(ChatColor.YELLOW + "Void Limit Y: " + ChatColor.WHITE + arena.getVoidLimit());
        player.sendMessage(ChatColor.YELLOW + "Finished: " + (arena.isFinished() ? "Yes" : "No"));
        if (arena.getType() == ArenaType.FFABUILD) {
            player.sendMessage(ChatColor.YELLOW + "Captured Blocks: " + arena.getOriginalBlocksMap().size());
        }
    }

    private String formatLocation(Location loc) {
        return (loc == null ? "Not set" : "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
    }

    private Location stringToLocation(World world, String s) {
        String[] split = s.split(",");
        if (split.length != 3) return null;
        try {
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "--- Arena Commands ---");
        p.sendMessage(ChatColor.YELLOW + "/arenamap <name> create|pos1|pos2|center|type|buildlimit|voidlimit|capture|restore|finish|clone|remove|info");
        p.sendMessage(ChatColor.YELLOW + "/arenamap list|reload|save|info world");
    }

    private void listArenas(Player player) {
        Collection<Arena> arenas = manager.getAllArenas();
        if (arenas.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No arenas found.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "--- Available Arenas ---");
        for (Arena arena : arenas) {
            String status = arena.isFinished()
                    ? ChatColor.GREEN + " (Ready)"
                    : ChatColor.RED + " (Incomplete)";
            player.sendMessage(ChatColor.YELLOW + "- " + arena.getName() +
                    ChatColor.GRAY + " (Type: " +
                    (arena.getType() != null ? arena.getType().name() : "N/A") + ")" + status);
        }
        player.sendMessage(ChatColor.GOLD + "-------------------------");
    }
}
