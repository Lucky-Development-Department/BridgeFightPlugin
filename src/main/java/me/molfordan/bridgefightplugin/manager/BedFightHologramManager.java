package me.molfordan.bridgefightplugin.manager;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.bedfight.BedFightSession;
import me.molfordan.bridgefightplugin.object.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BedFightHologramManager {
    private final BridgeFightPlugin plugin;

    public BedFightHologramManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    public void createTeamHolograms(BedFightSession session) {
        Location redSpawn = session.getRedSpawnLoc();
        Location blueSpawn = session.getBlueSpawnLoc();
        String arena = session.getArena().getName();
        if (arena.equals("WoodHouse")){
            createHologramsForTeam(session, "RED", redSpawn.clone().add(3, 2.5, 2.5), redSpawn.clone().add(3, 2.5, -2.5));


            createHologramsForTeam(session, "BLUE", blueSpawn.clone().add(-3, 2.5, 2.5), blueSpawn.clone().add(-3, 2.5, -2.5));
            return;
        }
        createHologramsForTeam(session, "RED", redSpawn.clone().add(2.5, 2.5, -3), redSpawn.clone().add(-2.5, 2.5, -3));


        createHologramsForTeam(session, "BLUE", blueSpawn.clone().add(2.5, 2.5, 3), blueSpawn.clone().add(-2.5, 2.5, 3));
    }

    private void createHologramsForTeam(BedFightSession session, String team, Location loc1, Location loc2) {
        // Use the first player in the team as the context for parsing placeholders, as PAPI placeholders are often global or player-independent.
        Player contextPlayer = null;
        for (UUID uuid : session.getPlayersByTeam(team)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                contextPlayer = p;
                break;
            }
        }

        // Hologram 1: Monthly Wins
        String name1 = "bf_wins_" + team + "_" + session.getMatchWorld().getName();
        Hologram holo1 = DHAPI.createHologram(name1, loc1);
        holo1.setDefaultVisibleState(false);
        DHAPI.addHologramLine(holo1, ChatColor.YELLOW + ChatColor.BOLD.toString() + "Top Monthly Wins");
        for (int i = 1; i <= 5; i++) {
            String line = i + ". %ranksystem_lb_monthlywins_bedfight_" + i + "_name% - %ranksystem_lb_monthlywins_bedfight_" + i + "_value%";
            DHAPI.addHologramLine(holo1, parsePlaceholders(contextPlayer, line));
        }

        // Hologram 2: Daily Streak
        String name2 = "bf_streak_" + team + "_" + session.getMatchWorld().getName();
        Hologram holo2 = DHAPI.createHologram(name2, loc2);
        holo2.setDefaultVisibleState(false);
        DHAPI.addHologramLine(holo2, ChatColor.YELLOW + ChatColor.BOLD.toString() + "Top Daily Streak");
        for (int i = 1; i <= 5; i++) {
            String line = i + ". %ranksystem_lb_dailystreak_bedfight_" + i + "_name% - %ranksystem_lb_dailystreak_bedfight_" + i + "_value%";
            DHAPI.addHologramLine(holo2, parsePlaceholders(contextPlayer, line));
        }

        // Set visibility for the team
        for (UUID uuid : session.getPlayersByTeam(team)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                holo1.setShowPlayer(p);
                holo2.setShowPlayer(p);
            }
        }
    }

    private String parsePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    public void removeHolograms(BedFightSession session) {
        String worldName = session.getMatchWorld().getName();
        DHAPI.removeHologram("bf_wins_RED_" + worldName);
        DHAPI.removeHologram("bf_streak_RED_" + worldName);
        DHAPI.removeHologram("bf_wins_BLUE_" + worldName);
        DHAPI.removeHologram("bf_streak_BLUE_" + worldName);
    }
}
