package me.molfordan.arenaAndFFAManager.hotbarmanager;

import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
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

    public boolean sort(Player player, ItemStack block) {
        if (block == null) return false;

        Map<Integer, String> layout = dataManager.load(player.getUniqueId());
        if (layout == null || layout.isEmpty()) return false;

        List<Integer> blockSlots = new ArrayList<>();
        for (Map.Entry<Integer, String> e : layout.entrySet()) {
            if ("blocks".equalsIgnoreCase(e.getValue())) {
                int slot = e.getKey();
                if (slot >= 0 && slot <= 8) blockSlots.add(slot);
            }
        }

        if (blockSlots.isEmpty()) return false;

        // Always add exactly +1 wool
        ItemStack incoming = new ItemStack(block.getType(), 1, block.getDurability());

        // Try stacking
        for (int slot : blockSlots) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing != null && existing.isSimilar(incoming)) {
                if (existing.getAmount() < existing.getMaxStackSize()) {
                    existing.setAmount(existing.getAmount() + 1);
                    player.getInventory().setItem(slot, existing);
                    return true;
                }
            }
        }

        // Try placing in empty block slot
        for (int slot : blockSlots) {
            if (player.getInventory().getItem(slot) == null) {
                player.getInventory().setItem(slot, incoming);
                return true;
            }
        }

        // Could not place → overflow must be handled by caller
        return false;
    }

}
