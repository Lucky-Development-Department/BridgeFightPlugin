package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitEditorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        BridgeFightPlugin.getPlugin().getBedFightHotbarSessionManager().openSession(player);
        return true;
    }
}
