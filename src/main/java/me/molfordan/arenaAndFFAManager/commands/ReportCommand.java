package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ReportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;
    // Map<ReporterUUID, Map<ReportedUUID, LastReportTime>>
    private final java.util.Map<java.util.UUID, java.util.Map<java.util.UUID, Long>> cooldowns = new java.util.HashMap<>();


    public ReportCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /report <player> <reason>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot report yourself.");
            return true;
        }

        // -----------------------------
        // Cooldown check (1 hour)
        // -----------------------------
        long now = System.currentTimeMillis();
        long cooldown = 3600_000L; // 1 hour

        java.util.UUID reporter = player.getUniqueId();
        java.util.UUID reported = target.getUniqueId();

        cooldowns.putIfAbsent(reporter, new java.util.HashMap<>());
        java.util.Map<java.util.UUID, Long> reportedMap = cooldowns.get(reporter);

        if (reportedMap.containsKey(reported)) {
            long lastTime = reportedMap.get(reported);

            if (now - lastTime < cooldown) {
                long remaining = cooldown - (now - lastTime);
                long minutes = remaining / 60000;

                player.sendMessage("§cYou have already reported this player recently.");
                player.sendMessage("§7Try again in §e" + minutes + " minutes§7.");
                return true;
            }
        }

        // Set new timestamp
        reportedMap.put(reported, now);

        // -----------------------------
        // Create report
        // -----------------------------
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        ReportManager rm = plugin.getReportManager();

        int id = rm.createReport(player.getUniqueId(), target.getUniqueId(), reason);

        player.sendMessage("§aReport submitted! (ID: " + id + ")");
        return true;
    }

}
