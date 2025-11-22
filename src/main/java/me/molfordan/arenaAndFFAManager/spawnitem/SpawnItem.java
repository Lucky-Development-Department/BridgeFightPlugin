package me.molfordan.arenaAndFFAManager.spawnitem;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;

public class SpawnItem implements Listener {

    private final ArenaAndFFAManager plugin;
    private final String GUI_TITLE = "Select Mode";
    private final String PLATGUI_TITLE = "Select Platform";

    public SpawnItem(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ============================================================
    //  PUBLIC METHOD TO GIVE SPAWN ITEMS
    // ============================================================
    public void giveSpawnItem(Player player) {

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);



        player.getInventory().setItem(0, getCompass());
        player.getInventory().setItem(4, getStatsHead(player));
        player.getInventory().setItem(8, getKitEditorBook());
    }

    public void giveSword(Player player){
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(0, getSword());
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

    private ItemStack getSword(){
        ItemStack item = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);

        return item;

    }

    private ItemStack getPlatformSelectorCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§ePlatform Selector");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getSpawnRedstone() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cBack to Spawn");
        item.setItemMeta(meta);
        return item;
    }

    public void giveBridgeFightSpawnItem(Player player) {

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Slot 0 = Platform Selector
        player.getInventory().setItem(0, getPlatformSelectorCompass());

        // Slot 8 = /spawn command item
        player.getInventory().setItem(8, getSpawnRedstone());
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
        bfMeta.setLore(java.util.Arrays.asList("§aClick to join"));
        bfMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        bfMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bf.setItemMeta(bfMeta);
        inv.setItem(3, bf);

        // Red Wool (BuildFFA)
        ItemStack bffa = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta bffaMeta = bffa.getItemMeta();
        bffaMeta.setDisplayName("§cBuildFFA");
        bffaMeta.setLore(java.util.Arrays.asList("§aClick to join"));
        bffaMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        bffaMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bffa.setItemMeta(bffaMeta);
        inv.setItem(5, bffa);

        return inv;
    }

    private Inventory getPlatGUI() {
        Inventory inv = Bukkit.createInventory(null, 27, PLATGUI_TITLE);

        // =============================
        // Create Black Glass Border
        // =============================
        ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);

        // Top row (0–8)
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }

        // Bottom row (18–26)
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Middle left & right
        inv.setItem(9, border);
        inv.setItem(17, border);

        // =============================
        // Get platforms + sort
        // =============================
        Map<String, PlatformRegion> platforms = plugin.getPlatformManager().getAllPlatforms();

        java.util.List<String> sortedNames = new java.util.ArrayList<>(platforms.keySet());
        sortedNames.sort(String::compareToIgnoreCase); // <-- SORT HERE

        // =============================
        // Fill inner GUI slots 10–16 & 19–25
        // =============================
        int[] fillSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25};
        int index = 0;

        for (String platName : sortedNames) {
            if (index >= fillSlots.length) break;

            PlatformRegion region = platforms.get(platName);
            boolean hasSpawn = region.getSpawn() != null;

            ItemStack item = new ItemStack(Material.WOOL, 1, hasSpawn ? (short) 5 : (short) 14);
            ItemMeta im = item.getItemMeta();

            im.setDisplayName("§e" + platName.toUpperCase());
            im.setLore(java.util.Arrays.asList(
                    hasSpawn ? "§aSpawn set" : "§cSpawn not set",
                    "§7Click to teleport"
            ));
            item.setItemMeta(im);

            inv.setItem(fillSlots[index], item);
            index++;
        }

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

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String displayName = item.getItemMeta().getDisplayName();

        // COMPASS → OPEN GUI
        if (item.getType() == Material.COMPASS && displayName.equals("§eMode Selector")) {
            event.setCancelled(true);
            player.openInventory(getCompassGUI());
        }
        // STATS HEAD → /guistats
        else if (item.getType() == Material.SKULL_ITEM && displayName.equals("§a" + player.getName() + "'s Stats")) {
            event.setCancelled(true);
            player.performCommand("guistats");
        }
        // BOOK → /hotbarmanager
        else if (item.getType() == Material.BOOK && displayName.equals("§bHotbar Manager")) {
            event.setCancelled(true);
            player.performCommand("hotbarmanager");
        }

        else if (item.getType() == Material.COMPASS &&
                displayName.equals("§ePlatform Selector")) {

            event.setCancelled(true);
            player.openInventory(getPlatGUI());
        }

        else if (item.getType() == Material.REDSTONE &&
                displayName.equals("§cBack to Spawn")) {

            event.setCancelled(true);
            player.performCommand("spawn");
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        // ensure it's a player
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Title null-safety
        String title = event.getView().getTitle();
        if (title == null) return;

        // ----------------------------
        // Mode selector GUI (9 slots)
        // ----------------------------
        if (title.equals(GUI_TITLE)) {
            event.setCancelled(true); // always cancel interactions inside the GUI

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();
            if (name == null) return;

            if (name.contains("BridgeFight")) {
                player.closeInventory();
                player.performCommand("bridgefight");
            } else if (name.contains("BuildFFA")) {
                player.closeInventory();
                player.performCommand("buildffa");
            }

            return;
        }

        // ----------------------------
        // Platform selector GUI (27 slots)
        // ----------------------------
        if (title.equals(PLATGUI_TITLE)) {
            event.setCancelled(true); // prevent item moving inside GUI

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();
            if (name == null || name.trim().isEmpty()) return; // ignore border panes with blank name

            // name example: "§ePLAT1" -> convert to "plat1"
            // This uses the exact §e prefix you use when creating the item.
            String plat = name.replace("§e", "").toLowerCase();

            player.closeInventory();
            player.performCommand(plat);
        }
    }


    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (getLockedItemType(item, player) != null) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onSwap(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getClick() == ClickType.NUMBER_KEY) {
            Player player = (Player) event.getWhoClicked();
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            ItemStack clicked = event.getCurrentItem();

            if (getLockedItemType(hotbarItem, player) != null ||
                    getLockedItemType(clicked, player) != null) {

                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }


    @EventHandler
    public void onInventoryClickProtect(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        LockedItemType type = getLockedItemType(item, player);
        if (type == null) return; // Not a locked item

        event.setCancelled(true); // STOP movement

        // Restore correct slot
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (type) {
                case MODE_SELECTOR:
                    player.getInventory().setItem(0, getCompass());
                    break;
                case PLATFORM_SELECTOR:
                    player.getInventory().setItem(0, getPlatformSelectorCompass());
                    break;
                case STATS_HEAD:
                    player.getInventory().setItem(4, getStatsHead(player));
                    break;
                case HOTBAR_MANAGER:
                    player.getInventory().setItem(8, getKitEditorBook());
                    break;
                case BACK_TO_SPAWN:
                    player.getInventory().setItem(8, getSpawnRedstone());
                    break;
            }
            player.updateInventory();
        });
    }

    private LockedItemType getLockedItemType(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getDisplayName();

        if (item.getType() == Material.COMPASS && name.equals("§eMode Selector"))
            return LockedItemType.MODE_SELECTOR;

        if (item.getType() == Material.COMPASS && name.equals("§ePlatform Selector"))
            return LockedItemType.PLATFORM_SELECTOR;

        if (item.getType() == Material.SKULL_ITEM && name.equals("§a" + player.getName() + "'s Stats"))
            return LockedItemType.STATS_HEAD;

        if (item.getType() == Material.BOOK && name.equals("§bHotbar Manager"))
            return LockedItemType.HOTBAR_MANAGER;

        if (item.getType() == Material.REDSTONE && name.equals("§cBack to Spawn"))
            return LockedItemType.BACK_TO_SPAWN;

        return null;
    }

    private enum LockedItemType {
        MODE_SELECTOR,
        PLATFORM_SELECTOR,
        STATS_HEAD,
        HOTBAR_MANAGER,
        BACK_TO_SPAWN
    }
}
