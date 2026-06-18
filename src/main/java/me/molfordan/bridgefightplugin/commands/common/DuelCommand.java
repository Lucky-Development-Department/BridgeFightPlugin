package me.molfordan.bridgefightplugin.commands.common;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelCommand implements CommandExecutor {
    private final BridgeFightPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public DuelCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // Cooldown check
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Please wait " + (timeLeft / 1000 + 1) + " seconds before using this command again.");
                return true;
            }
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 3000); // 3 second cooldown

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /duel <player> or /duel accept <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /duel accept <player>");
                return true;
            }
            Player senderPlayer = org.bukkit.Bukkit.getPlayer(args[1]);
            if (senderPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            plugin.getDuelManager().acceptRequest(player, senderPlayer);
            return true;
        }

        Player target = org.bukkit.Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(ChatColor.RED + "You cannot duel yourself!");
            return true;
        }

        plugin.getDuelManager().sendRequest(player, target);
        
        return true;
    }
}
