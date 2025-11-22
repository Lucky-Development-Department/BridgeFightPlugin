package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;
    private final StatsManager statsManager;

    public SetCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender.hasPermission("arenamap.admin") || sender.isOp())) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        // /set <player> <bridgefight|buildffa> <stat> <value>
        if (args.length != 4) {
            sender.sendMessage("§eUsage: /set <player> <bridgefight|buildffa> <kills|deaths|streak|highest_streak> <value>");
            return true;
        }

        String targetName = args[0];
        String mode = args[1].toLowerCase();
        String stat = args[2].toLowerCase();
        String valueStr = args[3];

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer must be online.");
            return true;
        }

        ArenaType type;
        switch (mode) {
            case "bridgefight":
                type = ArenaType.FFA;
                break;
            case "buildffa":
                type = ArenaType.FFABUILD;
                break;
            default:
                sender.sendMessage("§cInvalid mode! Use bridgefight | buildffa");
                return true;
        }

        int value;
        try {
            value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cValue must be a number.");
            return true;
        }

        PlayerStats stats = statsManager.getOrLoad(target.getUniqueId(), target.getName());

        if (stats == null) {
            sender.sendMessage("§cStats could not be loaded.");
            return true;
        }

        // APPLY STAT
        switch (type) {
            case FFA:
                applyBridgeStats(stats, stat, value, sender);
                break;

            case FFABUILD:
                applyBuildStats(stats, stat, value, sender);
                break;
        }

        statsManager.savePlayerAsync(stats);
        sender.sendMessage("§aUpdated!");
        return true;
    }

    private void applyBridgeStats(PlayerStats s, String stat, int value, CommandSender sender) {
        switch (stat) {
            case "kills": s.setBridgeKills(value); break;
            case "deaths": s.setBridgeDeaths(value); break;
            case "streak": s.setBridgeStreak(value); break;
            case "highest_streak": s.setBridgeHighestStreak(value); break;
            default:
                sender.sendMessage("§cUnknown stat! Use kills/deaths/streak/highest_streak");
        }
    }

    private void applyBuildStats(PlayerStats s, String stat, int value, CommandSender sender) {
        switch (stat) {
            case "kills": s.setBuildKills(value); break;
            case "deaths": s.setBuildDeaths(value); break;
            case "streak": s.setBuildStreak(value); break;
            case "highest_streak": s.setBuildHighestStreak(value); break;
            default:
                sender.sendMessage("§cUnknown stat! Use kills/deaths/streak/highest_streak");
        }
    }
}
