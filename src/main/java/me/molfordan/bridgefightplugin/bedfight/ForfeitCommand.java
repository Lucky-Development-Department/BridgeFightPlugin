package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class ForfeitCommand implements CommandExecutor {
    private final BridgeFightPlugin plugin;

    public ForfeitCommand(BridgeFightPlugin plugin) {
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

        BedFightPlayerState state = session.getPlayerState(player.getUniqueId());
        if (state == BedFightPlayerState.ENDED) {
            player.sendMessage(ChatColor.RED + "The game has already ended! Use /leave.");
            return true;
        }

        // Active state forfeit logic
        String playerTeam = session.getTeam(player.getUniqueId());
        String opponentTeam = playerTeam.equals("RED") ? "BLUE" : "RED";
        Set<UUID> opponents = session.getPlayersByTeam(opponentTeam);
        
        Player opponent = null;
        for (UUID oppId : opponents) {
            opponent = Bukkit.getPlayer(oppId);
            if (opponent != null) break;
        }

        String msg = String.format(BedFightMessages.FORFEIT, player.getName());
        plugin.getBedFightListener().broadcastMessage(session, ChatColor.RED + msg);

        performSimpleLeave(player);

        // End match with opponent team as winner
        plugin.getBedFightManager().endMatch(session, opponentTeam);

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
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        
        plugin.getBedFightManager().removePlayerFromSession(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getBedFightManager().isInMatch(player)) return;

            if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                plugin.getMatchmakingService().giveLeaveItem(player);
                return;
            }

            if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
                plugin.getPartyManager().givePartyItems(player);
            } else {
                plugin.getSpawnItem().giveSpawnItem(player);
            }
        }, 1L);
    }

}
