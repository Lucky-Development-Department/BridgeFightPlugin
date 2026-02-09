    package me.molfordan.arenaAndFFAManager.commands.admin;

    import me.molfordan.arenaAndFFAManager.region.CommandRegion;
    import me.molfordan.arenaAndFFAManager.region.CommandRegionManager;
    import me.molfordan.arenaAndFFAManager.region.FlagType;
    import org.bukkit.Material;
    import org.bukkit.command.*;
    import org.bukkit.entity.Player;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.inventory.meta.ItemMeta;

    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;

    public class RegionCommand implements TabExecutor {

        private final CommandRegionManager manager;

        public RegionCommand(CommandRegionManager manager) {
            this.manager = manager;
        }

        // ========================================================================
        // COMMAND EXECUTION
        // ========================================================================
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!sender.isOp()) return true;

            Player p = (Player) sender;

            if (a.length == 0) {
                sendHelp(p);
                return true;
            }

            String sub = a[0].toLowerCase();

            switch (sub) {

                // ----------------------------------------------------------------
                case "pos1":
                    manager.setPos1(p, p.getLocation());
                    p.sendMessage("§aPos1 set.");
                    return true;

                case "pos2":
                    manager.setPos2(p, p.getLocation());
                    p.sendMessage("§aPos2 set.");
                    return true;

                // ----------------------------------------------------------------
                case "wand":
                    ItemStack axe = new ItemStack(Material.STONE_AXE);
                    ItemMeta meta = axe.getItemMeta();
                    meta.setDisplayName("§aRegion Wand");
                    axe.setItemMeta(meta);
                    p.getInventory().addItem(axe);
                    p.sendMessage("§aYou received a Region Wand.");
                    return true;

                // ======================================================================
                // CREATE REGION
                // ======================================================================
                case "create": {

                    if (a.length < 2) {
                        sendCreateHelp(p);
                        return true;
                    }

                    String name = a[1];

                    // require selection
                    if (!manager.hasSelection(p)) {
                        p.sendMessage("§cYou must set pos1 and pos2 first.");
                        return true;
                    }

                    // ---------- CREATE WITH FLAG: /rc create <name> flag <flag> <value>
                    if (a.length >= 4 && a[2].equalsIgnoreCase("flag")) {

                        if (a.length != 5) {
                            p.sendMessage("§cUsage: /rc create <name> flag <flag> <value>");
                            return true;
                        }

                        FlagType flag = FlagType.fromId(a[3]);
                        if (flag == null) {
                            p.sendMessage("§cUnknown flag: §e" + a[3]);
                            return true;
                        }

                        if (!manager.createEmptyRegion(name, p)) {
                            p.sendMessage("§cRegion already exists.");
                            return true;
                        }

                        CommandRegion region = manager.getRegion(name);

                        // ------------------------------------------------------------------
                        // DEFAULT EXECUTOR + COMMAND for FLAG creation
                        // ------------------------------------------------------------------
                        region.setExecutor(CommandRegion.Executor.NULL);
                        region.setCommand(""); // no command for flag-only creation

                        region.setFlag(flag, a[4]);

                        manager.saveRegionToConfig(name, region);

                        p.sendMessage("§aRegion created: §e" + name);
                        p.sendMessage("§7Flag set: §e" + flag.id() + " = " + a[4]);
                        return true;
                    }

                    // ---------- CREATE WITH COMMAND: /rc create <name> command <player|console> <command...>
                    if (a.length >= 4 && a[2].equalsIgnoreCase("command")) {

                        String execRaw = a[3].toLowerCase();
                        CommandRegion.Executor executor;

                        if (execRaw.equals("player")) executor = CommandRegion.Executor.PLAYER;
                        else if (execRaw.equals("console")) executor = CommandRegion.Executor.CONSOLE;
                        else {
                            p.sendMessage("§cInvalid executor. Must be: §eplayer§c or §econsole");
                            return true;
                        }

                        String commandToRun = String.join(" ", Arrays.copyOfRange(a, 4, a.length));

                        if (!manager.createEmptyRegion(name, p)) {
                            p.sendMessage("§cRegion already exists.");
                            return true;
                        }

                        CommandRegion region = manager.getRegion(name);
                        region.setExecutor(executor);
                        region.setCommand(commandToRun);

                        manager.saveRegionToConfig(name, region);

                        p.sendMessage("§aRegion created: §e" + name);
                        p.sendMessage("§7Command assigned: §e" + executor + " -> " + commandToRun);
                        return true;
                    }

                    // fallback
                    sendCreateHelp(p);
                    return true;
                }

                // ======================================================================
                // EDIT REGION FLAG
                // ======================================================================
                case "edit": {

                    if (a.length < 5 || !a[2].equalsIgnoreCase("flag")) {
                        p.sendMessage("§cUsage: /rc edit <name> flag <flag> <value>");
                        return true;
                    }

                    CommandRegion region = manager.getRegion(a[1]);
                    if (region == null) {
                        p.sendMessage("§cRegion not found: §e" + a[1]);
                        return true;
                    }

                    FlagType flag = FlagType.fromId(a[3]);
                    if (flag == null) {
                        p.sendMessage("§cUnknown flag: " + a[3]);
                        return true;
                    }

                    region.setFlag(flag, a[4]);
                    manager.saveRegionToConfig(a[1], region);

                    p.sendMessage("§aFlag updated: §e" + flag.id() + " = " + a[4]);
                    return true;
                }

                // ======================================================================
                // DELETE REGION
                // ======================================================================
                case "delete":
                    if (a.length != 2) {
                        p.sendMessage("§cUsage: /rc delete <name>");
                        return true;
                    }
                    if (manager.deleteRegion(a[1])) {
                        p.sendMessage("§aRegion deleted: §e" + a[1]);
                    } else {
                        p.sendMessage("§cRegion not found.");
                    }
                    return true;

                // ======================================================================
                // LIST REGIONS
                // ======================================================================
                case "list":
                    p.sendMessage("§aRegions:");
                    for (String n : manager.getRegionNames())
                        p.sendMessage(" §7- §f" + n);
                    return true;

                // ======================================================================
                // REGION INFO
                // ======================================================================
                case "info":
                    if (a.length != 2) {
                        p.sendMessage("§cUsage: /rc info <name>");
                        return true;
                    }

                    CommandRegion info = manager.getRegion(a[1]);
                    if (info == null) {
                        p.sendMessage("§cRegion not found: §e" + a[1]);
                        return true;
                    }

                    p.sendMessage("§aRegion Info: §e" + a[1]);
                    p.sendMessage(" §7World: §f" + info.getPos1().getWorld().getName());
                    p.sendMessage(" §7Pos1: §f" + info.getPos1().getBlockX() + "," + info.getPos1().getBlockY() + "," + info.getPos1().getBlockZ());
                    p.sendMessage(" §7Pos2: §f" + info.getPos2().getBlockX() + "," + info.getPos2().getBlockY() + "," + info.getPos2().getBlockZ());
                    p.sendMessage(" §7Executor: §f" + info.getExecutor());
                    p.sendMessage(" §7Command: §f" + info.getCommand());
                    p.sendMessage(" §7Flags:");

                    info.getFlags().forEach((type, val) ->
                            p.sendMessage("   §e" + type.id() + "§7: §f" + val));

                    return true;
                case "save":
                    if (a.length != 2) {
                        p.sendMessage("§cUsage: /rc save <name>");
                        return true;
                    }

                    String saveName = a[1];
                    CommandRegion saveRegion = manager.getRegion(saveName);

                    if (saveRegion == null) {
                        p.sendMessage("§cRegion not found: §e" + saveName);
                        return true;
                    }

                    manager.saveRegionToConfig(saveName, saveRegion);
                    p.sendMessage("§aRegion saved: §e" + saveName);
                    return true;

                // ======================================================================
                default:
                    sendHelp(p);
                    return true;
            }
        }

        // ========================================================================
        // HELP MESSAGES
        // ========================================================================
        private void sendHelp(Player p) {
            p.sendMessage("§e--- Region Commands ---");
            p.sendMessage("§e/rc pos1 §7- set pos1");
            p.sendMessage("§e/rc pos2 §7- set pos2");
            p.sendMessage("§e/rc wand §7- get wand");
            p.sendMessage("§e/rc create <name> flag <flag> <value>");
            p.sendMessage("§e/rc create <name> command <player|console> <command>");
            p.sendMessage("§e/rc edit <name> flag <flag> <value>");
            p.sendMessage("§e/rc delete <name>");
            p.sendMessage("§e/rc info <name>");
            p.sendMessage("§e/rc save <name>");
            p.sendMessage("§e/rc list");
            p.sendMessage("§e------------------------");
        }

        private void sendCreateHelp(Player p) {
            p.sendMessage("§cUsage:");
            p.sendMessage("§e/rc create <name> flag <flag> <value>");
            p.sendMessage("§e/rc create <name> command <player|console> <command...>");
        }

        // ========================================================================
        // TAB COMPLETION
        // ========================================================================
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

            List<String> out = new ArrayList<>();

            if (!(sender instanceof Player)) return out;
            if (!sender.isOp()) return out;

            // /rc <sub>
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                String[] subs = {"pos1","pos2","create","delete","list","info","edit","wand"};
                for (String s : subs)
                    if (s.startsWith(partial))
                        out.add(s);
                return out;
            }

            // /rc delete <name>
            // /rc info <name>
            // /rc edit <name>
            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("delete") || sub.equals("info") || sub.equals("edit") || sub.equals("create")) {
                    String partial = args[1].toLowerCase();
                    for (String name : manager.getRegionNames())
                        if (name.toLowerCase().startsWith(partial))
                            out.add(name);
                }
                return out;
            }

            // /rc create <name> (flag|command)
            if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
                out.add("flag");
                out.add("command");
                return out;
            }

            // /rc create <name> command <player|console>
            if (args.length == 4 && args[0].equalsIgnoreCase("create") && args[2].equalsIgnoreCase("command")) {
                out.add("player");
                out.add("console");
                return out;
            }

            // /rc create <name> flag <flag>
            if (args.length == 4 && args[0].equalsIgnoreCase("create") && args[2].equalsIgnoreCase("flag")) {
                for (FlagType f : FlagType.values())
                    out.add(f.id());
                return out;
            }

            // /rc edit <name> flag <flag>
            if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("flag")) {
                for (FlagType f : FlagType.values())
                    out.add(f.id());
                return out;
            }

            return out;
        }
    }
