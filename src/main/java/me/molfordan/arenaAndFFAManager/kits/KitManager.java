package me.molfordan.arenaAndFFAManager.kits;

import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class KitManager {

    private final BridgeFightKit bridgeFightKit;
    private final BuildFFAKit buildFFAKit;
    private final HotbarDataManager hotbarDataManager;

    public KitManager(HotbarDataManager hotbarDataManager) {
        this.hotbarDataManager = hotbarDataManager;
        this.bridgeFightKit = new BridgeFightKit();
        this.buildFFAKit = new BuildFFAKit();
    }

    public void applyBridgeFightKit(Player player) {
        clearInventory(player);
        bridgeFightKit.giveKit(player);
    }

    public void applyBuildFFAKit(Player player) {
        clearInventory(player);

        Map<Integer, String> layout = hotbarDataManager.load(player.getUniqueId());

        buildFFAKit.giveKit(player, layout); // gives swords, pearls, pots, etc.

        //giveBlocks(player, 64);
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        // Clear back-inventory to avoid leftover blocks
        for (int i = 9; i <= 35; i++) {
            player.getInventory().setItem(i, null);
        }
    }

    public Map<String, ItemStack> getFFAKitItems() {
        return buildFFAKit.getKitItems();
    }


}
