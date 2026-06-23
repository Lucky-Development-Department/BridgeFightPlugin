package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        plugin.debug("DEBUG: LeaveCommand check inQueue=" + inQueue + " for " + player.getName());
        
        if (inQueue) {
            plugin.debug("DEBUG: LeaveCommand calling removeFromQueue for " + player.getName());
            plugin.getMatchmakingService().removeFromQueue(player);
            plugin.debug("DEBUG: LeaveCommand removeFromQueue called for " + player.getName());
            return true;
        }

        // 2. Check if in a session (participant or spectator)
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        plugin.debug("DEBUG: LeaveCommand session=" + (session != null) + " for " + player.getName());
        if (session != null) {
            BedFightPlayerState state = session.getPlayerState(uuid);
            
            if (state == BedFightPlayerState.ENDED) {
                performLeave(player);
                player.sendMessage(ChatColor.YELLOW + "You have left the match.");
            } else if (session.isSpectator(uuid)) {
                performLeave(player);
                player.sendMessage(ChatColor.YELLOW + "You are no longer spectating the match.");
            } else {
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
        String buildFFAWorld = plugin.getConfigManager().getBuildFFAWorldName();
        String bridgeFightWorld = plugin.getConfigManager().getBridgeFightWorldName();
        String currentWorld = player.getWorld().getName();

        // 1. Reset core player state
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard());
        
        // 2. Remove from active session tracking
        plugin.getBedFightManager().removePlayerFromSession(player);

        // 3. Logic: Stay in world with kit if applicable, otherwise return to lobby
        if (currentWorld.equalsIgnoreCase(buildFFAWorld)) {
            plugin.getKitManager().applyBuildFFAKit(player);
        } else if (currentWorld.equalsIgnoreCase(bridgeFightWorld)) {
            plugin.getKitManager().applyBridgeFightKit(player);
        } else {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.teleport(lobby);
        }
        
        // 4. Handle lobby-specific items with a 1-tick delay
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || plugin.getBedFightManager().isInMatch(player)) return;

            if (player.getWorld().getName().equalsIgnoreCase(lobby.getWorld().getName())) {
                if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
                    plugin.getMatchmakingService().giveLeaveItem(player);
                } else if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
                    plugin.getPartyManager().givePartyItems(player);
                } else {
                    plugin.getSpawnItem().giveSpawnItem(player);
                }
            }
        }, 1L);
    }
}
