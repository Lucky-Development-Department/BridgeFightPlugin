package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpecCommand implements CommandExecutor {
    private final ArenaAndFFAManager plugin;

    public SpecCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /spec <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        BedFightSession session = plugin.getBedFightManager().getSession(target);
        if (session == null) {
            player.sendMessage(ChatColor.RED + "That player is not in a BedFight match!");
            return true;
        }

        if (plugin.getBedFightManager().isInMatch(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a match!");
            return true;
        }

        plugin.getBedFightManager().addSpectator(session, player);
        session.setPlayerState(player.getUniqueId(), BedFightState.SPECTATOR);
        return true;
    }
}
