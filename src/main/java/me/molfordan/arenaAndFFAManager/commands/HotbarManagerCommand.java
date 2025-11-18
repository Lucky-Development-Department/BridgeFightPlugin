package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.HotbarSessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class HotbarManagerCommand implements CommandExecutor {

    private final HotbarSessionManager sessionManager;

    public HotbarManagerCommand(HotbarSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the hotbar manager.");
            return true;
        }
        Player p = (Player) sender;
        sessionManager.openSession(p);
        p.sendMessage(ChatColor.YELLOW + "Opened Hotbar Manager.");
        return true;
    }
}
