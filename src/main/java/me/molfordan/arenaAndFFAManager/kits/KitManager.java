package me.molfordan.arenaAndFFAManager.kits;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.Bukkit;
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
        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {
            forceArmorUpdate(player);
        },2);

    }

    public void applyBuildFFAKit(Player player) {
        clearInventory(player);

        Map<Integer, String> layout = hotbarDataManager.load(player.getUniqueId());

        buildFFAKit.giveKit(player, layout); // gives swords, pearls, pots, etc.

        //giveBlocks(player, 64);
        forceArmorUpdate(player);
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

    private void forceArmorUpdate(Player player) {
        Bukkit.getScheduler().runTask(ArenaAndFFAManager.getPlugin(), () -> {
            player.updateInventory(); // refresh client inventory
            player.getInventory().setArmorContents(player.getInventory().getArmorContents());
        });
    }


}
