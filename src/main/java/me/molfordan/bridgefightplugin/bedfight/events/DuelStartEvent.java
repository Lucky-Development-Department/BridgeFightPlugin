package me.molfordan.bridgefightplugin.bedfight.events;

import me.molfordan.bridgefightplugin.bedfight.BedFightSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

public class DuelStartEvent extends BedFightEvent {
    private static final HandlerList handlers = new HandlerList();

    public DuelStartEvent(BedFightSession session) {
        super(session);
    }

    public List<Player> getRedTeamPlayers() {
        return session.getInitialPlayers("RED");
    }

    public List<Player> getBlueTeamPlayers() {
        return session.getInitialPlayers("BLUE");
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
