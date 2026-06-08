package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.object.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    // Map of Leader UUID -> Party Object
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    // Map of Player UUID -> Leader UUID
    private final Map<UUID, UUID> playerToParty = new ConcurrentHashMap<>();
    
    // Players who have party chat toggled on
    private final Set<UUID> partyChatToggled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Map of Target UUID -> Map<Leader UUID, Expiry Time>
    private final Map<UUID, Map<UUID, Long>> pendingInvites = new ConcurrentHashMap<>();
    private static final long INVITE_EXPIRY_MS = 60000; // 60 seconds

    public void createParty(Player leader) {
        if (isInParty(leader.getUniqueId())) return;
        Party party = new Party(leader.getUniqueId());
        parties.put(leader.getUniqueId(), party);
        playerToParty.put(leader.getUniqueId(), leader.getUniqueId());
    }

    public void addInvite(UUID leaderId, UUID targetId) {
        pendingInvites.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>())
                      .put(leaderId, System.currentTimeMillis() + INVITE_EXPIRY_MS);
    }

    public boolean hasInvite(UUID targetId, UUID leaderId) {
        Map<UUID, Long> invites = pendingInvites.get(targetId);
        if (invites == null) return false;
        Long expiry = invites.get(leaderId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            invites.remove(leaderId);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID targetId, UUID leaderId) {
        Map<UUID, Long> invites = pendingInvites.get(targetId);
        if (invites != null) {
            invites.remove(leaderId);
        }
    }

    public void removeInvite(UUID targetId) {
        pendingInvites.remove(targetId);
    }

    public void disbandParty(Player leader) {
        if (!isLeader(leader.getUniqueId())) return;
        Party party = parties.remove(leader.getUniqueId());
        if (party != null) {
            broadcast(leader.getUniqueId(), ChatColor.RED + "The party has been disbanded.");
            for (UUID memberId : party.getMembers()) {
                playerToParty.remove(memberId);
                partyChatToggled.remove(memberId);
            }
        }
    }

    public boolean addMember(Player leader, Player member) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null || !isLeader(leader.getUniqueId())) return false;
        
        if (party.addMember(member.getUniqueId())) {
            playerToParty.put(member.getUniqueId(), leader.getUniqueId());
            broadcast(leader.getUniqueId(), ChatColor.GREEN + member.getName() + " joined the party.");
            return true;
        }
        return false;
    }

    public void leaveParty(Player player) {
        UUID leaderId = playerToParty.get(player.getUniqueId());
        if (leaderId == null) return;

        if (leaderId.equals(player.getUniqueId())) {
            // Player is leader
            Party party = parties.get(leaderId);
            if (party != null && party.getMembers().size() > 1) {
                // Find next leader (earliest member excluding the leader)
                Iterator<UUID> it = party.getMembers().iterator();
                it.next(); // Skip leader
                UUID nextLeader = it.next();
                
                promotePlayer(player, nextLeader);
                broadcast(nextLeader, ChatColor.GREEN + player.getName() + ChatColor.YELLOW + " left the party, " + ChatColor.GREEN + Bukkit.getOfflinePlayer(nextLeader).getName() + ChatColor.YELLOW + " is the new leader.");
                
                // Now remove the old leader
                party.removeMember(player.getUniqueId());
                playerToParty.remove(player.getUniqueId());
                partyChatToggled.remove(player.getUniqueId());
            } else {
                // Leader is alone, disband
                disbandParty(player);
            }
        } else {
            // Player is member
            broadcast(leaderId, ChatColor.RED + player.getName() + " left the party.");
            Party party = parties.get(leaderId);
            if (party != null) {
                party.removeMember(player.getUniqueId());
            }
            playerToParty.remove(player.getUniqueId());
            partyChatToggled.remove(player.getUniqueId());
        }
    }

    public void kickPlayer(Player leader, UUID targetId) {
        if (!isLeader(leader.getUniqueId())) return;
        Party party = parties.get(leader.getUniqueId());
        if (party != null && party.removeMember(targetId)) {
            playerToParty.remove(targetId);
            partyChatToggled.remove(targetId);
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(targetId);
            if (target != null) {
                target.sendMessage(ChatColor.RED + "You have been kicked from the party.");
            }
            broadcast(leader.getUniqueId(), ChatColor.RED + (target != null ? target.getName() : "A player") + " has been kicked from the party.");
        }
    }

    public void promotePlayer(Player leader, UUID targetId) {
        if (!isLeader(leader.getUniqueId())) return;
        Party party = parties.remove(leader.getUniqueId());
        if (party != null && party.getMembers().contains(targetId)) {
            party.setLeader(targetId);
            parties.put(targetId, party);
            for (UUID memberId : party.getMembers()) {
                playerToParty.put(memberId, targetId);
            }
            org.bukkit.entity.Player newLeader = org.bukkit.Bukkit.getPlayer(targetId);
            broadcast(targetId, ChatColor.GREEN + (newLeader != null ? newLeader.getName() : "A player") + " has been promoted to party leader.");
        }
    }

    public void broadcast(UUID leaderId, String message) {
        Party party = parties.get(leaderId);
        if (party != null) {
            for (UUID uuid : party.getMembers()) {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(ChatColor.LIGHT_PURPLE + "[Party] " + ChatColor.WHITE + message);
            }
        }
    }

    public boolean isInParty(UUID uuid) {
        return playerToParty.containsKey(uuid);
    }
    
    public boolean isInParty(Player player) {
        return isInParty(player.getUniqueId());
    }

    public boolean isLeader(UUID uuid) {
        return parties.containsKey(uuid);
    }

    public UUID getPartyLeader(UUID playerId) {
        return playerToParty.get(playerId);
    }

    public Set<UUID> getPartyMembers(UUID leaderId) {
        Party party = parties.get(leaderId);
        return party != null ? party.getMembers() : null;
    }
    
    public Party getParty(UUID leaderId) {
        return parties.get(leaderId);
    }
    
    public Party getPartyByMember(UUID memberId) {
        UUID leaderId = playerToParty.get(memberId);
        return leaderId != null ? parties.get(leaderId) : null;
    }

    public Set<UUID> getActivePartyLeaders() {
        return parties.keySet();
    }

    public List<Set<UUID>> splitParty(UUID leaderId) {
        Party party = parties.get(leaderId);
        if (party == null || party.getMembers().size() < 2) return null;

        List<UUID> memberList = new ArrayList<>(party.getMembers());
        Collections.shuffle(memberList);

        int mid = memberList.size() / 2;
        Set<UUID> team1 = new HashSet<>(memberList.subList(0, mid));
        Set<UUID> team2 = new HashSet<>(memberList.subList(mid, memberList.size()));

        List<Set<UUID>> split = new ArrayList<>();
        split.add(team1);
        split.add(team2);
        return split;
    }

    // Map of Challenger Leader UUID -> Target Leader UUID
    private final Map<UUID, UUID> pendingChallenges = new ConcurrentHashMap<>();

    public void addChallenge(UUID challengerId, UUID targetId) {
        pendingChallenges.put(challengerId, targetId);
    }

    public boolean hasChallenge(UUID challengerId, UUID targetId) {
        return targetId.equals(pendingChallenges.get(challengerId));
    }

    public void removeChallenge(UUID challengerId) {
        pendingChallenges.remove(challengerId);
    }
    
    public boolean isPartyChatToggled(UUID uuid) {
        return partyChatToggled.contains(uuid);
    }
    
    public void togglePartyChat(UUID uuid) {
        if (partyChatToggled.contains(uuid)) {
            partyChatToggled.remove(uuid);
        } else {
            partyChatToggled.add(uuid);
        }
    }

    public void givePartyItems(Player player) {
        player.getInventory().clear();

        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD);
        org.bukkit.inventory.meta.ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName(ChatColor.AQUA + "Party Queue");
        sword.setItemMeta(swordMeta);

        org.bukkit.inventory.ItemStack bed = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BED);
        org.bukkit.inventory.meta.ItemMeta bedMeta = bed.getItemMeta();
        bedMeta.setDisplayName(ChatColor.RED + "Leave Party");
        bed.setItemMeta(bedMeta);

        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(8, bed);
    }
}
