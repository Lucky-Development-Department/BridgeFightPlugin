package me.molfordan.bridgefightplugin.hotbarmanager;

import me.molfordan.bridgefightplugin.manager.HotbarDataManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HotbarSorter {

    private final HotbarDataManager dataManager;

    public HotbarSorter(HotbarDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void sortToHotbar(Player p, ItemStack item) {

        // 1. load player layout
        Map<Integer, String> layout = dataManager.load(p.getUniqueId());
        if (layout == null || layout.isEmpty()) return;

        // 2. detect category
        String cat = HotbarManager.getCategoryFromItem(item);
        if (cat == null) return; // not a tracked category → ignore

        // 3. find the slot where this category should go
        int target = -1;
        for (Map.Entry<Integer, String> e : layout.entrySet()) {
            if (e.getValue().equalsIgnoreCase(cat)) {
                target = e.getKey();
                break;
            }
        }

        // no slot for this category → ignore
        if (target < 0 || target > 8) return;

        // 4. place item into target slot (with stacking)
        ItemStack existing = p.getInventory().getItem(target);
        if (existing != null && existing.isSimilar(item)) {
            int max = existing.getMaxStackSize();
            int current = existing.getAmount();
            int toAdd = item.getAmount();
            
            if (current + toAdd <= max) {
                existing.setAmount(current + toAdd);
                p.getInventory().setItem(target, existing);
            } else {
                existing.setAmount(max);
                p.getInventory().setItem(target, existing);
                // Overflow back to inventory or drop? 
                // For simplicity, just cap it.
            }
        } else {
            p.getInventory().setItem(target, item);
        }
    }
}
