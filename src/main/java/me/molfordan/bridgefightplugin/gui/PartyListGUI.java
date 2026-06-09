package me.molfordan.bridgefightplugin.gui;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PartyListGUI {

    public static void open(Player player, BridgeFightPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Available Parties");
        PartyManager pm = plugin.getPartyManager();

        int i = 0;
        for (UUID leaderId : pm.getActivePartyLeaders()) {
            if (i >= 27) break;
            Player leader = Bukkit.getPlayer(leaderId);
            me.molfordan.bridgefightplugin.object.Party party = pm.getParty(leaderId);
            if (leader != null && party != null) {
                ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                meta.setOwner(leader.getName());
                meta.setDisplayName(ChatColor.YELLOW + leader.getName() + "'s Party");
                
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + party.getMembers().size() + "/" + party.getMaxSize());
                lore.add(ChatColor.GRAY + "Status: " + (party.isOpen() ? ChatColor.GREEN + "OPEN" : ChatColor.RED + "CLOSED"));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to Challenge/Join!");
                meta.setLore(lore);
                
                head.setItemMeta(meta);
                inv.setItem(i++, head);
            }
        }
        
        player.openInventory(inv);
    }
}
