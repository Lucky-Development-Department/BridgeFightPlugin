package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class BedFightScoreboard {
    private final ArenaAndFFAManager plugin;
    private final BedFightManager bedFightManager;

    public BedFightScoreboard(ArenaAndFFAManager plugin, BedFightManager bedFightManager) {
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

        Player p1 = Bukkit.getPlayer(session.getRedPlayer());
        Player p2 = Bukkit.getPlayer(session.getBluePlayer());

        // Status logic: ✔ if bed alive, player count if destroyed, X if eliminated
        String redStatus = getTeamStatus(session.isRedBedAlive(), session.isRedEliminated(), 1);
        String blueStatus = getTeamStatus(session.isBlueBedAlive(), session.isBlueEliminated(), 1);
        
        String redSuffix = player.equals(p1) ? ChatColor.GRAY + " YOU" : "";
        String blueSuffix = player.equals(p2) ? ChatColor.GRAY + " YOU" : "";

        int p1Ping = (p1 != null) ? getPing(p1) : 0;
        int p2Ping = (p2 != null) ? getPing(p2) : 0;

        int yourPing;
        int enemyPing;

        if (player.equals(p1)) {
            yourPing = p1Ping;
            enemyPing = p2Ping;
        } else if (player.equals(p2)) {
            yourPing = p2Ping;
            enemyPing = p1Ping;
        } else {
            // Spectator
            yourPing = getPing(player);
            enemyPing = (p1Ping + p2Ping) / 2; // Average or something? 
        }

        // Calculate total kills
        BedFightStats stats = session.getStats(player.getUniqueId());
        int totalKills = (stats != null) ? (stats.kills + stats.voidKills + stats.finalKills + stats.voidFinalKills) : 0;

        // Use teams for better score updating to prevent flickering
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
            updateScore(board, objective, ChatColor.WHITE + "Their ping: " + ChatColor.YELLOW + enemyPing + "ms", 2);
        } else {
            updateScore(board, objective, ChatColor.WHITE + "Arena: " + ChatColor.YELLOW + session.getArena().getName(), 2);
        }
        updateScore(board, objective, "   ", 1);
        updateScore(board, objective, ChatColor.YELLOW + "luckynetwork.net", 0);
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

    private String getTeamStatus(boolean bedAlive, boolean eliminated, int playerCount) {
        if (bedAlive) return ChatColor.GREEN + "✔";
        if (eliminated) return ChatColor.RED + "X";
        return ChatColor.YELLOW + String.valueOf(playerCount);
    }
}
