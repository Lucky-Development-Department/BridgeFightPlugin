package me.molfordan.bridgefightplugin.commands.common.gui;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUICustomKit implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public GUICustomKit(BridgeFightPlugin plugin) {
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
