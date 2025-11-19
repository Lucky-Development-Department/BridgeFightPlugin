package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.BridgeFightBanManager;
import me.molfordan.arenaAndFFAManager.utils.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BridgeBanCommand implements CommandExecutor {

    private final BridgeFightBanManager banManager;

    public BridgeBanCommand(BridgeFightBanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.isOp()) return true;

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /bridgeban <player> [duration] [reason]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();

        if (banManager.isPlayerBanned(uuid)) {
            sender.sendMessage("§cThat player is already banned from BridgeFight.");
            return true;
        }

        // duration
        long duration = -1;
        if (args.length >= 2) {
            duration = DurationParser.parseDuration(args[1]);
            if (duration == -1) {
                sender.sendMessage("§cInvalid duration format!");
                return true;
            }
        }

        // reason
        String reason = "BridgeFight rules violation";
        if (args.length >= 3) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        }

        banManager.ban(uuid, duration, reason);

        sender.sendMessage("§aPlayer §e" + target.getName() + " §ahas been banned from BridgeFight.");
        sender.sendMessage("§7Duration: §f" + (duration == -1 ? "Permanent" : args[1]));
        sender.sendMessage("§7Reason: §f" + reason);

        // If player is online, kick them from BridgeFight arena
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage("§cYou have been banned from BridgeFight.\n§7Reason: §f" + reason);
        }

        return true;
    }
}
