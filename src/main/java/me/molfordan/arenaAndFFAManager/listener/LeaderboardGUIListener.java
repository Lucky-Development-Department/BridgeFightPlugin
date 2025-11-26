package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
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
    //  MAIN MENU ("Leaderboard Menu")
    // ------------------------------------------------------
    @EventHandler
    public void onMainMenuClick(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.equalsIgnoreCase("Leaderboard Menu")) return;

        if (e.getCurrentItem() == null) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        // BridgeFight
        if (item.getType() == Material.STONE_SWORD) {
            plugin.getGuiLeaderboardBridgeFight().open(p, "kills", 1,
                    plugin.getLeaderboard("bridge_kills"));
            return;
        }

        // BuildFFA (red wool)
        if (item.getType() == Material.WOOL && item.getDurability() == 14) {
            plugin.getGuiLeaderboardBuildFFA().open(p, "kills", 1,
                    plugin.getLeaderboard("build_kills"));
        }
    }



    // ------------------------------------------------------
    //  BRIDGE-FIGHT GUI
    // ------------------------------------------------------
    @EventHandler
    public void onBridgeFightClick(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();

        if (!title.startsWith("§8BridgeFight")) return;  // <-- FIXED MATCHING

        if (e.getCurrentItem() == null) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Material type = item.getType();

        // Extract metric + page
        String stripped = ChatColor.stripColor(title);
        // Example: "BridgeFight - kills (Page 1)"
        String[] split1 = stripped.split(" - ");
        if (split1.length < 2) return;

        String[] split2 = split1[1].split(" \\(Page ");
        String metric = split2[0].trim();
        int page = Integer.parseInt(split2[1].replace(")", "").trim());

        // CATEGORY BUTTONS
        if (type == Material.IRON_SWORD) {
            plugin.getGuiLeaderboardBridgeFight().open(
                    p, "kills", 1, plugin.getLeaderboard("bridge_kills"));
            return;
        }

        if (type == Material.BLAZE_POWDER) {
            plugin.getGuiLeaderboardBridgeFight().open(
                    p, "streak", 1, plugin.getLeaderboard("bridge_streak"));
            return;
        }

        if (type == Material.NETHER_STAR) {
            plugin.getGuiLeaderboardBridgeFight().open(
                    p, "highest", 1, plugin.getLeaderboard("bridge_highest_streak"));
            return;
        }

        // PAGINATION
        if (type == Material.ARROW && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            boolean next = meta.getDisplayName().contains("Next");
            int newPage = next ? page + 1 : page - 1;

            if (newPage < 1) newPage = 1;

            plugin.getGuiLeaderboardBridgeFight().open(
                    p, metric, newPage, plugin.getLeaderboard(resolveBridgeKey(metric)));
        }
    }



    // ------------------------------------------------------
    //  BUILD-FFA GUI
    // ------------------------------------------------------
    @EventHandler
    public void onBuildFFAClick(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();

        if (!title.startsWith("§8BuildFFA")) return; // <-- FIXED MATCHING

        if (e.getCurrentItem() == null) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Material type = item.getType();

        // Extract metric + page
        String stripped = ChatColor.stripColor(title);
        String[] split1 = stripped.split(" - ");
        if (split1.length < 2) return;

        String[] split2 = split1[1].split(" \\(Page ");
        String metric = split2[0].trim();
        int page = Integer.parseInt(split2[1].replace(")", "").trim());

        // CATEGORY BUTTONS
        if (type == Material.IRON_SWORD) {
            plugin.getGuiLeaderboardBuildFFA().open(
                    p, "kills", 1, plugin.getLeaderboard("build_kills"));
            return;
        }

        if (type == Material.BLAZE_POWDER) {
            plugin.getGuiLeaderboardBuildFFA().open(
                    p, "streak", 1, plugin.getLeaderboard("build_streak"));
            return;
        }

        if (type == Material.NETHER_STAR) {
            plugin.getGuiLeaderboardBuildFFA().open(
                    p, "highest", 1, plugin.getLeaderboard("build_highest_streak"));
            return;
        }

        // PAGINATION
        if (type == Material.ARROW && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            boolean next = meta.getDisplayName().contains("Next");

            int newPage = next ? page + 1 : page - 1;
            if (newPage < 1) newPage = 1;

            plugin.getGuiLeaderboardBuildFFA().open(
                    p, metric, newPage, plugin.getLeaderboard(resolveBuildKey(metric)));
        }
    }

    private String resolveBuildKey(String metric) {
        switch (metric.toLowerCase()) {
            case "kills": return "build_kills";
            case "streak": return "build_streak";
            case "highest": return "build_highest_streak"; // FIXED
        }
        return "build_kills";
    }

    private String resolveBridgeKey(String metric) {
        switch (metric.toLowerCase()) {
            case "kills": return "bridge_kills";
            case "streak": return "bridge_streak";
            case "highest": return "bridge_highest_streak"; // FIXED
        }
        return "bridge_kills";
    }
}
