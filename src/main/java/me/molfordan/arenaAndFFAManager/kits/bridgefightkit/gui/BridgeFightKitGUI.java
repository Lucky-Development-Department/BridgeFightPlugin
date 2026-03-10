package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BridgeFightKitManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit2;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BridgeFightKitGUI {

    private final ArenaAndFFAManager plugin;
    private final BridgeFightKitManager kitManager;

    public BridgeFightKitGUI(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getBridgeFightKitManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Select BridgeFight Kit");

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        int kills = stats != null ? stats.getBridgeKills() : 0;

        // Sort kits
        List<Kit2> sorted = new ArrayList<>(kitManager.getAllKits());
        sorted.sort(Comparator.comparingInt(Kit2::getSort));

        // Slots that CAN be used (everything except the restricted ones)
        Set<Integer> blocked = new HashSet<>(Arrays.asList(
                0,1,2,3,4,5,6,7,8,
                9, 17,
                18, 26,
                27, 35,
                36,
                44,45,46,47,48,49,50,51,52,53
        ));

        List<Integer> allowedSlots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (!blocked.contains(i)) {
                allowedSlots.add(i);
            }
        }

        int index = 0;
        for (Kit2 kit : sorted) {
            if (index >= allowedSlots.size()) break;

            int slot = allowedSlots.get(index++);
            boolean unlocked = kills >= kit.getRequiredKills();

            ItemStack icon = new ItemStack(unlocked ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
            ItemMeta meta = icon.getItemMeta();

            meta.setDisplayName((unlocked ? "§a" : "§c") + kit.getDisplayName());
            meta.setLore(Arrays.asList(
                    "§7Kills Required: §e" + kit.getRequiredKills(),
                    unlocked ? "§aUnlocked" : "§cLocked",
                    "",
                    "§8ID: " + kit.getName()
            ));
            icon.setItemMeta(meta);

            inv.setItem(slot, icon);
        }

        player.openInventory(inv);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return;

        List<String> lore = meta.getLore();
        String realId = null;

        for (String line : lore) {
            if (line.startsWith("§8ID: ")) {
                realId = line.substring("§8ID: ".length());  // extract real ID
                break;
            }
        }

        if (realId == null) return;

        Kit2 kit = kitManager.get(realId);
        if (kit == null) return;

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        int kills = stats != null ? stats.getBridgeKills() : 0;

        if (kills < kit.getRequiredKills()) {
            player.sendMessage("§cYou do not meet the requirements for this kit!");
            return;
        }

        plugin.getKitManager().setSelectedBridgeFightKit(player.getUniqueId(), kit.getName());
        
        // Save last selected kit to database
        if (stats != null) {
            stats.setLastSelectedBridgeKit(kit.getName());
            plugin.getStatsManager().savePlayer(stats);
        }
        
        player.sendMessage("§aEquipped kit: " + kit.getDisplayName());
        player.closeInventory();
    }

    public void openEdit(Player player, Kit2 kit) {
        Inventory inv = Bukkit.createInventory(null, 27, "Edit Kit");

        // === Slot 11 — Rename Kit ===
        ItemStack rename = new ItemStack(Material.NAME_TAG);
        ItemMeta renameMeta = rename.getItemMeta();
        renameMeta.setDisplayName("§bRename Kit");
        renameMeta.setLore(Arrays.asList(
                "§7Current: §f" + kit.getName(),
                "",
                "§eClick to rename"
        ));
        rename.setItemMeta(renameMeta);
        inv.setItem(11, rename);

        // === Slot 13 — Edit Contents ===
        ItemStack contents = new ItemStack(Material.CHEST);
        ItemMeta contentsMeta = contents.getItemMeta();
        contentsMeta.setDisplayName("§bEdit Items");
        contentsMeta.setLore(Arrays.asList(
                "§7Modify all kit items.",
                "",
                "§eClick to edit"
        ));
        contents.setItemMeta(contentsMeta);
        inv.setItem(13, contents);

        // === Slot 15 — Delete Kit ===
        ItemStack delete = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.setDisplayName("§cDelete Kit");
        deleteMeta.setLore(Arrays.asList(
                "§7This will remove:",
                "§f" + kit.getName(),
                "",
                "§cCannot be undone!",
                "§eClick to delete"
        ));
        delete.setItemMeta(deleteMeta);
        inv.setItem(15, delete);

        // === Back Button ===
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§eBack");
        back.setItemMeta(backMeta);
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    private final Map<UUID, Kit2> editing = new HashMap<>();
    private final Map<UUID, Kit2> pendingRename = new HashMap<>();

    public void setEditingKit(UUID player, Kit2 kit) {
        editing.put(player, kit);
    }

    public Kit2 getEditingKit(UUID player) {
        return editing.get(player);
    }

    public void addPendingRename(UUID player, Kit2 kit) {
        pendingRename.put(player, kit);
    }

    public Kit2 getPendingRename(UUID player) {
        return pendingRename.get(player);
    }

}
