package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class LeaveCommand implements CommandExecutor {
    private final BridgeFightPlugin plugin;

    public LeaveCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // 1. Check if in queue
        boolean inQueue = plugin.getMatchmakingService().isInWaitingQueue(uuid);
        plugin.getLogger().info("DEBUG: LeaveCommand check inQueue=" + inQueue + " for " + player.getName());
        
        if (inQueue) {
            plugin.getLogger().info("DEBUG: LeaveCommand calling removeFromQueue for " + player.getName());
            plugin.getMatchmakingService().removeFromQueue(player);
            plugin.getLogger().info("DEBUG: LeaveCommand removeFromQueue called for " + player.getName());
            // MatchmakingService already sends "Left the queue!"
            return true;
        }

        // 2. Check if in a session (participant or spectator)
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        plugin.getLogger().info("DEBUG: LeaveCommand session=" + (session != null) + " for " + player.getName());
        if (session != null) {
            BedFightPlayerState state = session.getPlayerState(uuid);
            
            if (state == BedFightPlayerState.ENDED) {
                // Match finished, just leaving
                performLeave(player);
                player.sendMessage(ChatColor.YELLOW + "You have left the match.");
            } else if (session.isSpectator(uuid)) {
                // Joined via /spec OR eliminated during match
                performLeave(player);
                player.sendMessage(ChatColor.YELLOW + "You are no longer spectating the match.");
            } else {
                // Active participant leaving - treated as forfeit
                String playerTeam = session.getTeam(uuid);
                String opponentTeam = "RED".equalsIgnoreCase(playerTeam) ? "BLUE" : "RED";

                String msg = String.format(BedFightMessages.FORFEIT, player.getName());
                plugin.getBedFightListener().broadcastMessage(session, ChatColor.RED + msg);
                
                performLeave(player);
                player.sendMessage(ChatColor.RED + "You have forfeited the match.");
                session.setForfeit(true);
                if (session.getSessionState() == BedFightSessionState.COUNTDOWN) {
                    session.setForfeitDuringCountdown(true);
                }
                plugin.getBedFightManager().endMatch(session, opponentTeam, true);
            }
            return true;
        }

        // 3. Fallback
        String lobbyWorld = plugin.getConfigManager().getLobbyWorldName();
        if (player.getWorld().getName().equalsIgnoreCase(lobbyWorld)) {
            player.sendMessage(ChatColor.RED + "You are already in the lobby!");
        } else {
            player.sendMessage(ChatColor.RED + "You are not in a match or queue!");
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
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard());
        
        plugin.getBedFightManager().removePlayerFromSession(player);
        
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
