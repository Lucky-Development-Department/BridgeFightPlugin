package me.molfordan.bridgefightplugin.cosmetics.objects;

import org.bukkit.ChatColor;

public enum CosmeticTier {
    COMMON("Common", ChatColor.GRAY),
    UNCOMMON("Uncommon", ChatColor.GREEN),
    RARE("Rare", ChatColor.BLUE),
    EPIC("Epic", ChatColor.DARK_PURPLE),
    LEGENDARY("Legendary", ChatColor.GOLD),
    MYTHIC("Mythic", ChatColor.LIGHT_PURPLE);

    private final String name;
    private final ChatColor color;

    CosmeticTier(String name, ChatColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getFormattedName() {
        return color + name;
    }

    public static CosmeticTier fromString(String tierStr) {
        if (tierStr == null) return COMMON;
        try {
            return valueOf(tierStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
