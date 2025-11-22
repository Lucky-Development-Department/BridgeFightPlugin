package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsResetCommand implements CommandExecutor, TabCompleter {

    private final ArenaAndFFAManager plugin;
    private final StatsManager statsManager;

    public StatsResetCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) return true;

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /statsreset <player>");
            return true;
        }

        OfflinePlayer target = null;

        for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {
            if (off.getName() != null && off.getName().equalsIgnoreCase(args[0])) {
                target = off;
                break;
            }
        }

        if (target == null) {
            sender.sendMessage("§cPlayer has never joined before.");
            return true;
        }

        UUID uuid = target.getUniqueId();

        // Load if not loaded
        PlayerStats stats = statsManager.getOrLoad(uuid, target.getName() == null ? "Unknown" : target.getName());

        if (stats == null) {
            sender.sendMessage("§cFailed to load player stats.");
            return true;
        }

        // Reset values
        stats.setBridgeKills(0);
        stats.setBridgeDeaths(0);
        stats.setBridgeStreak(0);
        stats.setBridgeHighestStreak(0);

        stats.setBuildKills(0);
        stats.setBuildDeaths(0);
        stats.setBuildStreak(0);
        stats.setBuildHighestStreak(0);

        // Save async
        statsManager.savePlayerAsync(stats);

        sender.sendMessage("§aSuccessfully reset stats for §e" + target.getName() + "§a.");
        return true;
    }

    // -------------------------------------------------------
    // TAB COMPLETION
    // -------------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
        }

        return list;
    }
}
