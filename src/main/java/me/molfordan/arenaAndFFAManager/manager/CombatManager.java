package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CombatManager {

    private final Map<UUID, Long> combatTagged = new HashMap<>();
    private final Map<UUID, UUID> attackerMap = new HashMap<>();
    private final Map<UUID, Arena> combatArenaMap = new HashMap<>();

    private final Map<UUID, Set<UUID>> damageHistory = new HashMap<>();

    private final Set<UUID> commandBypass = new HashSet<>();


    private final ArenaAndFFAManager plugin;
    private final long combatDurationMillis = 30 * 1000; // 30 seconds

    public CombatManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        startCombatChecker();
    }

    public void tag(Player victim, Player damager, Arena arena) {
        if (victim == null || damager == null || arena == null) return;
        
        long expireTime = System.currentTimeMillis() + combatDurationMillis;
        UUID victimId = victim.getUniqueId();
        UUID damagerId = damager.getUniqueId();

        // Tag victim
        combatTagged.put(victimId, expireTime);
        attackerMap.put(victimId, damagerId);
        combatArenaMap.put(victimId, arena);
        
        // Tag damager (if not self-damage)
        if (!victimId.equals(damagerId)) {
            combatTagged.put(damagerId, expireTime);
            combatArenaMap.put(damagerId, arena);
        }
        
        plugin.debug("[Combat] Tagged " + victim.getName() + " (attacker: " + damager.getName() + ") in " + arena.getName());
    }

    public boolean isInCombat(Player player) {
        if (player == null) return false;
        Long expireTime = combatTagged.get(player.getUniqueId());
        return expireTime != null && expireTime > System.currentTimeMillis();
    }

    public long getLastHitTime(Player player) {
        Long expire = combatTagged.get(player.getUniqueId());
        if (expire == null) return 0;
        return expire - combatDurationMillis;
    }

    public Player getAttacker(Player player) {
        if (player == null) return null;
        UUID lastDamagerUUID = attackerMap.get(player.getUniqueId());
        if (lastDamagerUUID == null) return null;
        return Bukkit.getPlayer(lastDamagerUUID);
    }

    public Player getLastDamager(Player player) {
        if (player == null) return null;
        UUID lastDamagerUUID = attackerMap.get(player.getUniqueId());
        if (lastDamagerUUID == null) return null;
        return Bukkit.getPlayer(lastDamagerUUID); // may still be null if offline
    }

    public Arena getTaggedArena(Player player) {
        return combatArenaMap.get(player.getUniqueId());
    }

    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        combatTagged.remove(uuid);
        attackerMap.remove(uuid);
        combatArenaMap.remove(uuid);
    }

    public void recordDamage(Player damager, Player victim) {
        damageHistory.computeIfAbsent(victim.getUniqueId(), k -> new HashSet<>())
                .add(damager.getUniqueId());
    }

    public boolean didPlayerDamage(Player attacker, Player possibleVictim) {
        if (attacker == null || possibleVictim == null) return false;
        Set<UUID> damagedPlayers = damageHistory.get(attacker.getUniqueId());
        return damagedPlayers != null && damagedPlayers.contains(possibleVictim.getUniqueId());
    }

    public void untag(Player player) {
        clear(player);
    }

    public long getCombatTime(UUID uuid) {
        return combatTagged.getOrDefault(uuid, 0L);
    }

    public long getCombatDuration() {
        return combatDurationMillis;
    }

    private void startCombatChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, Long>> iterator = combatTagged.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = iterator.next();
                    if (entry.getValue() < now) {
                        UUID uuid = entry.getKey();
                        attackerMap.remove(uuid);
                        combatArenaMap.remove(uuid);
                        iterator.remove();
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    public void toggleBypass(UUID uuid) {
        if (commandBypass.contains(uuid)) {
            commandBypass.remove(uuid);
        } else {
            commandBypass.add(uuid);
        }
    }

    public boolean isBypassing(UUID uuid) {
        return commandBypass.contains(uuid);
    }
}
