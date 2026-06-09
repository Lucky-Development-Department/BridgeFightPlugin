package me.molfordan.bridgefightplugin.bedfight;

import org.bukkit.ChatColor;

public enum BedFightScoreboardState {
    PLAYING(ChatColor.GREEN + "✔"),
    BED_DESTROYED(ChatColor.YELLOW + "%d"),
    ELIMINATED(ChatColor.RED + "X"),
    ENDED(ChatColor.RED + "X");

    private final String icon;

    BedFightScoreboardState(String icon) {
        this.icon = icon;
    }

    public String getIcon(int playerCount) {
        return String.format(icon, playerCount);
    }
}
