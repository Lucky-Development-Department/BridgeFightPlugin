package me.molfordan.bridgefightplugin.bedfight.events;

import me.molfordan.bridgefightplugin.bedfight.BedFightSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

public class DuelEndEvent extends BedFightEvent {
    private static final HandlerList handlers = new HandlerList();
    private final String winningTeam;
    private final boolean isForfeit;

    public DuelEndEvent(BedFightSession session, String winningTeam, boolean isForfeit) {
        super(session);
        this.winningTeam = winningTeam;
        this.isForfeit = isForfeit;
    }

    public String getWinningTeamColor() {
        return winningTeam;
    }

    public boolean isForfeit() {
        return isForfeit;
    }

    public List<Player> getWinners() {
        if (winningTeam == null) return new ArrayList<>();
        return session.getInitialPlayers(winningTeam);
    }

    public List<Player> getLosers() {
        if (winningTeam == null) return new ArrayList<>();
        String loserColor = winningTeam.equalsIgnoreCase("RED") ? "BLUE" : "RED";
        return session.getInitialPlayers(loserColor);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
