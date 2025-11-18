package me.molfordan.arenaAndFFAManager.task;

import me.molfordan.arenaAndFFAManager.manager.CombatManager;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CombatTagDisplayTask extends BukkitRunnable {

    private final CombatManager combatManager;
    private final Set<UUID> inCombatLastTick = new HashSet<>();

    public CombatTagDisplayTask(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean isTagged = combatManager.isInCombat(player);

            if (isTagged) {
                long timeLeft = (combatManager.getCombatTime(uuid) - System.currentTimeMillis()) / 1000L;
                timeLeft = Math.max(timeLeft, 0);

                // Show time remaining in action bar
                sendActionBar(player, "§c⚔ You are in combat for §f" + timeLeft + " §cseconds");

                // Show "You are in combat" only once
                if (!inCombatLastTick.contains(uuid)) {
                    inCombatLastTick.add(uuid);
                    player.sendMessage("§cYou are in combat.");
                }
            } else {

                // Show "You are no longer in combat" only once
                if (inCombatLastTick.remove(uuid)) {
                    player.sendMessage("§aYou are no longer in combat.");
                }
            }
        }
    }

    // ✅ 1.8.9-compatible action bar method using NMS
    public void sendActionBar(Player player, String message) {
        IChatBaseComponent component = new ChatComponentText(message);
        PacketPlayOutChat packet = new PacketPlayOutChat(component, (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}
