package me.molfordan.arenaAndFFAManager.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.placeholder.LeaderboardPlaceholderExpansion.LBEntry;
import me.molfordan.arenaAndFFAManager.utils.HeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GUILeaderboardBridgeFight {

    private final ArenaAndFFAManager plugin;

    public GUILeaderboardBridgeFight(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    private ItemStack outline() {
        ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack category(String name, Material material, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§e§l" + name);
        m.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(m);
        return item;
    }

    public void open(Player p, String metric, int page, List<LBEntry> entries) {

        int perPage = 28;
        int maxPage = (int) Math.ceil(entries.size() / (double) perPage);
        if (maxPage <= 0) maxPage = 1;
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(null, 54,
                "§8BridgeFight §7- §e" + metric + " §7(Page " + page + ")");

        // Outline
        for (int i = 0; i < 54; i++) {
            if (i == 45 || i == 53) continue;
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, outline());
            }
        }

        // Category Buttons
        inv.setItem(2, category("Kills", Material.IRON_SWORD, "§7Click to view kills leaderboard."));
        inv.setItem(4, category("Streak", Material.BLAZE_POWDER, "§7Click to view streak leaderboard."));
        inv.setItem(6, category("Highest Streak", Material.NETHER_STAR, "§7Click to view highest streak leaderboard."));

        // Pagination
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§aPrevious Page");
            prev.setItemMeta(pm);
            inv.setItem(45, prev);
        }

        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§aNext Page");
            next.setItemMeta(nm);
            inv.setItem(53, next);
        }

        // Leaderboard slots
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, entries.size());

        int[] slots = {
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43
        };

        int si = 0;

        for (int i = start; i < end; i++) {
            int slot = slots[si];
            LBEntry e = entries.get(i);

            // create placeholder head & async update
            ItemStack placeholder = HeadUtils.getHead(
                    e.uuid,
                    e.name,
                    e.value,
                    i + 1,
                    updated -> Bukkit.getScheduler().runTask(plugin, () -> inv.setItem(slot, updated))
            );

            inv.setItem(slot, placeholder);
            si++;
        }

        while (si < slots.length) {
            int slot = slots[si];

            ItemStack placeholder = HeadUtils.getHead(
                    null,
                    null,
                    0,
                    si + 1,
                    updated -> Bukkit.getScheduler().runTask(plugin, () -> inv.setItem(slot, updated))
            );

            inv.setItem(slot, placeholder);
            si++;
        }

        p.openInventory(inv);
    }

}
