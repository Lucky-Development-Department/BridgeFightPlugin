package me.molfordan.bridgefightplugin.object;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private UUID leader;
    private final Set<UUID> members;
    private boolean open;
    private int maxSize;

    public Party(UUID leader) {
        this.leader = leader;
        this.members = new LinkedHashSet<>();
        this.members.add(leader);
        this.open = false;
        this.maxSize = 8; // Default max size
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean addMember(UUID uuid) {
        if (members.size() >= maxSize) return false;
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        if (uuid.equals(leader)) return false; // Leader cannot be removed this way
        return members.remove(uuid);
    }
}
