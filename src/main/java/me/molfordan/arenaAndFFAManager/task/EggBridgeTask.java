package me.molfordan.arenaAndFFAManager.task;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class EggBridgeTask implements Runnable {

    private final Player player;
    private final Egg egg;
    private final Plugin plugin;
    private final byte woolColor;

    private final BukkitTask task;
    private final List<Block> placedBlocks = new ArrayList<>();

    private boolean removalScheduled = false;

    public EggBridgeTask(Player player, Egg egg, Plugin plugin) {
        this.player = player;
        this.egg = egg;
        this.plugin = plugin;
        this.woolColor = getWoolColor(player);

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 0L, 1L);
    }

    private byte getWoolColor(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.getType() == Material.LEATHER_HELMET) {
            LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
            Color color = meta.getColor();
            if (color.equals(Color.RED)) {
                return 14; // Red wool
            } else if (color.equals(Color.LIME)) {
                return 5; // Lime wool
            }
        }
        return 0; // Default to white if no color found
    }

    @Override
    public void run() {

        if (egg.isDead() || !egg.isValid()) {
            cancel();
            return;
        }

        Location loc = egg.getLocation();

        // Stop task if egg is too far away
        if (player.getLocation().distance(loc) > 30) {
            cancel();
            return;
        }

        placeBlock(loc.clone().subtract(0, 2, 0));
        placeBlock(loc.clone().subtract(1, 2, 0));
        placeBlock(loc.clone().subtract(0, 2, 1));
    }

    private void placeBlock(Location location) {
        Block block = location.getBlock();

        if (block.getType() == Material.AIR) {
            block.setType(Material.WOOL);
            block.setData(woolColor);
            block.setMetadata("egg_bridge_block", new FixedMetadataValue(plugin, true));
            placedBlocks.add(block);
        }
    }

    private void cancel() {
        task.cancel();

        if (removalScheduled) return;
        removalScheduled = true;

        // Start removing after 10 seconds
        Bukkit.getScheduler().runTaskLater(
                plugin,
                this::startBridgeRemoval,
                20L * 10
        );
    }

    private void startBridgeRemoval() {

        new BukkitRunnable() {

            int index = 0;

            @Override
            public void run() {

                if (index >= placedBlocks.size()) {
                    cancel();
                    return;
                }

                Block block = placedBlocks.get(index);

                if (block != null && block.getType() == Material.WOOL) {
                    block.setType(Material.AIR);
                }

                index++;
            }

        }.runTaskTimer(plugin, 0L, 1L); // remove 1 block every 1 tick
    }
}
