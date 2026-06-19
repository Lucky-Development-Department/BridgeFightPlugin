package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetBalanceCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public SetBalanceCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bridgefight.balance.set")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set balances.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setbalance <player> <amount>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a whole number.");
            return true;
        }

        if (amount < 0) {
            sender.sendMessage(ChatColor.RED + "Amount cannot be negative.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getBalanceManager().setBalance(target.getUniqueId(), amount);
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s balance to " + amount + " coins.");

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(ChatColor.GOLD + "Your balance was set to " + ChatColor.YELLOW + amount + " coins.");
        }
        return true;
    }
}
