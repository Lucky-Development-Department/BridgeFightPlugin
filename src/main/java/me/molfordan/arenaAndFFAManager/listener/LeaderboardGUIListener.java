package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboard;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class LeaderboardGUIListener implements Listener {

    private final ArenaAndFFAManager plugin;
    private final GUILeaderboard gui;

    public LeaderboardGUIListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.gui = plugin.getGuiLeaderboard();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        // Safety checks
        if (e.getClickedInventory() == null) return;
        if (e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().contains("Leaderboard")) return;
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.AIR) return;

        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Material type = item.getType();

        // -----------------------------
        // CATEGORY SELECTORS
        // -----------------------------

        // BridgeFight (stone sword)
        if (type == Material.STONE_SWORD) {
            gui.open(p, "bridge", "kills", 1, plugin.getLeaderboard("bridge_kills"));
            return;
        }

        // BuildFFA (red wool, durability 14)
        if (type == Material.WOOL && item.getDurability() == 14) {
            gui.open(p, "build", "kills", 1, plugin.getLeaderboard("build_kills"));
            return;
        }

        // -----------------------------
        // PAGINATION
        // -----------------------------
        if (type == Material.ARROW) {

            if (!item.hasItemMeta()) return;
            if (!item.getItemMeta().hasDisplayName()) return;

            boolean next = item.getItemMeta().getDisplayName().contains("Next");

            String title = e.getView().getTitle();
            String[] parts = title.split(" ");

            // title = "Leaderboard (bridge kills Page 1)"
            // parts = ["Leaderboard", "(bridge", "kills", "Page", "1)"]

            int page;
            try {
                page = Integer.parseInt(parts[parts.length - 1].replace(")", ""));
            } catch (Exception ignored) {
                return;
            }

            page = next ? page + 1 : page - 1;
            if (page < 1) page = 1;

            // Extract category + metric from title
            String category = parts[1].replace("(", ""); // (bridge → bridge
            String metric = parts[2];                   // kills

            gui.open(
                    p,
                    category,
                    metric,
                    page,
                    plugin.getLeaderboard(category + "_" + metric)
            );
        }
    }


    @EventHandler
    public void onClick2(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(ChatColor.GOLD + "Leaderboard Menu")) return;
        if (e.getCurrentItem() == null) return;

        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Material type = item.getType();

        if (type == Material.STONE_SWORD) {
            // open first leaderboard page for bridge_kills
            plugin.getGuiLeaderboard().open(p, "bridge", "kills", 1,
                    plugin.getLeaderboard("bridge_kills"));
        }

        if (type == Material.WOOL && item.getDurability() == 14) {
            // open first leaderboard page for build_kills
            plugin.getGuiLeaderboard().open(p, "build", "kills", 1,
                    plugin.getLeaderboard("build_kills"));
        }
    }
}
