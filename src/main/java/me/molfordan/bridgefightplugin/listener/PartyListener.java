package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PartyManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PartyListener implements Listener {
    private final BridgeFightPlugin plugin;
    private final PartyManager partyManager;

    public PartyListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();

        // Handle '!' prefix for party chat
        if (message.startsWith("!")) {
            if (partyManager.isInParty(uuid)) {
                event.setCancelled(true);
                String partyMessage = message.substring(1).trim();
                if (!partyMessage.isEmpty()) {
                    UUID leaderId = partyManager.getPartyLeader(uuid);
                    partyManager.broadcast(leaderId, ChatColor.AQUA + player.getName() + ": " + ChatColor.WHITE + partyMessage);
                }
                return;
            }
        }

        // Handle toggled party chat
        if (partyManager.isPartyChatToggled(uuid)) {
            if (partyManager.isInParty(uuid)) {
                event.setCancelled(true);
                UUID leaderId = partyManager.getPartyLeader(uuid);
                partyManager.broadcast(leaderId, ChatColor.AQUA + player.getName() + ": " + ChatColor.WHITE + message);
            } else {
                // If they are not in a party but have toggle on, turn it off
                partyManager.togglePartyChat(uuid);
                player.sendMessage(ChatColor.RED + "Party chat disabled as you are no longer in a party.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (partyManager.isInParty(player.getUniqueId())) {
            partyManager.leaveParty(player);
        }
    }
}
