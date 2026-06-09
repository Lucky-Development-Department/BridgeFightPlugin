package me.molfordan.bridgefightplugin.commands.common.gui;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUILeaderboardCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public GUILeaderboardCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player p = (Player) sender;

        // Open main menu instead of forcing bridge kills
        plugin.getLeaderboardPlaceholderExpansion().updateLeaderboardCache();
        plugin.getGuiLeaderboardMain().open(p);

        return true;
    }
}
