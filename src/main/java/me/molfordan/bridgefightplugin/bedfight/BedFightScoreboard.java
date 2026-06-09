package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BedFightScoreboard {
    private final BridgeFightPlugin plugin;
    private final BedFightManager bedFightManager;

    public BedFightScoreboard(BridgeFightPlugin plugin, BedFightManager bedFightManager) {
        this.plugin = plugin;
        this.bedFightManager = bedFightManager;
    }

    public void updateScoreboard(Player player) {
        BedFightSession session = bedFightManager.getSession(player);
        
        // Only show if in a match world and in a session
        if (session == null || !player.getWorld().getName().startsWith("bf_")) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }

        Scoreboard board = player.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective("bedfight");
        if (objective == null) {
            objective = board.registerNewObjective("bedfight", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "BEDFIGHT");
        }

        int redAlive = 0;
        int redPingTotal = 0;
        int redCount = 0;
        for (UUID uuid : session.getPlayersByTeam("RED")) {
            redCount++;
            if (session.getPlayerState(uuid) != BedFightState.ENDED) redAlive++;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) redPingTotal += getPing(p);
        }
        
        int blueAlive = 0;
        int bluePingTotal = 0;
        int blueCount = 0;
        for (UUID uuid : session.getPlayersByTeam("BLUE")) {
            blueCount++;
            if (session.getPlayerState(uuid) != BedFightState.ENDED) blueAlive++;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) bluePingTotal += getPing(p);
        }

        // Status logic using BedFightScoreboardState
        BedFightState playerState = session.getPlayerState(player.getUniqueId());
        if (session.isSpectator(player.getUniqueId()) && playerState != BedFightState.SPECTATOR_DUEL) {
            // Spectator view (for joining spectators)
            updateScore(board, objective, ChatColor.RED + "Red: " + getTeamFormatted(session, "RED"), 8);
            updateScore(board, objective, ChatColor.BLUE + "Blue: " + getTeamFormatted(session, "BLUE"), 7);
            updateScore(board, objective, " ", 6);
            updateScore(board, objective, ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "SPECTATING", 5);
        } else {
            // Participant view
            BedFightScoreboardState redState = session.getTeamScoreboardState("RED");
            BedFightScoreboardState blueState = session.getTeamScoreboardState("BLUE");
            
            String redStatus = redState.getIcon(redAlive);
            String blueStatus = blueState.getIcon(blueAlive);
            
            String team = session.getInitialTeam(player.getUniqueId());
            String redSuffix = "RED".equals(team) ? ChatColor.GRAY + " YOU" : "";
            String blueSuffix = "BLUE".equals(team) ? ChatColor.GRAY + " YOU" : "";

            int yourPing = getPing(player);
            int enemyPing;

            if ("RED".equals(team)) {
                enemyPing = blueCount > 0 ? bluePingTotal / blueCount : 0;
            } else if ("BLUE".equals(team)) {
                enemyPing = redCount > 0 ? redPingTotal / redCount : 0;
            } else {
                enemyPing = (redCount + blueCount) > 0 ? (redPingTotal + bluePingTotal) / (redCount + blueCount) : 0;
            }

            BedFightStats stats = session.getStats(player.getUniqueId());
            int totalKills = (stats != null) ? (stats.kills + stats.voidKills + stats.finalKills + stats.voidFinalKills) : 0;

            updateScore(board, objective, ChatColor.RED + "R " + ChatColor.WHITE + "Red: " + redStatus + redSuffix, 8);
            updateScore(board, objective, ChatColor.BLUE + "B " + ChatColor.WHITE + "Blue: " + blueStatus + blueSuffix, 7);
            updateScore(board, objective, " ", 6);
            if (stats != null) {
                updateScore(board, objective, ChatColor.WHITE + "Kills: " + ChatColor.YELLOW + totalKills, 5);
            } else {
                updateScore(board, objective, ChatColor.GRAY + "SPECTATING", 5);
            }
            updateScore(board, objective, "  ", 4);
            updateScore(board, objective, ChatColor.WHITE + "Your ping: " + ChatColor.YELLOW + yourPing + "ms", 3);
            if (stats != null) {
                updateScore(board, objective, ChatColor.WHITE + "Enemy ping: " + ChatColor.YELLOW + enemyPing + "ms", 2);
            } else {
                updateScore(board, objective, ChatColor.WHITE + "Arena: " + ChatColor.YELLOW + session.getArena().getName(), 2);
            }
        }
        updateScore(board, objective, "   ", 1);
        updateScore(board, objective, ChatColor.YELLOW + "luckynetwork.net", 0);
    }

    private String getTeamFormatted(BedFightSession session, String team) {
        List<String> formatted = new ArrayList<>();
        for (UUID uuid : session.getPlayersByTeam(team)) {
            if (session.getPlayerState(uuid) == BedFightState.PLAYING || session.getPlayerState(uuid) == BedFightState.RESPAWNED) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    formatted.add(p.getName() + "(" + getPing(p) + "ms)");
                }
            }
        }
        return formatted.isEmpty() ? ChatColor.RED + "ELIMINATED" : String.join(", ", formatted);
    }

    private void updateScore(Scoreboard board, Objective objective, String entry, int score) {
        // Simple implementation: remove old scores and set new
        for (String s : board.getEntries()) {
            if (board.getObjective(DisplaySlot.SIDEBAR).getScore(s).getScore() == score) {
                if (!s.equals(entry)) {
                    board.resetScores(s);
                }
            }
        }
        objective.getScore(entry).setScore(score);
    }

    private int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            return 0;
        }
    }
}
