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

public class GUILeaderboard {

    private final ArenaAndFFAManager plugin;

    public GUILeaderboard(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    private ItemStack outline() {
        ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack categoryItem(String name, Material m, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName("§e§l" + name);
        meta.setLore(java.util.Arrays.asList(lore));
        i.setItemMeta(meta);
        return i;
    }

    private ItemStack categoryItem(String name, Material m, int in, short s, String... lore) {
        ItemStack i = new ItemStack(m, in, s);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName("§e§l" + name);
        meta.setLore(java.util.Arrays.asList(lore));
        i.setItemMeta(meta);
        return i;
    }


    // -------------------------------------------------------
    // OPEN leaderboard GUI
    // -------------------------------------------------------
    public void open(Player p, String category, String metric, int page, List<LBEntry> list) {

        int perPage = 28;
        int maxPage = (int) Math.ceil(list.size() / (double) perPage);
        if (maxPage == 0) maxPage = 1;
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(null, 54,
                "§8Leaderboard: §e" + category + " " + metric + " §7(Page " + page + ")");

        // Outline
        for (int i = 0; i < 54; i++) {

            // Don't overwrite prev/next page buttons
            if (i == 45 || i == 53) continue;

            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, outline());
            }
        }

        // Categories
        inv.setItem(0, categoryItem("BridgeFight", Material.STONE_SWORD, "§7View BridgeFight leaderboards."));
        inv.setItem(8, categoryItem("BuildFFA", Material.WOOL, 1, (short) 14, "§7View BuildFFA leaderboards."));

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

        // Fill leaderboard
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, list.size());

        int[] slots = {
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43
        };

        int si = 0;
        for (int i = start; i < end; i++) {

            LBEntry e = list.get(i);

            // --- USE NEW SAFE HEAD ---
            ItemStack skull = HeadUtils.getHead(e.uuid, e.name, e.value, i + 1);

            inv.setItem(slots[si], skull);
            si++;
        }

        // Fill empty remaining slots with '?' heads
        while (si < slots.length) {
            inv.setItem(slots[si], HeadUtils.getHead(null, null, 0, si + 1));
            si++;
        }

        p.openInventory(inv);
    }
}
