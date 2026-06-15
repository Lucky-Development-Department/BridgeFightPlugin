package me.molfordan.bridgefightplugin.bedfight.events;

import me.molfordan.bridgefightplugin.bedfight.BedFightSession;
import org.bukkit.event.Event;

public abstract class BedFightEvent extends Event {
    protected final BedFightSession session;

    public BedFightEvent(BedFightSession session) {
        this.session = session;
    }

    public BedFightSession getSession() {
        return session;
    }
}
