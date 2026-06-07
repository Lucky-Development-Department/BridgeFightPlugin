package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LeaveCommand implements CommandExecutor {
    private final ArenaAndFFAManager plugin;

    public LeaveCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not in a BedFight session!");
            return true;
        }

        BedFightState state = session.getPlayerState(player.getUniqueId());
        if (state == BedFightState.ENDED || state == BedFightState.SPECTATOR) {
            // Simple exit
            performLeave(player);
        } else {
            // Treat as forfeit if leaving during active play
            UUID opponentUUID = session.getRedPlayer().equals(player.getUniqueId()) ? session.getBluePlayer() : session.getRedPlayer();
            Player opponent = org.bukkit.Bukkit.getPlayer(opponentUUID);

            String msg = String.format(BedFightMessages.FORFEIT, player.getName());
            plugin.getBedFightListener().broadcastMessage(session, ChatColor.RED + msg);
            
            performLeave(player);
            plugin.getBedFightManager().endMatch(session, opponent);
        }

        return true;
    }

    private void performLeave(Player player) {
        org.bukkit.Location lobby = plugin.getConfigManager().getLobbyLocation();
        player.teleport(lobby);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        plugin.getSpawnItem().giveSpawnItem(player);
        plugin.getBedFightManager().removePlayerFromSession(player);
    }
}
