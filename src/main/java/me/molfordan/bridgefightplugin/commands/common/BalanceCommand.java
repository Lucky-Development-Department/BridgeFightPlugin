package me.molfordan.bridgefightplugin.commands.common;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public BalanceCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        int balance = plugin.getBalanceManager().getBalance(player);
        player.sendMessage(ChatColor.GOLD + "Balance: " + ChatColor.YELLOW + balance + " coins");
        return true;
    }
}
