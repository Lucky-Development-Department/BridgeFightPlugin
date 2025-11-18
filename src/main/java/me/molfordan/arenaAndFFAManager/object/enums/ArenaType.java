package me.molfordan.arenaAndFFAManager.object.enums;

public enum ArenaType {
    FFA,
    FFABUILD,

    TOPFIGHT,
    DUEL;

    public static ArenaType fromString(String input) {
        if (input == null) return null;
        switch (input.toLowerCase()) {
            case "ffa": return FFA;
            case "ffabuild": return FFABUILD;
            case "duel": return DUEL;
            case "topfight": return TOPFIGHT;
            default: return null;
        }
    }
}