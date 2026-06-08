package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class PartyListGUIListener implements Listener {
    private final ArenaAndFFAManager plugin;

    public PartyListGUIListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Available Parties")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.SKULL_ITEM) return;

        Player player = (Player) event.getWhoClicked();
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        String targetName = meta.getOwner();
        
        if (targetName == null) return;
        
        Player targetLeader = Bukkit.getPlayer(targetName);
        if (targetLeader == null) {
            player.sendMessage(ChatColor.RED + "That party leader is no longer online.");
            return;
        }

        if (targetLeader.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot challenge your own party!");
            return;
        }

        me.molfordan.arenaAndFFAManager.object.Party party = plugin.getPartyManager().getParty(targetLeader.getUniqueId());
        if (party != null && party.isOpen() && !plugin.getPartyManager().isInParty(player)) {
            player.closeInventory();
            player.performCommand("bfparty join " + targetName);
        } else {
            player.closeInventory();
            player.performCommand("bfparty challenge " + targetName);
        }
    }
}
