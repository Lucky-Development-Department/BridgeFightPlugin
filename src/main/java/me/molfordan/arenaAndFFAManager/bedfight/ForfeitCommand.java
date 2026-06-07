package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ForfeitCommand implements CommandExecutor {
    private final ArenaAndFFAManager plugin;

    public ForfeitCommand(ArenaAndFFAManager plugin) {
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
        if (state == BedFightState.ENDED) {
            player.sendMessage(ChatColor.RED + "The game has already ended! Use /leave.");
            return true;
        }

        // Active state forfeit logic
        UUID opponentUUID = session.getRedPlayer().equals(player.getUniqueId()) ? session.getBluePlayer() : session.getRedPlayer();
        Player opponent = Bukkit.getPlayer(opponentUUID);

        String msg = String.format(BedFightMessages.FORFEIT, player.getName());
        plugin.getBedFightListener().broadcastMessage(session, ChatColor.RED + msg);

        performSimpleLeave(player);

        // End match with opponent as winner
        plugin.getBedFightManager().endMatch(session, opponent);

        return true;
    }

    private void performSimpleLeave(Player player) {
        Location lobby = plugin.getConfigManager().getLobbyLocation();
        player.teleport(lobby);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        plugin.getSpawnItem().giveSpawnItem(player);
        plugin.getBedFightManager().removePlayerFromSession(player);
    }

}
