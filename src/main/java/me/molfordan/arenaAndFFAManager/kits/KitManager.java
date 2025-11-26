package me.molfordan.arenaAndFFAManager.kits;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BridgeFightKitManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.Kit;
import me.molfordan.arenaAndFFAManager.manager.HotbarDataManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KitManager {

    private final BridgeFightKit bridgeFightKit;
    private final BuildFFAKit buildFFAKit;
    private final HotbarDataManager hotbarDataManager;

    private final Map<UUID, String> selectedBridgeFightKit = new HashMap<>();

    public KitManager(HotbarDataManager hotbarDataManager) {
        this.hotbarDataManager = hotbarDataManager;
        this.bridgeFightKit = new BridgeFightKit();
        this.buildFFAKit = new BuildFFAKit();
    }

    public void applyBridgeFightKit(Player player) {
        clearInventory(player);
        BridgeFightKitManager manager = ArenaAndFFAManager.getPlugin().getBridgeFightKitManager();

        String kitName = selectedBridgeFightKit.getOrDefault(player.getUniqueId(), "Default");

        Kit kit = manager.get(kitName);
        if (kit != null) kit.apply(player);
        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {
            fixArmor(player);       // ensures final armor is correct
            sendArmorUpdate(player); // sends FINAL armor
        }, 5);

        /*

        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {
            System.out.println("FINAL ARMOR:");
            System.out.println("Helmet = " + player.getInventory().getHelmet());
            System.out.println("Chest  = " + player.getInventory().getChestplate());
            System.out.println("Legs   = " + player.getInventory().getLeggings());
            System.out.println("Boots  = " + player.getInventory().getBoots());
        }, 5);

         */

    }

    public void applyBuildFFAKit(Player player) {
        clearInventory(player);

        Map<Integer, String> layout = hotbarDataManager.load(player.getUniqueId());

        buildFFAKit.giveKit(player, layout); // gives swords, pearls, pots, etc.

        //giveBlocks(player, 64);
        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {
            fixArmor(player);       // ensures final armor is correct
            sendArmorUpdate(player);
        }, 5);

        /*

        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () -> {
            System.out.println("FINAL ARMOR:");
            System.out.println("Helmet = " + player.getInventory().getHelmet());
            System.out.println("Chest  = " + player.getInventory().getChestplate());
            System.out.println("Legs   = " + player.getInventory().getLeggings());
            System.out.println("Boots  = " + player.getInventory().getBoots());
        }, 5);

         */
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

    private void sendArmorUpdate(Player target) {
        int entityId = target.getEntityId();
        ItemStack[] armor = target.getInventory().getArmorContents();

        // armor[] order:
        // armor[0] = BOOTS
        // armor[1] = LEGS
        // armor[2] = CHEST
        // armor[3] = HELMET

        for (Player viewer : target.getWorld().getPlayers()) {
            if (viewer.equals(target)) continue;

            sendEquipment(viewer, entityId, 1, armor[0]); // BOOTS → slot 4
            sendEquipment(viewer, entityId, 2, armor[1]); // LEGS → slot 3
            sendEquipment(viewer, entityId, 3, armor[2]); // CHEST → slot 2
            sendEquipment(viewer, entityId, 4, armor[3]); // HELMET → slot 1
        }
    }

    private void sendEquipment(Player viewer, int entityId, int slot, ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        PacketPlayOutEntityEquipment packet =
                new PacketPlayOutEntityEquipment(entityId, slot, nms);

        ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(packet);
    }

    private void fixArmor(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        ItemStack chest = p.getInventory().getChestplate();
        ItemStack legs = p.getInventory().getLeggings();
        ItemStack boots = p.getInventory().getBoots();

        // reapply correct kit armor if overwritten
        p.getInventory().setHelmet(helmet);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
    }

    public void setSelectedBridgeFightKit(UUID uuid, String kitName) {
        selectedBridgeFightKit.put(uuid, kitName);
    }

    public String getSelectedBridgeFightKit(UUID uuid) {
        return selectedBridgeFightKit.getOrDefault(uuid, "Default");
    }


}
