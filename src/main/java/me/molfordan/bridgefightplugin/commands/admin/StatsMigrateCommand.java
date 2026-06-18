package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.StatsManager;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StatsMigrateCommand implements CommandExecutor, TabCompleter {

    private final BridgeFightPlugin plugin;
    private final StatsManager statsManager;

    public StatsMigrateCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenamap.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /statsmigrate <oldplayername> <newplayername>");
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        OfflinePlayer oldPlayer = Bukkit.getOfflinePlayer(oldName);
        if (oldPlayer == null || !oldPlayer.hasPlayedBefore()) {
             // Fallback to manual check if hasPlayedBefore is unreliable (sometimes it is on older versions)
             boolean found = false;
             for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {
                 if (off.getName() != null && off.getName().equalsIgnoreCase(oldName)) {
                     oldPlayer = off;
                     found = true;
                     break;
                 }
             }
             if (!found) {
                 sender.sendMessage(ChatColor.RED + "Player '" + oldName + "' not found or has never played before.");
                 return true;
             }
        }

        OfflinePlayer newPlayer = Bukkit.getOfflinePlayer(newName);
        // We don't necessarily require newPlayer to have played before, but it's better if they have.
        // However, if we just want to migrate to a name that might join later, we can still do it if we can get a UUID.
        // But getOfflinePlayer(name) usually returns a profile even if they never joined, but it might not have a valid UUID for our needs.
        // In most cases, the player being migrated TO should have at least joined once to have a record.
        
        UUID oldUUID = oldPlayer.getUniqueId();
        UUID newUUID = newPlayer.getUniqueId();

        if (oldUUID.equals(newUUID)) {
            sender.sendMessage(ChatColor.RED + "Old and new players are the same.");
            return true;
        }

        PlayerStats oldStats = statsManager.loadPlayer(oldUUID, oldPlayer.getName());
        PlayerStats newStats = statsManager.loadPlayer(newUUID, newPlayer.getName());

        if (oldStats == null || newStats == null) {
            sender.sendMessage(ChatColor.RED + "Failed to load player stats.");
            return true;
        }

        // Migrate stats
        migrateStats(oldStats, newStats);

        // Reset old stats
        resetStats(oldStats);

        // Save both
        statsManager.savePlayer(oldStats);
        statsManager.savePlayer(newStats);

        sender.sendMessage(ChatColor.GREEN + "Successfully migrated stats from " + ChatColor.YELLOW + oldName + 
                ChatColor.GREEN + " to " + ChatColor.YELLOW + newName + ChatColor.GREEN + ".");
        sender.sendMessage(ChatColor.GRAY + "(Note: Old player stats have been reset)");

        return true;
    }

    private void migrateStats(PlayerStats from, PlayerStats to) {
        to.setBridgeKills(from.getBridgeKills());
        to.setBridgeDeaths(from.getBridgeDeaths());
        to.setBridgeStreak(from.getBridgeStreak());
        to.setBridgeHighestStreak(from.getBridgeHighestStreak());
        to.setBridgeDailyStreak(from.getBridgeDailyStreak());
        to.setBridgeDailyHighestStreak(from.getBridgeDailyHighestStreak());

        to.setBuildKills(from.getBuildKills());
        to.setBuildDeaths(from.getBuildDeaths());
        to.setBuildStreak(from.getBuildStreak());
        to.setBuildHighestStreak(from.getBuildHighestStreak());
        to.setBuildDailyStreak(from.getBuildDailyStreak());
        to.setBuildDailyHighestStreak(from.getBuildDailyHighestStreak());

        to.setRankedElo(from.getRankedElo());
        to.setPeakElo(from.getPeakElo());
        to.setRankedWins(from.getRankedWins());
        to.setRankedLosses(from.getRankedLosses());
        to.setRankedKills(from.getRankedKills());
        to.setRankedDeaths(from.getRankedDeaths());
        to.setRankedBeds(from.getRankedBeds());
        to.setRankedStreak(from.getRankedStreak());
        to.setRankedHighestStreak(from.getRankedHighestStreak());

        to.setUnrankedWins(from.getUnrankedWins());
        to.setUnrankedLosses(from.getUnrankedLosses());
        to.setUnrankedKills(from.getUnrankedKills());
        to.setUnrankedDeaths(from.getUnrankedDeaths());
        to.setUnrankedBeds(from.getUnrankedBeds());
        to.setUnrankedStreak(from.getUnrankedStreak());
        to.setBestUnrankedStreak(from.getBestUnrankedStreak());
        
        to.setDuelWins(from.getDuelWins());
        to.setDuelLosses(from.getDuelLosses());
        to.setBedFightBedBreaks(from.getBedFightBedBreaks());
        
        to.setLastSelectedBridgeKit(from.getLastSelectedBridgeKit());
    }

    private void resetStats(PlayerStats stats) {
        stats.setBridgeKills(0);
        stats.setBridgeDeaths(0);
        stats.setBridgeStreak(0);
        stats.setBridgeHighestStreak(0);
        stats.setBridgeDailyStreak(0);
        stats.setBridgeDailyHighestStreak(0);

        stats.setBuildKills(0);
        stats.setBuildDeaths(0);
        stats.setBuildStreak(0);
        stats.setBuildHighestStreak(0);
        stats.setBuildDailyStreak(0);
        stats.setBuildDailyHighestStreak(0);

        stats.setRankedElo(1000);
        stats.setPeakElo(1000);
        stats.setRankedWins(0);
        stats.setRankedLosses(0);
        stats.setRankedKills(0);
        stats.setRankedDeaths(0);
        stats.setRankedBeds(0);
        stats.setRankedStreak(0);
        stats.setRankedHighestStreak(0);

        stats.setUnrankedWins(0);
        stats.setUnrankedLosses(0);
        stats.setUnrankedKills(0);
        stats.setUnrankedDeaths(0);
        stats.setUnrankedBeds(0);
        stats.setUnrankedStreak(0);
        stats.setBestUnrankedStreak(0);
        
        stats.setDuelWins(0);
        stats.setDuelLosses(0);
        stats.setBedFightBedBreaks(0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 || args.length == 2) {
            String partialName = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
