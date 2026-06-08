package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpectatorListener implements Listener {
    private final ArenaAndFFAManager plugin;

    public SpectatorListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session == null || session.getPlayerState(player.getUniqueId()) != BedFightState.ENDED) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();
        if (name.contains("Teleport")) {
            // Placeholder: Open inventory for teleport
            player.sendMessage(ChatColor.YELLOW + "Teleport feature coming soon!");
        } else if (name.contains("Leave")) {
            player.performCommand("leave");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session == null || session.getPlayerState(player.getUniqueId()) != BedFightState.ENDED) return;
        
        event.setCancelled(true);
    }

    public static void giveSpectatorItems(Player player) {
        player.getInventory().clear();

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GREEN + "Teleport (Right Click)");
        compass.setItemMeta(compassMeta);

        ItemStack leaveItem = new ItemStack(Material.BED);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        leaveMeta.setDisplayName(ChatColor.RED + "Leave (Right Click)");
        leaveItem.setItemMeta(leaveMeta);

        player.getInventory().setItem(0, compass);
        player.getInventory().setItem(8, leaveItem);
    }
}
