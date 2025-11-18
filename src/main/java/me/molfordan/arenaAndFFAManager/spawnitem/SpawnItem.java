package me.molfordan.arenaAndFFAManager.spawnitem;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class SpawnItem implements Listener {

    private final ArenaAndFFAManager plugin;
    private final String GUI_TITLE = "Select Mode";

    public SpawnItem(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ============================================================
    //  PUBLIC METHOD TO GIVE SPAWN ITEMS
    // ============================================================
    public void giveItems(Player player) {

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);



        player.getInventory().setItem(0, getCompass());
        player.getInventory().setItem(4, getStatsHead(player));
        player.getInventory().setItem(8, getKitEditorBook());
    }

    // ============================================================
    //  ITEM CREATION
    // ============================================================
    private ItemStack getCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eMode Selector");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getStatsHead(Player player) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player.getName());
        meta.setDisplayName("§a" + player.getName() + "'s Stats");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getKitEditorBook() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bHotbar Manager");
        item.setItemMeta(meta);
        return item;
    }

    // ============================================================
    //  COMPASS GUI CREATOR
    // ============================================================
    private Inventory getCompassGUI() {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);

        // Stone Sword (BridgeFight)
        ItemStack bf = new ItemStack(Material.STONE_SWORD);
        ItemMeta bfMeta = bf.getItemMeta();
        bfMeta.setDisplayName("§cBridgeFight");
        bf.setItemMeta(bfMeta);
        inv.setItem(3, bf);

        // Red Wool (BuildFFA)
        ItemStack bffa = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta bffaMeta = bffa.getItemMeta();
        bffaMeta.setDisplayName("§cBuildFFA");
        bffa.setItemMeta(bffaMeta);
        inv.setItem(5, bffa);

        return inv;
    }

    // ============================================================
    //  LISTENERS
    // ============================================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // COMPASS → OPEN GUI
        if (item.getType() == Material.COMPASS) {
            event.setCancelled(true);
            player.openInventory(getCompassGUI());
        }

        // STATS HEAD → /guistats
        else if (item.getType() == Material.SKULL_ITEM) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                event.setCancelled(true);
                player.performCommand("guistats");
            }
        }

        // BOOK → /hotbarmanager
        else if (item.getType() == Material.BOOK) {
            event.setCancelled(true);
            player.performCommand("hotbarmanager");
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        if (!clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("BridgeFight")) {
            player.closeInventory();
            player.performCommand("bridgefight");

        } else if (name.contains("BuildFFA")) {
            player.closeInventory();
            player.performCommand("buildffa");
        }
    }
}
