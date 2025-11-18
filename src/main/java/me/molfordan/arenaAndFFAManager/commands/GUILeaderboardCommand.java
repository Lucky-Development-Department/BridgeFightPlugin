package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboardMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUILeaderboardCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public GUILeaderboardCommand(ArenaAndFFAManager plugin) {
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
