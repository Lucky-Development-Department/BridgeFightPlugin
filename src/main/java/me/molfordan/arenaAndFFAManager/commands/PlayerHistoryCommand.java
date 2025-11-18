package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

import java.util.UUID;

public class PlayerHistoryCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public PlayerHistoryCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!sender.hasPermission("bridgefight.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: §e/playerhistory <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        UUID uuid = target.getUniqueId();

        int count = plugin.getBridgeFightBanManager().getBanHistory(uuid);

        sender.sendMessage("§e" + target.getName() + " §ahas been banned from BridgeFight §c" + count + "x§a.");

        return true;
    }
}

