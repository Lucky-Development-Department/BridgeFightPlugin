package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ReportManager;
import me.molfordan.arenaAndFFAManager.manager.ReportManager.ReportData;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportsCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public ReportsCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {

        if (!sender.isOp()) return true;

        ReportManager rm = plugin.getReportManager();

        // -------------------------
        // /reports clear
        // -------------------------
        if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
            rm.clearAllReports(); // <-- You need to implement this if not yet created
            sender.sendMessage("§aAll reports have been cleared.");
            return true;
        }

        // -------------------------
        // Page argument
        // -------------------------
        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (Exception ignored) {}
        }

        Map<Integer, ReportData> all = rm.getAllReports();

        if (all.isEmpty()) {
            sender.sendMessage("§cNo reports found.");
            return true;
        }

        List<ReportData> list = new ArrayList<>(all.values());
        int perPage = 10;
        int maxPage = (int) Math.ceil(list.size() / (double) perPage);

        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        sender.sendMessage("§e--- Reports (Page " + page + "/" + maxPage + ") ---");

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, list.size());

        for (int i = start; i < end; i++) {
            ReportData r = list.get(i);
            OfflinePlayer reporter = Bukkit.getOfflinePlayer(r.reporter);
            OfflinePlayer reported = Bukkit.getOfflinePlayer(r.reported);

            sender.sendMessage("§f#" + r.id + " §7" +
                    reporter.getName() + " §f→ §c" + reported.getName() +
                    " §7Reason: §f" + r.reason);
        }

        // Pagination buttons
        TextComponent line = new TextComponent("");

        if (page > 1) {
            TextComponent prev = new TextComponent("§a[Previous]");
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Go to previous page").create()));
            line.addExtra(prev);
        }

        line.addExtra(" ");

        if (page < maxPage) {
            TextComponent next = new TextComponent("§a[Next]");
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Go to next page").create()));
            line.addExtra(next);
        }

        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(line);
        } else {
            sender.sendMessage("§7(Interactive buttons unavailable from console)");
        }

        return true;
    }
}
