package me.molfordan.bridgefightplugin.gui;

import me.molfordan.bridgefightplugin.bedfight.BedFightSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SpectatorTeleportGUI {

    public static void open(Player player, BedFightSession session) {
        int size = (int) Math.ceil(session.getAllPlayers().size() / 9.0) * 9;
        Inventory inv = Bukkit.createInventory(null, Math.max(9, size), ChatColor.DARK_GRAY + "Teleport to Player");

        for (UUID uuid : session.getAllPlayers()) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target != player) {
                ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwner(target.getName());
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                head.setItemMeta(meta);
                inv.addItem(head);
            }
        }
        
        player.openInventory(inv);
    }
}
