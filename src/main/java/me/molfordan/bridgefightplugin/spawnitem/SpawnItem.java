package me.molfordan.bridgefightplugin.spawnitem;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
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

    private final BridgeFightPlugin plugin;
    private final String GUI_TITLE = "Select Mode";
    private final String PLATGUI_TITLE = "Select Platform";

    public SpawnItem(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void giveSpawnItem(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.getInventory().setItem(0, getCompass());
        player.getInventory().setItem(4, getStatsHead(player));
        player.getInventory().setItem(8, getKitEditorBook());
        player.getInventory().setItem(1, getKitEditorPaper());
        player.getInventory().setItem(2, getQueueSword());
    }

    public void giveSword(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(0, getSword());
    }

    private ItemStack getCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eMode Selector §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getBook(){
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eSelect Kit §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getSword() {
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
        meta.setDisplayName("§ePlatform Selector §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getSpawnRedstone() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cBack to Spawn §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    public void giveBridgeFightSpawnItem(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.getInventory().setItem(0, getPlatformSelectorCompass());
        player.getInventory().setItem(8, getSpawnRedstone());
        player.getInventory().setItem(4, getBook());
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
        meta.setDisplayName("§bHotbar Manager §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getKitEditorPaper() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bBedFight Kit Editor §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getQueueSword() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aQueue §7(Right Click)");
        item.setItemMeta(meta);
        return item;
    }

    private Inventory getCompassGUI() {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);

        ItemStack bf = new ItemStack(Material.STONE_SWORD);
        ItemMeta bfMeta = bf.getItemMeta();
        bfMeta.setDisplayName("§cBridgeFight");
        bfMeta.setLore(java.util.Arrays.asList(
                "§aClick to join",
                "§7Players: " + plugin.getConfigManager().getPlayersInWorld("bridgefight")
        ));
        bfMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        bfMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bf.setItemMeta(bfMeta);
        inv.setItem(3, bf);

        ItemStack bffa = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta bffaMeta = bffa.getItemMeta();
        bffaMeta.setDisplayName("§cBuildFFA");
        bffaMeta.setLore(java.util.Arrays.asList(
                "§aClick to join",
                "§7Players: " + plugin.getConfigManager().getPlayersInWorld("buildffa")
        ));
        bffaMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        bffaMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bffa.setItemMeta(bffaMeta);
        inv.setItem(5, bffa);

        return inv;
    }

    private Inventory getPlatGUI() {
        Inventory inv = Bukkit.createInventory(null, 27, PLATGUI_TITLE);

        ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 18; i < 27; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);

        Map<String, PlatformRegion> platforms = plugin.getPlatformManager().getAllPlatforms();

        java.util.List<String> sorted = new java.util.ArrayList<>(platforms.keySet());
        sorted.sort(String::compareToIgnoreCase);

        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25};
        int index = 0;

        for (String key : sorted) {
            if (index >= slots.length) break;

            PlatformRegion region = platforms.get(key);
            boolean hasSpawn = region.getSpawn() != null;

            String formatted = formatPlatformName(key);

            ItemStack item = new ItemStack(Material.WOOL, 1, hasSpawn ? (short) 5 : (short) 14);
            ItemMeta im = item.getItemMeta();
            im.setDisplayName("§e" + formatted);
            im.setLore(java.util.Arrays.asList(
                    hasSpawn ? "§aSpawn set" : "§cSpawn not set",
                    "§7Click to teleport"
            ));
            item.setItemMeta(im);

            inv.setItem(slots[index], item);
            index++;
        }

        return inv;
    }

    private String formatPlatformName(String raw) {
        raw = raw.toUpperCase();

        String number = "";
        int i = raw.length() - 1;

        while (i >= 0 && Character.isDigit(raw.charAt(i))) {
            number = raw.charAt(i) + number;
            i--;
        }

        String base = raw.substring(0, raw.length() - number.length());

        switch (base) {
            case "PLAT": return "Platform " + number;
            case "BIGPLAT": return "Big Platform " + number;
            case "SMALLPLAT": return "Small Platform " + number;
            case "BOXINGPLAT": return "Boxing Platform " + number;
        }

        return raw;
    }

    private String unformatPlatformName(String formatted) {
        formatted = formatted.replace("§e", "").trim();

        String[] parts = formatted.split(" ");
        if (parts.length < 2) return null;

        String number = parts[parts.length - 1];

        if (formatted.startsWith("Big Platform")) {
            return "bigplat" + number;
        } else if (formatted.startsWith("Small Platform")) {
            return "smallplat" + number;
        } else if (formatted.startsWith("Boxing Platform")) {
            return "boxingplat" + number;
        } else if (formatted.startsWith("Platform")) {
            return "plat" + number;
        }

        return null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();

        if (item.getType() == Material.COMPASS && name.startsWith("§eMode Selector")) {
            event.setCancelled(true);
            player.openInventory(getCompassGUI());
        }
        else if (item.getType() == Material.SKULL_ITEM && name.startsWith("§a" + player.getName() + "'s Stats")) {
            event.setCancelled(true);
            player.performCommand("guistats");
        }
        else if (item.getType() == Material.BOOK && name.startsWith("§bHotbar Manager")) {
            event.setCancelled(true);
            player.performCommand("hotbarmanager");
        }
        else if (item.getType() == Material.PAPER && name.startsWith("§bBedFight Kit Editor §7(Right Click)")) {
            event.setCancelled(true);
            player.performCommand("kiteditor");
        }
        else if (item.getType() == Material.IRON_SWORD && name.startsWith("§aQueue §7(Right Click)")) {
            event.setCancelled(true);
            player.performCommand("queue");
        }
        else if (item.getType() == Material.COMPASS && name.startsWith("§ePlatform Selector")) {
            event.setCancelled(true);
            player.openInventory(getPlatGUI());
        }
        else if (item.getType() == Material.REDSTONE && name.startsWith("§cBack to Spawn")) {
            event.setCancelled(true);
            player.performCommand("spawn");
        } else if (item.getType() == Material.BOOK && name.startsWith("§eSelect Kit")) {

            event.setCancelled(true);
            plugin.getBridgeFightGUI().open(player);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title == null) return;

        if (title.equals(GUI_TITLE)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();

            if (name.contains("BridgeFight")) {
                player.closeInventory();
                player.performCommand("bridgefight");
            }
            else if (name.contains("BuildFFA")) {
                player.closeInventory();
                player.performCommand("buildffa");
            }

            return;
        }

        if (title.equals(PLATGUI_TITLE)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();
            if (name.trim().isEmpty()) return;

            String plat = unformatPlatformName(name);
            if (plat == null) return;

            player.closeInventory();
            player.performCommand(plat);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().startsWith("bf_")) return;

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
        if (type == null) return;

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (type) {
                case MODE_SELECTOR: player.getInventory().setItem(0, getCompass()); break;
                case PLATFORM_SELECTOR: player.getInventory().setItem(0, getPlatformSelectorCompass()); break;
                case STATS_HEAD: player.getInventory().setItem(4, getStatsHead(player)); break;
                case HOTBAR_MANAGER: player.getInventory().setItem(8, getKitEditorBook()); break;
                case KIT_EDITOR: player.getInventory().setItem(1, getKitEditorPaper()); break;
                case QUEUE_ITEM: player.getInventory().setItem(2, getQueueSword()); break;
                case BACK_TO_SPAWN: player.getInventory().setItem(8, getSpawnRedstone()); break;
                case SELECT_KIT: player.getInventory().setItem(4, getBook()); break;
            }
            player.updateInventory();
        });
    }

    private LockedItemType getLockedItemType(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getDisplayName();

        if (item.getType() == Material.COMPASS &&  name.startsWith("§eMode Selector")) return LockedItemType.MODE_SELECTOR;
        if (item.getType() == Material.COMPASS &&  name.startsWith("§ePlatform Selector")) return LockedItemType.PLATFORM_SELECTOR;
        if (item.getType() == Material.SKULL_ITEM && name.startsWith("§a" + player.getName() + "'s Stats")) return LockedItemType.STATS_HEAD;
        if (item.getType() == Material.BOOK &&  name.startsWith("§bHotbar Manager")) return LockedItemType.HOTBAR_MANAGER;
        if (item.getType() == Material.PAPER &&  name.startsWith("§bBedFight Kit Editor §7(Right Click)")) return LockedItemType.KIT_EDITOR;
        if (item.getType() == Material.IRON_SWORD && name.startsWith("§aQueue §7(Right Click)")) return LockedItemType.QUEUE_ITEM;
        if (item.getType() == Material.REDSTONE && name.startsWith("§cBack to Spawn")) return LockedItemType.BACK_TO_SPAWN;
        if (item.getType() == Material.BOOK && name.startsWith("§eSelect Kit")) return LockedItemType.SELECT_KIT;

        return null;
    }

    private enum LockedItemType {
        MODE_SELECTOR,
        PLATFORM_SELECTOR,
        STATS_HEAD,
        HOTBAR_MANAGER,
        KIT_EDITOR,
        QUEUE_ITEM,
        BACK_TO_SPAWN,
        SELECT_KIT
    }
}
