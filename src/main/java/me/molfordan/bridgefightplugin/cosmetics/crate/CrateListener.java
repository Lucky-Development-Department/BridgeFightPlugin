package me.molfordan.bridgefightplugin.cosmetics.crate;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CrateListener implements Listener {

    private final CrateManager crateManager;

    /**
     * Guards against scheduling duplicate reopen tasks.
     */
    private final Set<UUID> reopenPending = new HashSet<>();

    public CrateListener(CrateManager crateManager) {
        this.crateManager = crateManager;
    }

    // -----------------------------------------------------------------------
    // Block placement: register crate when the setup Ender Chest is placed
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.ENDER_CHEST) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        if (item.getItemMeta().getDisplayName().equals(SetupCrateCommand.SETUP_BLOCK_NAME)) {
            Block block = event.getBlockPlaced();
            crateManager.registerCrate(block.getLocation());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Crate setup successfully at your location!");
        }
    }

    // -----------------------------------------------------------------------
    // Block break: only ops / permission holders can remove a crate
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.ENDER_CHEST) return;

        Location match = crateManager.getMatchingCrateLocation(block.getLocation());
        if (match != null) {
            Player player = event.getPlayer();
            if (!player.isOp() && !player.hasPermission("bridgefight.setupcrate")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot break the Cosmetics Crate!");
                return;
            }
            crateManager.unregisterCrate(match);
            player.sendMessage(ChatColor.RED + "Cosmetics Crate has been removed.");
        }
    }

    // -----------------------------------------------------------------------
    // Right-click: open crate
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) return;

        Location match = crateManager.getMatchingCrateLocation(block.getLocation());
        if (match != null) {
            event.setCancelled(true);
            crateManager.startRoll(event.getPlayer(), match);
        }
    }

    // -----------------------------------------------------------------------
    // Inventory click: block all item movement inside the crate GUI
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(CrateManager.CRATE_GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Inventory close: re-open the GUI if a roll is still in progress.
    //
    // OOM safety:
    //   • Only ONE runTaskLater is scheduled per close attempt (not recursive).
    //   • The task checks activeRolls before acting; finishRoll() removes the
    //     UUID *before* calling closeInventory(), so the task is always a no-op
    //     by the time the legitimate close fires.
    //   • reopenPending prevents scheduling duplicate tasks if the close is spammed.
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(CrateManager.CRATE_GUI_TITLE)) return;

        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Not rolling — let it close normally
        if (!crateManager.getActiveRolls().contains(uuid)) return;

        // If a reopen is already scheduled, don't schedule another one
        if (reopenPending.contains(uuid)) return;

        // Capture the inventory reference *before* the close completes
        Inventory inv = event.getInventory();
        BridgeFightPlugin plugin = crateManager.getPlugin();

        reopenPending.add(uuid);

        // Schedule a single 1-tick task to reopen — no recursion, no loop
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reopenPending.remove(uuid);
            
            // Guard: roll may have legitimately finished while we were waiting
            if (!player.isOnline() || !crateManager.getActiveRolls().contains(uuid)) return;

            // If the player already has the crate GUI open, do not reopen
            if (player.getOpenInventory() != null && player.getOpenInventory().getTitle().equals(CrateManager.CRATE_GUI_TITLE)) {
                return;
            }

            player.openInventory(inv);
        }, 1L);
    }

    // -----------------------------------------------------------------------
    // Player quit: clean up activeRolls and award the cosmetic so it is
    // not lost. forceCompleteRoll is safe to call on an offline player
    // (it only touches Maps and stats, no inventory operations).
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        reopenPending.remove(uuid); // clean up pending set just in case
        if (crateManager.getActiveRolls().contains(uuid)) {
            crateManager.forceCompleteRoll(player);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        crateManager.loadCrates();
    }
}
