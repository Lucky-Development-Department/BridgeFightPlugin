package me.molfordan.bridgefightplugin.commands.common;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    private final BridgeFightPlugin plugin;

    public StatsCommand(BridgeFightPlugin plugin) {
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
