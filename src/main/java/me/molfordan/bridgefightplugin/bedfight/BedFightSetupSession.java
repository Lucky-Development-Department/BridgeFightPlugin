package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.object.Arena;

import java.util.UUID;

public class BedFightSetupSession {
    private final UUID playerUUID;
    private final String arenaName;
    private final Arena arena;

    public BedFightSetupSession(UUID playerUUID, String arenaName, Arena arena) {
        this.playerUUID = playerUUID;
        this.arenaName = arenaName;
        this.arena = arena;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getArenaName() {
        return arenaName;
    }

    public Arena getArena() {
        return arena;
    }
}
