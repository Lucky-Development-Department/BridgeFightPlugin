package me.molfordan.arenaAndFFAManager.commands.common.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUICustomKit implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public GUICustomKit(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!sender.isOp()) return true;

        if (!(sender instanceof Player)) return true;


        plugin.getCustomKitBaseGUI().open((Player) sender);
        sender.sendMessage("§aOpened Custom Kit GUI");

        return true;
    }
}
