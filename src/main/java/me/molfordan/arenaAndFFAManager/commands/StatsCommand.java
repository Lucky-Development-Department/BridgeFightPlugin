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
    private final GUIStatsCommand guiStats;

    public StatsCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
        this.guiStats = new GUIStatsCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /stats → self TEXT
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must use: /stats player <player>");
                return true;
            }
            Player p = (Player) sender;
            loadAndShowStats(sender, p.getUniqueId(), p.getName());
            return true;
        }

        String mode = args[0].toLowerCase();

        if (!mode.equals("gui") && !mode.equals("player")) {
            sender.sendMessage("§cUsage: /stats [gui|player] [player]");
            return true;
        }

        // Determine target player
        OfflinePlayer target;

        if (args.length == 1) {
            // Self
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player.");
                return true;
            }
            target = (Player) sender;

        } else {
            // args[1] = target
            target = Bukkit.getOfflinePlayer(args[1]);

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
        }

        UUID uuid = target.getUniqueId();
        String name = target.getName();

        // TEXT MODE
        if (mode.equals("player")) {
            loadAndShowStats(sender, uuid, name);
            return true;
        }

        // GUI MODE
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cConsole cannot open GUIs.");
            return true;
        }

        Player viewer = (Player) sender;
        guiStats.openGUIAsync(viewer, uuid, name);
        return true;
    }


    // -------------------------------------------------------------
    // TEXT OUTPUT (ASYNC LOAD)
    // -------------------------------------------------------------
    private void loadAndShowStats(CommandSender sender, UUID uuid, String username) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            PlayerStats stats = statsManager.getOrLoad(uuid, username);

            Bukkit.getScheduler().runTask(plugin, () ->
                    sendStatsMessage(sender, stats)
            );
        });
    }


    // -------------------------------------------------------------
    // TEXT MESSAGE
    // -------------------------------------------------------------
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
