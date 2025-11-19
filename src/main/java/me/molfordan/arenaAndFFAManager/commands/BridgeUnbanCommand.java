package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.BridgeFightBanManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.util.UUID;

public class BridgeUnbanCommand implements CommandExecutor {

    private final BridgeFightBanManager banManager;

    public BridgeUnbanCommand(BridgeFightBanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.isOp()) return true;

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /bridgeunban <player> [reason]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();

        if (!banManager.isPlayerBanned(uuid)) {
            sender.sendMessage("§cThat player is not banned from BridgeFight.");
            return true;
        }

        // reason
        String reason = "Player has been unbanned";
        if (args.length >= 2) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        }

        banManager.unban(uuid);

        sender.sendMessage("§aPlayer §e" + target.getName() + " §ahas been unbanned.");
        sender.sendMessage("§7Reason: §f" + reason);

        return true;
    }
}
