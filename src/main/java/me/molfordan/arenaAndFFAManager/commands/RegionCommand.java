package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.region.CommandRegion;
import me.molfordan.arenaAndFFAManager.region.CommandRegionManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RegionCommand implements TabExecutor {

    private final CommandRegionManager manager;

    public RegionCommand(CommandRegionManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player p = (Player) sender;

        if (a.length == 0) {
            sendHelp(p);
            return true;
        }

        String sub = a[0].toLowerCase();
        switch (sub) {

            case "pos1":
                manager.setPos1(p, p.getLocation());
                return true;

            case "pos2":
                manager.setPos2(p, p.getLocation());
                return true;

            case "create":
                if (a.length < 3) {
                    p.sendMessage("§cUsage: /rc create <name> [console|player] <command...>");
                    return true;
                }
                String name = a[1];

                int cmdStart = 2;
                CommandRegion.Executor exec = CommandRegion.Executor.CONSOLE;

                String maybe = a[2].toLowerCase();
                if (maybe.equals("console") || maybe.equals("player")) {
                    exec = CommandRegion.Executor.valueOf(maybe.toUpperCase());
                    cmdStart = 3;
                    if (a.length < 4) {
                        p.sendMessage("§cUsage: /rc create <name> [console|player] <command...>");
                        return true;
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (int i = cmdStart; i < a.length; i++) {
                    sb.append(a[i]).append(" ");
                }
                String command = sb.toString().trim();

                if (manager.createRegionAndSave(name, p, command, exec)) {
                    p.sendMessage("§aRegion created: §e" + name);
                    p.sendMessage("§7Cmd: §f" + command + " §7(exec: §e" + exec.name() + "§7)");
                } else {
                    p.sendMessage("§cYou must set pos1 and pos2 first (use /rc pos1 and /rc pos2).");
                }
                return true;

            case "delete":
                if (a.length != 2) {
                    p.sendMessage("§cUsage: /rc delete <name>");
                    return true;
                }
                if (manager.deleteRegion(a[1])) {
                    p.sendMessage("§aRegion deleted: §e" + a[1]);
                } else {
                    p.sendMessage("§cRegion not found: §e" + a[1]);
                }
                return true;

            case "list":
                if (manager.getRegionNames().isEmpty()) {
                    p.sendMessage("§cNo regions.");
                    return true;
                }
                p.sendMessage("§aRegions:");
                for (String n : manager.getRegionNames()) {
                    p.sendMessage(" §7- §f" + n);
                }
                return true;

            case "info":
                if (a.length < 2) {
                    p.sendMessage("§cUsage: /rc info <name>");
                    return true;
                }
                CommandRegion region = manager.getRegion(a[1]);
                if (region == null) {
                    p.sendMessage("§cRegion not found: §e" + a[1]);
                    return true;
                }
                p.sendMessage("§aRegion Info: §e" + a[1]);
                p.sendMessage(" §7World: §f" + region.getPos1().getWorld().getName());
                p.sendMessage(" §7Pos1: §f" + region.getPos1().getBlockX() + "," + region.getPos1().getBlockY() + "," + region.getPos1().getBlockZ());
                p.sendMessage(" §7Pos2: §f" + region.getPos2().getBlockX() + "," + region.getPos2().getBlockY() + "," + region.getPos2().getBlockZ());
                p.sendMessage(" §7Command: §f" + region.getCommand());
                p.sendMessage(" §7Executor: §f" + region.getExecutor().name());
                return true;

            default:
                sendHelp(p);
                return true;
        }
    }

    private void sendHelp(Player p) {
        p.sendMessage("§e/rc pos1 §7- set selection pos1");
        p.sendMessage("§e/rc pos2 §7- set selection pos2");
        p.sendMessage("§e/rc create <name> [console|player] <command...> §7- create and save");
        p.sendMessage("§e/rc delete <name> §7- delete region");
        p.sendMessage("§e/rc list §7- list regions");
        p.sendMessage("§e/rc info <name> §7- show region info");
    }

    // --------------------------
    // TAB COMPLETION
    // --------------------------


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player)) return out;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            // Java 8 compatible: no List.of
            String[] subs = { "pos1", "pos2", "create", "delete", "list", "info" };

            for (String s : subs) {
                if (s.startsWith(partial)) out.add(s);
            }
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("delete") || sub.equals("info")) {
                String partial = args[1].toLowerCase();

                // Java 8 compatible filtering (no streams)
                for (String n : manager.getRegionNames()) {
                    if (n.toLowerCase().startsWith(partial)) {
                        out.add(n);
                    }
                }
            }
            return out;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            out.add("console");
            out.add("player");
            return out;
        }

        return out;
    }

}
