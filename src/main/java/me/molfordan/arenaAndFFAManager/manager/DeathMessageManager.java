package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.listener.PlayerKillEventListener;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.utils.CustomItem;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeathMessageManager {

    private final Set<UUID> recentlyHandled = new HashSet<>();

    private CombatManager combatManager;
    private ArenaAndFFAManager plugin;

    // kept for compatibility in case other classes reference them
    public static final Set<UUID> voidHandled = new HashSet<>();
    public static final Set<UUID> quitHandled = new HashSet<>();
    public static final Set<UUID> deathHandled = ConcurrentHashMap.newKeySet();

    private final HotbarDataManager hotbarDataManager;
    private final StatsManager statsManager;

    private ArenaManager arenaManager;

    public DeathMessageManager(ArenaAndFFAManager plugin,
                               CombatManager combatManager,
                               ArenaManager arenaManager,
                               HotbarDataManager hotbarDataManager,
                               StatsManager statsManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.arenaManager = arenaManager;
        this.hotbarDataManager = hotbarDataManager;
        this.statsManager = statsManager;
    }

    /**
     * Void death message — does NOT modify stats (stats already updated in handleDeath)
     */
    public void sendVoidDeathMessage(Player victim, Player attacker, Arena arena, int attackerStreak) {
        // Always re-resolve arena from victim's location for correctness
        arena = arenaManager.getArenaByLocationIgnoreY(victim.getLocation());
        if (arena == null) return;
        if (!arena.isInside(victim.getLocation(), true)) return;

        // Prevent duplicate handling (short window)
        if (recentlyHandled.contains(victim.getUniqueId())) return;
        recentlyHandled.add(victim.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyHandled.remove(victim.getUniqueId()), 2L);

        String message = (attacker != null && attacker.isOnline())
                ? ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] was thrown into the void by "
                + ChatColor.GREEN + attacker.getName() + ChatColor.GRAY + "[" + attackerStreak + "]"
                : ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] fell into the void.";

        // Rewards / effects should use the already computed streak
        if (attacker != null && attacker.isOnline()) {
            if (arena.getType() == ArenaType.FFABUILD) {
                giveKillRewards(attacker, attackerStreak);
                giveKillEffect(attacker);
            }
        }

        broadcastMessage(arena, message);
    }

    /**
     * Central death handler — updates streaks and totals exactly once, then routes to message senders.
     *
     * @param victim      player who died
     * @param arena       candidate arena (may be re-resolved)
     * @param isVoidDeath whether this is a void death
     * @param isQuit      whether this was a quit/combat-logout
     */
    public void handleDeath(Player victim, Arena arena, boolean isVoidDeath, boolean isQuit) {
        UUID id = victim.getUniqueId();

        // Global duplicate-protection
        if (!deathHandled.add(id)) {
            return; // already processed by another event listener
        }

        // Auto-unlock after 2 ticks
        Bukkit.getScheduler().runTaskLater(plugin, () -> deathHandled.remove(id), 2L);

        // Always re-detect arena from victim location
        arena = arenaManager.getArenaByLocationIgnoreY(victim.getLocation());
        if (arena == null) {
            // fallback (never add stats if arena unknown)
            combatManager.clear(victim);
            statsManager.resetStreak(id, ArenaType.FFA);
            statsManager.resetStreak(id, ArenaType.FFABUILD);
            return;
        }

        // Find killer (vanilla -> combat tag)
        Player killer = victim.getKiller();
        if (killer == null) {
            killer = combatManager.getAttacker(victim);
        }

        boolean killerValid = killer != null &&
                killer.isOnline() &&
                arenaManager.isInArenaIgnoreY(killer);

        // Get current streak before any increments
        int currentKillerStreak = killerValid
                ? statsManager.addKillToStreak(killer.getUniqueId(), arena.getType())
                : 0;

        // Update totals and increment streaks
        addDeath(victim, arena);

        if (killerValid) {
            // Increment streak when adding the kill
            addKill(killer, arena, true);
        }

        // Reset victim streak
        statsManager.resetStreak(victim.getUniqueId(), arena.getType());

        // Route message with incremented streak value
        if (isQuit) {
            sendQuitMessage(victim, killer, arena, currentKillerStreak + 1);
        } else if (isVoidDeath) {
            sendVoidDeathMessage(victim, killer, arena, currentKillerStreak + 1);
        } else {
            sendPlayerKillMessage(victim, killer, arena, currentKillerStreak + 1);
        }

        // Cleanup combat state
        combatManager.clear(victim);
    }

    /**
     * Main entry for giving rewards.
     *
     * @param player      target player
     * @param material    material type of reward (used for counting & layout matching)
     * @param itemToGive  the ItemStack to give (amount in this stack = intended give amount)
     * @param minAmount   minimum total the player should have after giving (soft)
     * @param maxAmount   hard cap for this material per player
     */
    private void giveReward(Player player, Material material, ItemStack itemToGive, int minAmount, int maxAmount) {
        if (player == null || !player.isOnline() || itemToGive == null || maxAmount <= 0) return;

        // Count total current amount in inventory
        int currentTotal = countMaterial(player, material);

        // Nothing to do if already at or above max
        if (currentTotal >= maxAmount) return;

        // Determine amount to attempt to give:
        // - If current < minAmount, try to bring them up to minAmount (but don't exceed max)
        // - Otherwise, try to give itemToGive.getAmount(), but not exceeding max
        int spaceLeft = maxAmount - currentTotal;
        int desiredGive;
        if (currentTotal < minAmount) {
            desiredGive = Math.min(minAmount - currentTotal, spaceLeft);
        } else {
            desiredGive = Math.min(itemToGive.getAmount(), spaceLeft);
        }
        if (desiredGive <= 0) return;

        ItemStack toGive = itemToGive.clone();
        toGive.setAmount(desiredGive);

        // Try to place into hotbar layout
        if (placeIntoLayoutOrInventory(player, material, toGive, maxAmount)) {
            return;
        }

        // Fallback: place into inventory (stacking is handled by addItem)
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
        if (!leftovers.isEmpty()) {
            // If anything couldn't be added (shouldn't happen due to spaceLeft check), drop it
            for (ItemStack rem : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }
        }
    }

    /**
     * Attempt to place item into the player's layout slot if one exists.
     * Enforces the layout slot (displaces wrong item to inventory / ground), stacks correctly, respects maxAmount.
     *
     * @return true if placed into layout; false if no layout slot matched and caller should fallback to inventory
     */
    private boolean placeIntoLayoutOrInventory(Player player, Material material, ItemStack toGive, int maxAmount) {
        Map<Integer, String> layout = hotbarDataManager.load(player.getUniqueId());
        if (layout == null || layout.isEmpty()) return false;

        for (Map.Entry<Integer, String> entry : layout.entrySet()) {
            int slot = entry.getKey();
            String configured = entry.getValue();
            if (!layoutEntryMatches(material, configured)) continue;

            // We found the layout slot that should hold this material
            ItemStack existing = player.getInventory().getItem(slot);

            // If existing is different and not null -> displace it into inventory (or drop if inventory full)
            if (existing != null && existing.getType() != material) {
                // Try to move existing into inventory first
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(existing);
                if (!leftover.isEmpty()) {
                    // Could not fit everything: drop leftovers near player
                    for (ItemStack rem : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), rem);
                    }
                }
                // Clear the slot (we will replace it below)
                player.getInventory().setItem(slot, null);
                existing = null;
            }

            // If same material exists in slot -> stack up to max
            if (existing != null && existing.getType() == material) {
                int current = existing.getAmount();
                int addable = Math.min(toGive.getAmount(), maxAmount - countMaterial(player, material)); // ensure global cap
                if (addable <= 0) return true; // already at cap

                existing.setAmount(Math.min(current + addable, maxAmount));
                player.getInventory().setItem(slot, existing);
                return true;
            } else {
                // Slot is empty (or was displaced). We must ensure not to exceed global max when placing.
                int currentTotal = countMaterial(player, material);
                int placeAmount = Math.min(toGive.getAmount(), maxAmount - currentTotal);
                if (placeAmount <= 0) return true;

                ItemStack clone = toGive.clone();
                clone.setAmount(placeAmount);
                player.getInventory().setItem(slot, clone);
                return true;
            }
        }

        // No matching layout slot found
        return false;
    }

    /**
     * Helper: check whether a layout string maps to this material.
     * Accepts:
     *  - literal values like "snowball", "ender_pearl"
     *  - material name matches like "SNOW_BALL" or "ENDER_PEARL" (case-insensitive)
     *  - allow common synonyms ("snowball" -> SNOW_BALL)
     */
    private boolean layoutEntryMatches(Material material, String configured) {
        if (configured == null) return false;
        String cfg = configured.trim().toLowerCase();

        // match common names
        if (cfg.equals("snowball") && material == Material.SNOW_BALL) return true;
        if (cfg.equals("ender_pearl") && material == Material.ENDER_PEARL) return true;
        if (cfg.equals(material.name().toLowerCase())) return true;

        // allow underscores vs spaces
        String normalized = cfg.replace(" ", "_");
        return normalized.equalsIgnoreCase(material.name());
    }

    /**
     * Count how many of a given material the player currently has in their inventory.
     */
    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Compatibility overloads kept for existing calls in your class.
     */
    private void ensureItem(Player player, Material material, int minAmount, ItemStack fallback, int maxAmount) {
        if (fallback == null) {
            // fallback to simple material item with amount = 1
            giveReward(player, material, new ItemStack(material, 1), minAmount, maxAmount);
        } else {
            giveReward(player, material, fallback, minAmount, maxAmount);
        }
    }

    private void ensureItem(Player player, Material material, int minAmount, int giveAmount, int maxAmount) {
        giveReward(player, material, new ItemStack(material, giveAmount), minAmount, maxAmount);
    }


    public void sendQuitMessage(Player quitter, Player opponent, Arena arena, int opponentStreak) {
        // Avoid duplicate handling
        if (recentlyHandled.contains(quitter.getUniqueId())) return;
        recentlyHandled.add(quitter.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyHandled.remove(quitter.getUniqueId()), 2L);

        // Try to determine the arena if null
        if (arena == null) {
            arena = arenaManager.getArenaByLocation(
                    opponent != null ? opponent.getLocation() : quitter.getLocation()
            );
        }

        // If no arena found, just announce generic quit
        if (arena == null) {
            PlayerKillEventListener.resetStreak(quitter.getUniqueId());
            Bukkit.broadcastMessage(ChatColor.RED + quitter.getName() + ChatColor.GRAY + " quit during combat.");
            return;
        }

        // Reset quitter’s temporary streak
        PlayerKillEventListener.resetStreak(quitter.getUniqueId());

        // Build message. Stats were already updated in handleDeath(), so DO NOT touch them here.
        String message;
        if (opponent != null && opponent.isOnline()) {
            message = ChatColor.RED + quitter.getName() + ChatColor.GRAY + "[0] quit while fighting "
                    + ChatColor.GREEN + opponent.getName() + ChatColor.GRAY + "[" + opponentStreak + "]";
        } else {
            message = ChatColor.RED + quitter.getName() + ChatColor.GRAY + "[0] quit during combat.";
        }

        // Rewards/effects for opponent if any (based on precomputed streak)
        if (opponent != null && opponent.isOnline() && arena.getType() == ArenaType.FFABUILD) {
            giveKillRewards(opponent, opponentStreak);
            giveKillEffect(opponent);
        }

        plugin.debug("sendQuitMessage: arena=" + (arena == null ? "null" : arena.getName()) +
                ", opponent=" + (opponent != null ? opponent.getName() : "none"));
        plugin.debug("message=" + message);

        broadcastMessage(arena, message);
    }

    public void sendPlayerKillMessage(Player victim, Player killer, Arena arena, int newStreak) {
        // Resolve arena reliably
        arena = arenaManager.getArenaByLocation(killer != null ? killer.getLocation() : victim.getLocation());
        if (arena == null) return;

        if (arena.getType() == ArenaType.TOPFIGHT) return;

        if (recentlyHandled.contains(victim.getUniqueId())) return;
        recentlyHandled.add(victim.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyHandled.remove(victim.getUniqueId()), 2L);

        // Stats were already updated in handleDeath() — do not touch them here.
        String message = ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] was killed by "
                + ChatColor.GREEN + (killer != null ? killer.getName() : "unknown") + ChatColor.GRAY + "[" + newStreak + "]";

        // Rewards/effects for killer if appropriate
        if (killer != null && killer.isOnline() && arena.getType() == ArenaType.FFABUILD) {
            giveKillRewards(killer, newStreak);
            giveKillEffect(killer);
        }

        broadcastMessage(arena, message);
    }

    public void sendCombatLogDeathMessage(Player victim, Player attacker, Arena arena, int newStreak) {
        if (attacker != null && attacker.isOnline()) {
            Bukkit.broadcastMessage("§c" + victim.getName() + " §7combat logged and was killed by §c"
                    + attacker.getName() + " §7(§e" + newStreak + "§7 kill streak)");
            giveKillRewards(attacker, newStreak);
        } else {
            Bukkit.broadcastMessage("§c" + victim.getName() + " §7combat logged and died.");
        }
    }

    public void clear(UUID playerId) {
        recentlyHandled.remove(playerId);
    }

    private void broadcastMessage(Arena arena, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arena != null && arena.isInside(player.getLocation(), true)) {
                player.sendMessage(message);
            }
        }
    }

    private void giveKillRewards(Player killer, int streak) {
        if (killer == null || !killer.isOnline()) return;
        killer.setHealth(killer.getMaxHealth());
        ensureItem(killer, Material.ENDER_PEARL, 1, 2, 2);
        ensureItem(killer, Material.SNOW_BALL, 1, CustomItem.getTeleportSnowball(), 2);

        // Rest of the reward logic...

        // Resort the inventory to respect hotbar layout
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            HotbarManager.resortInventory(killer, hotbarDataManager);
        });
        if (streak > 0 && streak % 5 == 0) {
            killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        }
        switch (streak) {
            case 5:
                break;
            case 10:
                givePotion(killer, PotionEffectType.JUMP, 45, 4, ChatColor.LIGHT_PURPLE + "Jump Boost V (45s)");
                break;
            case 15:
                givePotion(killer, PotionEffectType.SPEED, 30, 1, ChatColor.RED + "Speed Potion (30s)");
                givePotion(killer, PotionEffectType.JUMP, 45, 4, ChatColor.LIGHT_PURPLE + "Jump Boost V (45s)");
                break;
            case 20:
                givePotion(killer, PotionEffectType.INCREASE_DAMAGE, 30, 0, ChatColor.RED + "Strength Potion (30s)");
                break;
            case 25:
                givePotion(killer, PotionEffectType.SPEED, 30, 1, ChatColor.RED + "Speed Potion (30s)");
                givePotion(killer, PotionEffectType.JUMP, 45, 4, ChatColor.LIGHT_PURPLE + "Jump Boost V (45s)");
                killer.getInventory().setLeggings(null);
                killer.getInventory().setBoots(null);
                killer.getInventory().setLeggings(createArmorPiece(Material.IRON_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 4));
                killer.getInventory().setBoots(createArmorPiece(Material.IRON_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 4));
                break;
            case 30:
                killer.getInventory().remove(Material.STONE_SWORD);
                killer.getInventory().setItem(0, createSword(Material.STONE_SWORD, Enchantment.DAMAGE_ALL, 1));
                givePotion(killer, PotionEffectType.INCREASE_DAMAGE, 30, 1, ChatColor.RED + "Strength Potion (30s)");
                break;
            case 35:
                givePotion(killer, PotionEffectType.DAMAGE_RESISTANCE, 45, 2, ChatColor.AQUA + "Resistance Potion (45s)");
                break;
            case 40:
                killer.getInventory().remove(Material.STONE_SWORD);
                killer.getInventory().setItem(0, createSword(Material.STONE_SWORD, Enchantment.DAMAGE_ALL, 2));
                break;
            case 45:
                giveMultiEffectPotion(killer, ChatColor.DARK_PURPLE + "Health Boost & Regeneration (45s)",
                        new PotionEffect(PotionEffectType.HEALTH_BOOST, 45 * 20, 4),
                        new PotionEffect(PotionEffectType.REGENERATION, 45 * 20, 1),
                        new PotionEffect(PotionEffectType.ABSORPTION, 45 * 20, 1)
                );
                break;
            case 50:
                givePotion(killer, PotionEffectType.INCREASE_DAMAGE, 30, 1, ChatColor.RED + "Strength Potion (30s)");
                givePotion(killer, PotionEffectType.DAMAGE_RESISTANCE, 45, 2, ChatColor.AQUA + "Resistance Potion (45s)");
                break;
            case 55:
                givePotion(killer, PotionEffectType.SPEED, 30, 1, ChatColor.RED + "Speed Potion (30s)");
                givePotion(killer, PotionEffectType.JUMP, 45, 4, ChatColor.LIGHT_PURPLE + "Jump Boost V (45s)");
                givePotion(killer, PotionEffectType.DAMAGE_RESISTANCE, 45, 2, ChatColor.AQUA + "Resistance Potion (45s)");
                break;
            case 60:
                givePotion(killer, PotionEffectType.INCREASE_DAMAGE, 30, 1, ChatColor.RED + "Strength Potion (30s)");
                killer.getInventory().remove(Material.STONE_SWORD);
                killer.getInventory().setItem(0, createSword(Material.IRON_SWORD, Enchantment.DAMAGE_ALL, 1));
                break;
            case 100:
                givePotion(killer, PotionEffectType.INCREASE_DAMAGE, 120, 1, ChatColor.RED + "Strength Potion (120s)");
                killer.getInventory().remove(Material.IRON_SWORD);
                killer.getInventory().setItem(0, createSword(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 2));
                killer.getInventory().setLeggings(null);
                killer.getInventory().setBoots(null);
                killer.getInventory().setLeggings(createArmorPiece(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 4));
                killer.getInventory().setBoots(createArmorPiece(Material.IRON_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 4));
                break;
        }
    }
    /*
    private void ensureItem(Player player, Material material, int minAmount, ItemStack fallback, int maxAmount) {
        giveRewardIntoLayout(player, material, fallback, minAmount, maxAmount);
    }

     */

    private void giveKillEffect(Player killer) {
        if (killer == null) return;

        PotionEffect found = null;
        for (PotionEffect pe : killer.getActivePotionEffects()) {
            if (pe.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                found = pe;
                break;
            }
        }

        if (found != null) {
            int remainingTicks = found.getDuration(); // duration in ticks
            int remainingSeconds = remainingTicks / 20;
            if (remainingSeconds > 5) {
                // If more than 5 seconds remain, do nothing
                return;
            }
            killer.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }

        // Apply new 5s Strength I
        killer.addPotionEffect(new PotionEffect(
                PotionEffectType.INCREASE_DAMAGE,
                60, // 5 seconds * 20 ticks
                0,
                true,
                false
        ), true);
    }

    /*
    private void ensureItem(Player player, Material material, int minAmount, int giveAmount, int maxAmount) {
        giveRewardIntoLayout(player, material, new ItemStack(material, giveAmount), minAmount, maxAmount);
    }

     */

    private void givePotion(Player player, PotionEffectType type, int durationSeconds, int amplifier, String name) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addCustomEffect(new PotionEffect(type, durationSeconds * 20, amplifier), true);
            potion.setItemMeta(meta);
            player.getInventory().addItem(potion);
        }
    }

    private void giveMultiEffectPotion(Player player, String name, PotionEffect... effects) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            for (PotionEffect effect : effects) {
                meta.addCustomEffect(effect, true);
            }
            potion.setItemMeta(meta);
            player.getInventory().addItem(potion);
        }
    }

    private ItemStack createSword(Material material, Enchantment enchantment, int enchantmentlevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.addEnchant(enchantment, enchantmentlevel, true);
            meta.spigot().setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createArmorPiece(Material material, Enchantment enchantment, int enchantmentlevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.addEnchant(enchantment, enchantmentlevel, true);
            meta.spigot().setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Increase only total kills (not streak). Streaks are handled centrally in handleDeath().
     */
    private void addKill(Player killer, Arena arena, boolean incrementStreak) {
        if (killer == null) return;

        PlayerStats stats = statsManager.getStats(killer.getUniqueId());
        if (stats == null) return;

        if (arena.getType() == ArenaType.FFA) {
            stats.addBridgeKill(incrementStreak); // Pass the flag to increment streak
        } else if (arena.getType() == ArenaType.FFABUILD) {
            stats.addBuildKill(incrementStreak); // Pass the flag to increment streak
        }

        statsManager.savePlayerAsync(stats);
    }

    private void addDeath(Player victim, Arena arena) {
        if (victim == null) return;

        PlayerStats stats = statsManager.getStats(victim.getUniqueId());
        if (stats == null) return;

        if (arena.getType() == ArenaType.FFA) {
            stats.addBridgeDeath();  // BridgeFight death
            stats.resetBridgeStreak();
        } else if (arena.getType() == ArenaType.FFABUILD) {
            stats.addBuildDeath();
            stats.resetBuildStreak();// BuildFFA death
        }

        statsManager.savePlayerAsync(stats);
    }
}
