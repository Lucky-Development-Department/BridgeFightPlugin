package me.molfordan.bridgefightplugin.event;

import me.molfordan.bridgefightplugin.object.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BridgeFightKillEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private final Player killer;
    private final Player victim;
    private final Arena arena;
    private final boolean isVoid;

    public BridgeFightKillEvent(Player killer, Player victim, Arena arena, boolean isVoid) {
        this.killer = killer;
        this.victim = victim;
        this.arena = arena;
        this.isVoid = isVoid;
    }

    public Player getKiller() {
        return killer;
    }

    public Player getVictim() {
        return victim;
    }

    public Arena getArena() {
        return arena;
    }

    public boolean isVoid() {
        return isVoid;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
