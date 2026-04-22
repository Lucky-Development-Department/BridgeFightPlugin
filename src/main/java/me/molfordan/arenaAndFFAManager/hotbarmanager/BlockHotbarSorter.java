package me.molfordan.arenaAndFFAManager.hotbarmanager;

import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockHotbarSorter {

    private final HotbarDataManager dataManager;

    public BlockHotbarSorter(HotbarDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public boolean sort(Player player, ItemStack item) {
        if (item == null) return false;

        Map<Integer, String> layout = dataManager.load(player.getUniqueId());
        if (layout == null || layout.isEmpty()) return false;

        String category = "blocks";
        if (item.getType() == Material.LADDER) {
            category = "ladder";
            // Check if player actually has a ladder slot in their layout
            boolean hasLadderSlot = false;
            for (String v : layout.values()) {
                if ("ladder".equalsIgnoreCase(v)) {
                    hasLadderSlot = true;
                    break;
                }
            }
            // If no ladder slot, fall back to blocks category
            if (!hasLadderSlot) {
                category = "blocks";
            }
        }

        List<Integer> targetSlots = new ArrayList<>();
        for (Map.Entry<Integer, String> e : layout.entrySet()) {
            if (category.equalsIgnoreCase(e.getValue())) {
                int slot = e.getKey();
                if (slot >= 0 && slot <= 8) targetSlots.add(slot);
            }
        }

        if (targetSlots.isEmpty()) return false;

        // Always add exactly +1
        // For ladders, we strip the data (which is facing) so it stacks with kit ladders
        short data = item.getDurability();
        if (item.getType() == Material.LADDER) {
            data = 0;
        }
        ItemStack incoming = new ItemStack(item.getType(), 1, data);

        // Try stacking
        for (int slot : targetSlots) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing != null && existing.isSimilar(incoming)) {
                if (existing.getAmount() < existing.getMaxStackSize()) {
                    existing.setAmount(existing.getAmount() + 1);
                    player.getInventory().setItem(slot, existing);
                    return true;
                }
            }
        }

        // Try placing in empty target slot
        for (int slot : targetSlots) {
            if (player.getInventory().getItem(slot) == null) {
                player.getInventory().setItem(slot, incoming);
                return true;
            }
        }

        // Could not place → overflow must be handled by caller
        return false;
    }

}
