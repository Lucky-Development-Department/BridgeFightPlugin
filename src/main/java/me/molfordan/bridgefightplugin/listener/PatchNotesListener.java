package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PatchNotesManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PatchNotesListener implements Listener {

    private final BridgeFightPlugin plugin;

    public PatchNotesListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        PatchNotesManager.PatchNoteSession session = plugin.getPatchNotesManager().getSession(p.getUniqueId());
        
        if (session != null && session.isListening) {
            e.setCancelled(true);
            String message = e.getMessage();
            
            if (message.equalsIgnoreCase("/patchnotes finish")) {
                plugin.getPatchNotesManager().finishSession(p.getUniqueId());
                p.sendMessage(ChatColor.GREEN + "Patch notes archived successfully!");
                return;
            }
            
            if (message.equalsIgnoreCase("/patchnotes cancel")) {
                plugin.getPatchNotesManager().cancelSession(p.getUniqueId());
                p.sendMessage(ChatColor.YELLOW + "Session cancelled.");
                return;
            }

            plugin.getPatchNotesManager().addNoteToSession(p.getUniqueId(), message);
            p.sendMessage(ChatColor.GRAY + "Added note: " + ChatColor.translateAlternateColorCodes('&', message));
            p.sendMessage(ChatColor.YELLOW + "Type next note or '" + ChatColor.GREEN + "/patchnotes finish" + ChatColor.YELLOW + "'");
            e.setCancelled(true);
        }
    }
}
