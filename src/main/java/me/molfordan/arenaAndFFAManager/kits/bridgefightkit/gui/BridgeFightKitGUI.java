package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BridgeFightKitManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BridgeFightKitGUI {

    private final ArenaAndFFAManager plugin;
    private final BridgeFightKitManager kitManager;

    public BridgeFightKitGUI(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getBridgeFightKitManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Select BridgeFight Kit");

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        int kills = stats != null ? stats.getBridgeKills() : 0;

        int slot = 10;

        for (Kit kit : kitManager.getAllKits()) {

            boolean unlocked = kills >= kit.getRequiredKills();

            ItemStack icon = new ItemStack(unlocked ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(unlocked ? "§a" + kit.getName() : "§c" + kit.getName());
            meta.setLore(Arrays.asList(
                    "§7Kills Required: §e" + kit.getRequiredKills(),
                    unlocked ? "§aUnlocked" : "§cLocked"
            ));
            icon.setItemMeta(meta);

            inv.setItem(slot, icon);
            slot++;
        }

        player.openInventory(inv);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;

        String name = e.getCurrentItem().getItemMeta().getDisplayName();
        if (name == null) return;

        // remove colors
        name = name.replace("§a", "").replace("§c", "");

        Kit kit = kitManager.get(name);
        if (kit == null) return;

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        int kills = stats != null ? stats.getBridgeKills() : 0;

        if (kills < kit.getRequiredKills()) {
            player.sendMessage("§cYou do not meet the requirements for this kit!");
            return;
        }

        plugin.getKitManager().setSelectedBridgeFightKit(player.getUniqueId(), kit.getName());
        player.sendMessage("§aEquipped kit: " + kit.getName());
        player.closeInventory();
    }
}
