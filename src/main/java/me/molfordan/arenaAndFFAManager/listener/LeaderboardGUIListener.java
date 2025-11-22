package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboard;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboardMain;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LeaderboardGUIListener implements Listener {

    private final ArenaAndFFAManager plugin;

    public LeaderboardGUIListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------
    //  MAIN LEADERBOARD MENU ("Leaderboard Menu")
    // ------------------------------------------------------
    @EventHandler
    public void onMainMenuClick(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(ChatColor.GOLD + "Leaderboard Menu")) return;

        if (e.getCurrentItem() == null) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        Material type = e.getCurrentItem().getType();

        if (type == Material.STONE_SWORD) {
            // Open BRIDGE leaderboard page 1
            plugin.getGuiLeaderboard().open(
                    p, "bridge", "kills", 1, plugin.getLeaderboard("bridge_kills")
            );
        }

        if (type == Material.WOOL && e.getCurrentItem().getDurability() == 14) {
            // Open BUILD leaderboard page 1
            plugin.getGuiLeaderboard().open(
                    p, "build", "kills", 1, plugin.getLeaderboard("build_kills")
            );
        }
    }

    // ------------------------------------------------------
    //  LEADERBOARD PAGE NAVIGATION ("§8Leaderboard: ...")
    // ------------------------------------------------------
    @EventHandler
    public void onLeaderboardPageClick(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();

        if (!title.startsWith("§8Leaderboard: §e")) return;
        if (e.getCurrentItem() == null) return;

        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Material type = item.getType();

        // Extract metadata from title
        // Example: "§8Leaderboard: §ebridge kills §7(Page 1)"
        try {
            String t = title.replace("§8Leaderboard: §e", "");
            String[] sec = t.split(" §7\\(Page ");
            String[] catMet = sec[0].split(" ");
            String category = catMet[0];
            String metric = catMet[1];
            int page = Integer.parseInt(sec[1].replace(")", ""));

            // CATEGORY buttons
            if (type == Material.STONE_SWORD) {
                plugin.getGuiLeaderboard().open(
                        p, "bridge", "kills", 1,
                        plugin.getLeaderboard("bridge_kills")
                );
                return;
            }

            if (type == Material.WOOL && item.getDurability() == 14) {
                plugin.getGuiLeaderboard().open(
                        p, "build", "kills", 1,
                        plugin.getLeaderboard("build_kills")
                );
                return;
            }

            // PAGINATION buttons
            if (type == Material.ARROW) {
                if (!item.hasItemMeta()) return;
                ItemMeta meta = item.getItemMeta();
                if (!meta.hasDisplayName()) return;

                boolean next = meta.getDisplayName().contains("Next");

                int newPage = next ? page + 1 : page - 1;
                if (newPage < 1) newPage = 1;

                plugin.getGuiLeaderboard().open(
                        p,
                        category,
                        metric,
                        newPage,
                        plugin.getLeaderboard(category + "_" + metric)
                );
            }

        } catch (Exception ignored) {
        }
    }
}
