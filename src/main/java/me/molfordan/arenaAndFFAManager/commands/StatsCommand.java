package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;
    private final StatsManager statsManager;

    public StatsCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /stats → self
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must use: /stats <player>");
                return true;
            }

            Player p = (Player) sender;
            loadAndShowStats(sender, p.getUniqueId(), p.getName());
            return true;
        }

        // /stats <player>
        String name = args[0];

        // 1. Try online
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            loadAndShowStats(sender, online.getUniqueId(), online.getName());
            return true;
        }

        // 2. Try offline
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);

        if (offline == null || (!offline.hasPlayedBefore() && !offline.isOnline())) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        loadAndShowStats(sender, offline.getUniqueId(), offline.getName());
        return true;
    }


    // --------------------------------------------------------------------------------------
    // ASYNC STATS LOAD → SYNC MESSAGE SEND
    // --------------------------------------------------------------------------------------
    private void loadAndShowStats(final CommandSender sender, final UUID uuid, final String username) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {

                // Load from cache or database
                final PlayerStats stats = statsManager.getOrLoad(uuid, username);

                // Now output stats safely on main thread
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        sendStatsMessage(sender, stats);
                    }
                });
            }
        });
    }


    // --------------------------------------------------------------------------------------
    // SEND FORMATTED STATS
    // --------------------------------------------------------------------------------------
    private void sendStatsMessage(CommandSender sender, PlayerStats s) {

        sender.sendMessage("§8§m-------------------------------");
        sender.sendMessage("§6§lStats for §e" + s.getUsername());
        sender.sendMessage("§8§m-------------------------------");

        sender.sendMessage("§e§lBRIDGE FIGHT:");
        sender.sendMessage(" §fKills: §a" + s.getBridgeKills());
        sender.sendMessage(" §fDeaths: §a" + s.getBridgeDeaths());
        sender.sendMessage(" §fCurrent Streak: §a" + s.getBridgeStreak());
        sender.sendMessage(" §fHighest Streak: §6" + s.getBridgeHighestStreak());

        sender.sendMessage("");

        sender.sendMessage("§e§lBUILD FFA:");
        sender.sendMessage(" §fKills: §a" + s.getBuildKills());
        sender.sendMessage(" §fDeaths: §a" + s.getBuildDeaths());
        sender.sendMessage(" §fCurrent Streak: §a" + s.getBuildStreak());
        sender.sendMessage(" §fHighest Streak: §6" + s.getBuildHighestStreak());

        sender.sendMessage("§8§m-------------------------------");
    }
}
