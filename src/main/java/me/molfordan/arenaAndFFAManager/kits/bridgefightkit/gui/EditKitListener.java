package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit2;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EditKitListener implements Listener {

    private final ArenaAndFFAManager plugin;

    public EditKitListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        // Check GUI title
        if (!e.getView().getTitle().contains("Edit Kit")) return;
        e.setCancelled(true);

        // Get active kit the player is editing
        Kit2 kit = plugin.getBridgeFightGUI().getEditingKit(player.getUniqueId());
        if (kit == null) return;

        int slot = e.getRawSlot();

        switch (slot) {

            // -------------------------------
            // Rename Kit
            // -------------------------------
            case 11:
                player.closeInventory();
                player.sendMessage("§eType the new kit name in chat.");
                plugin.getBridgeFightGUI().addPendingRename(player.getUniqueId(), kit);
                break;

            // -------------------------------
            // Edit Items (open kit editor)
            // -------------------------------
            case 13:
                plugin.getBridgeFightGUI().openEdit(player, kit);
                break;

            // -------------------------------
            // Delete Kit
            // -------------------------------
            case 15:
                plugin.getBridgeFightKitManager().delete(kit.getName());
                player.sendMessage("§cKit deleted.");
                player.closeInventory();
                plugin.getBridgeFightGUI().open(player);
                break;

            // -------------------------------
            // Back to main GUI
            // -------------------------------
            case 22:
                plugin.getBridgeFightGUI().open(player);
                break;
        }
    }
}
