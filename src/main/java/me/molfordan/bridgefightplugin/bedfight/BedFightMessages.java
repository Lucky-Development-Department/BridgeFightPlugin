package me.molfordan.bridgefightplugin.bedfight;

import org.bukkit.ChatColor;

public class BedFightMessages {
    // Regular Kill
    public static final String REGULAR_KILL = "%s " + ChatColor.YELLOW + "was killed by %s";
    public static final String REGULAR_DEATH = "%s " + ChatColor.YELLOW + "died.";

    // Final Kill
    public static final String FINAL_KILL = "%s " + ChatColor.YELLOW + "was killed by %s" + ChatColor.AQUA + ChatColor.BOLD + " FINAL KILL";
    public static final String FINAL_DEATH = "%s " + ChatColor.YELLOW + "was killed." + ChatColor.AQUA + ChatColor.BOLD + " FINAL KILL";

    // Void Kill
    public static final String VOID_KILL = "%s " + ChatColor.YELLOW + "was thrown into the void by %s.";
    public static final String VOID_DEATH = "%s " + ChatColor.YELLOW + "fell into the void.";

    // Final Void Kill
    public static final String FINAL_VOID_KILL = "%s " + ChatColor.YELLOW + "was hit into the void by %s" + ChatColor.AQUA + ChatColor.BOLD + " FINAL KILL";
    public static final String FINAL_VOID_DEATH = "%s " + ChatColor.YELLOW + "fell into the void " + ChatColor.AQUA + ChatColor.BOLD + " FINAL KILL";

    // Forfeit/Disconnect
    public static final String FORFEIT = ChatColor.RED + "%s" + ChatColor.YELLOW + " forfeited.";
    public static final String DISCONNECT = ChatColor.RED + "%s" + ChatColor.YELLOW + " disconnected.";
}
