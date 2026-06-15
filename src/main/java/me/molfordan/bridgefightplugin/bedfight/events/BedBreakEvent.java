package me.molfordan.bridgefightplugin.bedfight.events;

import me.molfordan.bridgefightplugin.bedfight.BedFightSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class BedBreakEvent extends BedFightEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Player breaker;
    private final String teamColor;

    public BedBreakEvent(BedFightSession session, Player breaker, String teamColor) {
        super(session);
        this.breaker = breaker;
        this.teamColor = teamColor;
    }

    public Player getBreaker() {
        return breaker;
    }

    public String getTeamColor() {
        return teamColor;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
