package me.molfordan.arenaAndFFAManager.commands.bridgefight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit2;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BridgeFightKitCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public BridgeFightKitCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        // ------------------------------
        // /kit
        // ------------------------------
        if (args.length == 0) {
            plugin.getBridgeFightGUI().open(player);
            return true;
        }

        // ------------------------------
        // /kit select <name>
        // ------------------------------
        if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            String kitName = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(kitName);

            if (kit == null) {
                player.sendMessage("§cNo kit found with that name.");
                return true;
            }

            plugin.getKitManager().setSelectedBridgeFightKit(player.getUniqueId(), kit.getName());
            player.sendMessage("§aYou selected: §f" + kit.getDisplayName());
            return true;
        }

        // ------------------------------
        // Admin Section
        // ------------------------------
        if (!player.hasPermission("arenamap.kit.admin")) {
            player.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reloadall")) {
            if (!sender.hasPermission("arena.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }

            plugin.getBridgeFightKitManager().reloadAllKits();
            sender.sendMessage("§aAll kits reloaded from configuration.");
            return true;
        }

        // ------------------------------
        // /kit create <name>
        // ------------------------------
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            String name = args[1];

            if (plugin.getBridgeFightKitManager().get(name) != null) {
                player.sendMessage("§cA kit with this name already exists.");
                return true;
            }

            plugin.getBridgeFightKitManager().createEmptyKit(name);
            player.sendMessage("§aCreated kit: §f" + name);
            return true;
        }

        // ------------------------------
        // /kit setinv <name>
        // ------------------------------
        if (args.length == 2 && args[0].equalsIgnoreCase("setinv")) {
            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cUnknown kit.");
                return true;
            }

            kit.loadFromPlayerInventory(player);
            plugin.getBridgeFightKitManager().saveKit(kit);
            player.sendMessage("§aInventory saved.");
            return true;
        }

        // ------------------------------
        // /kit getinv <name>
        // ------------------------------
        if (args.length == 2 && args[0].equalsIgnoreCase("getinv")) {
            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cUnknown kit.");
                return true;
            }

            kit.applyToPlayer(player);
            player.sendMessage("§aYou received this kit.");
            return true;
        }

        // ------------------------------
        // /kit setdisplayname <name> <display name...>
        // ------------------------------
        if (args.length >= 3 && args[0].equalsIgnoreCase("setdisplayname")) {
            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cUnknown kit.");
                return true;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) sb.append(args[i]).append(" ");
            String display = sb.toString().trim();

            kit.setDisplayName(display);
            plugin.getBridgeFightKitManager().saveKit(kit);

            player.sendMessage("§aDisplay name updated.");
            return true;
        }

        // ------------------------------
        // /kit edit <name>
        // ------------------------------
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cUnknown kit.");
                return true;
            }

            plugin.getBridgeFightGUI().openEdit(player, kit);
            return true;
        }

        // ---------------------------------------------------
        // NEW COMMANDS BELOW
        // ---------------------------------------------------

        // ------------------------------
        // /kit save <name>
        // ------------------------------
        if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cUnknown kit.");
                return true;
            }

            plugin.getBridgeFightKitManager().saveKit(kit);
            player.sendMessage("§aKit saved.");
            return true;
        }

        // ------------------------------
        // /kit saveall
        // ------------------------------
        if (args.length == 1 && args[0].equalsIgnoreCase("saveall")) {
            for (Kit2 kit : plugin.getBridgeFightKitManager().getAllKits()) {
                plugin.getBridgeFightKitManager().saveKit(kit);
            }
            player.sendMessage("§aAll kits saved.");
            return true;
        }

        // ------------------------------
        // /kit requiredkills <name> <value>
        // ------------------------------
        if (args.length == 3 && args[0].equalsIgnoreCase("requiredkills")) {
            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cUnknown kit.");
                return true;
            }

            try {
                int val = Integer.parseInt(args[2]);
                kit.setRequiredKills(val);
                plugin.getBridgeFightKitManager().saveKit(kit);
                player.sendMessage("§aRequired kills updated to: §f" + val);
            } catch (NumberFormatException ex) {
                player.sendMessage("§cInvalid number.");
            }

            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("sort")) {

            String name = args[1];
            Kit2 kit = plugin.getBridgeFightKitManager().get(name);

            if (kit == null) {
                player.sendMessage("§cKit not found.");
                return true;
            }

            int sort;
            try {
                sort = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                player.sendMessage("§cSort value must be a number.");
                return true;
            }

            kit.setSort(sort);
            plugin.getBridgeFightKitManager().saveKit(kit);

            player.sendMessage("§aSort order updated for §f" + kit.getName() +
                    "§a → §e" + sort);
            return true;
        }

        player.sendMessage("§cInvalid usage.");
        return true;
    }
}
