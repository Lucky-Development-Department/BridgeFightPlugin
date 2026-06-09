package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.StatsManager;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import me.molfordan.bridgefightplugin.object.enums.ArenaType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;
    private final StatsManager statsManager;

    public SetCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender.hasPermission("arenamap.admin") || sender.isOp())) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        // /set <player> <bridgefight|buildffa|bedfight> <[ranked|unranked].stat|stat> <value>
        if (args.length < 4 || args.length > 5) {
            sender.sendMessage("§eUsage: /set <player> <bridgefight|buildffa|bedfight> <stat> <value>");
            return true;
        }

        String targetName = args[0];
        String mode = args[1].toLowerCase();
        
        // Handle bedfight stat parsing which might be split across args
        String stat;
        String valueStr;
        if (mode.equals("bedfight") && args.length == 5) {
            stat = args[2] + "." + args[3];
            valueStr = args[4];
        } else {
            stat = args[2].toLowerCase();
            valueStr = args[3];
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer must be online.");
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
        if (mode.equals("bedfight")) {
            applyBedFightStats(stats, stat, value, sender);
        } else {
            ArenaType type;
            switch (mode) {
                case "bridgefight":
                    type = ArenaType.FFA;
                    break;
                case "buildffa":
                    type = ArenaType.FFABUILD;
                    break;
                default:
                    sender.sendMessage("§cInvalid mode! Use bridgefight | buildffa | bedfight");
                    return true;
            }
            switch (type) {
                case FFA:
                    applyBridgeStats(stats, stat, value, sender);
                    break;
                case FFABUILD:
                    applyBuildStats(stats, stat, value, sender);
                    break;
            }
        }

        statsManager.savePlayerAsync(stats);
        sender.sendMessage("§aUpdated!");
        return true;
    }

    private void applyBedFightStats(PlayerStats s, String stat, int value, CommandSender sender) {
        String[] parts = stat.split("\\.");
        boolean isRanked = parts.length > 0 && parts[0].equalsIgnoreCase("ranked");
        String actualStat = parts.length > 1 ? parts[1] : stat;

        switch (actualStat) {
            case "kills":
                if (isRanked) s.setRankedKills(value);
                else s.setUnrankedKills(value);
                break;
            case "deaths":
                if (isRanked) s.setRankedDeaths(value);
                else s.setUnrankedDeaths(value);
                break;
            case "beds":
                if (isRanked) s.setRankedBeds(value);
                else s.setUnrankedBeds(value);
                break;
            case "elo":
                if (isRanked) s.setRankedElo(value);
                else sender.sendMessage("§cELO only applies to ranked stats.");
                break;
            default:
                sender.sendMessage("§cUnknown stat! Use [ranked|unranked].kills/deaths/beds/elo");
        }
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
