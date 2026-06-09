package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.ArenaManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener to prevent players from removing armor by hand or inventory manipulation
 */
public class ArmorRemovalListener implements Listener {

    private final BridgeFightPlugin plugin;
    private final ArenaManager arenaManager;

    public ArmorRemovalListener(BridgeFightPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    /**
     * Prevent clicking on armor slots in inventory to remove armor
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;




        
        Player player = (Player) event.getWhoClicked();

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("bf")) return;
        
        // Only apply restriction in arenas
        if (!arenaManager.isInArenaIgnoreY(player)) return;
        
        // Check if clicking on armor slots
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            plugin.debug("Armor removal prevented for " + player.getName() + " (armor slot click)");
            return;
        }
        
        // Prevent shift-clicking armor into inventory from armor slots
        if (event.isShiftClick() && event.getCurrentItem() != null && isArmor(event.getCurrentItem().getType())) {
            // Check if the item is currently in an armor slot
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                plugin.debug("Armor shift-click removal prevented for " + player.getName());
            }
        }
    }

    /**
     * Prevent right-clicking armor with armor piece to equip/unequip
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Only apply restriction in arenas
        if (!arenaManager.isInArenaIgnoreY(player)) return;
        
        // Check if player is right-clicking with armor piece
        if (event.getAction() == Action.RIGHT_CLICK_AIR ||
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            ItemStack item = player.getItemInHand();
            if (item != null && isArmor(item.getType())) {
                // Cancel the event to prevent automatic armor equipping
                // This also prevents unequipping when right-clicking existing armor
                event.setCancelled(true);
                plugin.debug("Armor interact prevented for " + player.getName());
            }
        }
    }

    /**
     * Prevent hotbar switching to armor slots that might allow removal
     */
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // Only apply restriction in arenas
        if (!arenaManager.isInArenaIgnoreY(player)) return;
        
        // This is a basic prevention - more sophisticated checks might be needed
        // depending on server setup and plugins
        int newSlot = event.getNewSlot();
        ItemStack item = player.getInventory().getItem(newSlot);
        
        if (item != null && isArmor(item.getType())) {
            // Allow holding armor in hotbar but prevent certain interactions
            // The main prevention happens in InventoryClickEvent
            plugin.debug("Player " + player.getName() + " switched to armor slot " + newSlot);
        }
    }

    /**
     * Check if a material is an armor piece
     */
    private boolean isArmor(Material material) {
        switch (material) {
            // Helmet materials
            case LEATHER_HELMET:
            case CHAINMAIL_HELMET:
            case IRON_HELMET:
            case DIAMOND_HELMET:
            case GOLD_HELMET:
            
            // Chestplate materials
            case LEATHER_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case IRON_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
            case GOLD_CHESTPLATE:
            
            // Leggings materials
            case LEATHER_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case IRON_LEGGINGS:
            case DIAMOND_LEGGINGS:
            case GOLD_LEGGINGS:
            
            // Boots materials
            case LEATHER_BOOTS:
            case CHAINMAIL_BOOTS:
            case IRON_BOOTS:
            case DIAMOND_BOOTS:
            case GOLD_BOOTS:
            
            // Additional armor types (if any)
            case PUMPKIN:
                return true;
            default:
                return false;
        }
    }
}
