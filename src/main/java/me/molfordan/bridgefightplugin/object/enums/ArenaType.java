package me.molfordan.bridgefightplugin.object.enums;

public enum ArenaType {
    FFA,
    FFABUILD,
    BRIDGE,
    BUILD,

    TOPFIGHT,
    BEDFIGHT,
    DUEL;

    public static ArenaType fromString(String input) {
        if (input == null) return null;
        switch (input.toLowerCase()) {
            case "ffa": return FFA;
            case "ffabuild": return FFABUILD;
            case "bridge": return BRIDGE;
            case "build": return BUILD;
            case "duel": return DUEL;
            case "topfight": return TOPFIGHT;
            case "bedfight": return BEDFIGHT;
            default: return null;
        }
    }
}
