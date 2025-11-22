package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.manager.TeleportPendingManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final TeleportPendingManager pending;

    public LobbyCommand(ConfigManager configManager, TeleportPendingManager pending){
        this.configManager = configManager;
        this.pending = pending;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) return true;

        String prefix = configManager.getServerPrefix();

        Player player = (Player) sender;

        Location lobbyWorld = configManager.getLobbyLocation();
        String worldName = configManager.getLobbyWorldName();

        if (lobbyWorld == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cFailed to send you to the Spawn, Please Contact Admins."));
            return true;
        }

        if (worldName == null) return true;

        if (player.getWorld().getName().equals(worldName)){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cYou are already connected to this server"));
            return true;
        }



        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " sending you to Spawn.... (Wait for 5 seconds)"));
        try {

            if (player.isOp()){
                player.teleport(lobbyWorld);
                return true;
            }

            pending.add(player);

            Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {

                // If cancelled due to movement
                if (!pending.isWaiting(player)) return;

                // Remove from pending
                pending.remove(player);

                // Teleport
                player.teleport(lobbyWorld);
                player.sendMessage(ChatColor.GREEN + "Teleported to Spawn!");

            }, 100);


        } catch (Exception e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cFailed to send you to the Spawn, Please Contact Admins."));
            return true;
        }
        return true;
    }
}
