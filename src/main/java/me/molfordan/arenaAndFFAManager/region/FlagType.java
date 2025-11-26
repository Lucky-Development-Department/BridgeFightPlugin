package me.molfordan.arenaAndFFAManager.region;

public enum FlagType {

    // --------- BASIC CONTROL ---------
    BUILD("build"),                 // allow / deny breaking & placing blocks
    PVP("pvp"),                     // allow / deny combat
    USE("use"),                     // opening doors, buttons, levers
    CHEST_ACCESS("chest-access"),   // chest, furnace, dispenser access
    INTERACT("interact"),           // general interaction

    // --------- PLAYER MOVEMENT ---------
    ENTRY("entry"),                 // allow entering region
    EXIT("exit"),                   // allow leaving region
    ENTRY_MESSAGE("entry-message"),
    EXIT_MESSAGE("exit-message"),

    // --------- MOB & ENTITY CONTROL ---------
    MOB_SPAWNING("mob-spawning"),   // mobs spawn?
    CREEPER_EXPLOSION("creeper-explosion"),
    TNT("tnt"),
    FIRE_SPREAD("fire-spread"),

    // --------- DAMAGE CONTROL ---------
    FALL_DAMAGE("fall-damage"),
    FIRE_DAMAGE("fire-damage"),
    LAVA_DAMAGE("lava-damage"),
    SUFFOCATE("suffocate"),
    DROWNING("drowning"),
    EXPLOSION_DAMAGE("explosion-damage"),

    // --------- ITEM CONTROL ---------
    ITEM_DROP("item-drop"),
    ITEM_PICKUP("item-pickup"),

    // --------- INVENTORY / INTERACTION ---------
    ENDER_CHEST_ACCESS("ender-chest-access"),
    ANVIL_USE("anvil-use"),
    ENCHANTING("enchanting"),

    // --------- NON–WG BUT USEFUL ---------
    COMMAND_EXECUTE("command-execute"), // block certain commands
    TELEPORT("teleport");               // block teleporting inside region

    private final String id;

    FlagType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static FlagType fromId(String id) {
        for (FlagType f : values()) {
            if (f.id.equalsIgnoreCase(id)) return f;
        }
        return null;
    }
}
