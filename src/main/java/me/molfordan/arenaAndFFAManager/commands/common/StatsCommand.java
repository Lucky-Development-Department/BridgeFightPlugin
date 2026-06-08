package me.molfordan.arenaAndFFAManager.commands.common;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    private final ArenaAndFFAManager plugin;

    public StatsCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        plugin.getStatsGUI().open(player);
        return true;
    }
}
