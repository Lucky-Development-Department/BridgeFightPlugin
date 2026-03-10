package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.listener.PlayerKillEventListener;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.utils.WorldGuardUtils;
import me.molfordan.arenaAndFFAManager.utils.CustomItem;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeathMessageManager implements Listener {

    private final Set<UUID> recentlyHandled = new HashSet<>();

    private CombatManager combatManager;
    private ArenaAndFFAManager plugin;

    // kept for compatibility in case other classes reference them
    public static final Set<UUID> voidHandled = new HashSet<>();
    public static final Set<UUID> quitHandled = new HashSet<>();
    public static final Set<UUID> deathHandled = ConcurrentHashMap.newKeySet();

    private final HotbarDataManager hotbarDataManager;
    private final StatsManager statsManager;
    private final FireballTracker fireballTracker;

    private ArenaManager arenaManager;
    public String PREFIX;

    public DeathMessageManager(ArenaAndFFAManager plugin,
                               CombatManager combatManager,
                               ArenaManager arenaManager,
                               HotbarDataManager hotbarDataManager,
                               StatsManager statsManager,
                               FireballTracker fireballTracker) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.arenaManager = arenaManager;
        this.hotbarDataManager = hotbarDataManager;
        this.statsManager = statsManager;
        this.fireballTracker = fireballTracker;
        this.PREFIX = plugin.getConfigManager().getServerPrefix();
        startDuelCleaner();
    }

    /**
     * Void death message — does NOT modify stats (stats already updated in handleDeath)
     */
    public void sendVoidDeathMessage(Player victim, Player attacker, Arena arena, int attackerStreak) {
        plugin.debug("sendVoidDeathMessage: Victim=" + victim.getName() + ", Attacker=" + (attacker != null ? attacker.getName() : "none") + ", Arena=" + (arena != null ? arena.getName() : "none") + ", AttackerStreak=" + attackerStreak);

        // Always re-resolve arena from victim's location for correctness
        Arena resolvedArena = arenaManager.getArenaByLocationIgnoreY(victim.getLocation());
        plugin.debug("sendVoidDeathMessage: Resolved Arena from victim's location: " + (resolvedArena != null ? resolvedArena.getName() : "none"));

        if (resolvedArena == null) {
            plugin.debug("sendVoidDeathMessage: Resolved Arena is null. Returning.");
            return;
        }
        if (!resolvedArena.isInside(victim.getLocation(), true)) {
            plugin.debug("sendVoidDeathMessage: Victim not inside resolved arena. Returning.");
            return;
        }
        arena = resolvedArena; // Use the re-resolved arena

        // Prevent duplicate handling (short window)
        if (recentlyHandled.contains(victim.getUniqueId())) {
            plugin.debug("sendVoidDeathMessage: Victim " + victim.getName() + " recently handled. Returning.");
            return;
        }
        recentlyHandled.add(victim.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyHandled.remove(victim.getUniqueId()), 2L);
        plugin.debug("sendVoidDeathMessage: Victim " + victim.getName() + " added to recentlyHandled.");

        // Check if victim was recently hit by a fireball and find the fireball owner
        Player fireballOwner = null;
        if (fireballTracker.wasRecentlyHitByFireball(victim)) {
            // Try to find the most recent fireball owner from combat manager or other means
            // For now, we'll use the existing attacker if available
            fireballOwner = attacker;
        }

        String message;
        if (fireballOwner != null && fireballOwner.isOnline()) {
            message = ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] was thrown into the void by a fireball from "
                    + ChatColor.GREEN + fireballOwner.getName() + ChatColor.GRAY + "[" + attackerStreak + "]";
        } else if (attacker != null && attacker.isOnline()) {
            message = ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] was thrown into the void by "
                    + ChatColor.GREEN + attacker.getName() + ChatColor.GRAY + "[" + attackerStreak + "]";
        } else {
            message = ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] fell into the void.";
        }

        plugin.debug("sendVoidDeathMessage: Constructed message: " + message);

        // Rewards / effects should use the already computed streak
        Player rewardPlayer = fireballOwner != null ? fireballOwner : attacker;
        if (rewardPlayer != null && rewardPlayer.isOnline()) {
            if (arena.getType() == ArenaType.FFABUILD) {
                giveKillRewards(rewardPlayer, attackerStreak);
                giveKillEffect(rewardPlayer);
                plugin.debug("sendVoidDeathMessage: Reward player " + rewardPlayer.getName() + " received FFABUILD rewards.");
            }
        }

        broadcastMessage(arena, message);
        plugin.debug("sendVoidDeathMessage: Message broadcasted for " + victim.getName());
    }

    public void clearDuel(UUID uuid) {
        DuelSession duel = duels.remove(uuid);
        if (duel == null) return;

        // Restore visibility for both players
        Player p1 = Bukkit.getPlayer(duel.p1);
        Player p2 = Bukkit.getPlayer(duel.p2);

        if (p1 != null && p1.isOnline()) {
            showAllFor(p1);
        }
        if (p2 != null && p2.isOnline()) {
            showAllFor(p2);
        }

        duels.remove(duel.p1);
        duels.remove(duel.p2);
    }

    private void startDuelCleaner() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<DuelSession> it = new HashSet<>(duels.values()).iterator();
            while (it.hasNext()) {
                DuelSession duel = it.next();
                if (duel.expired()) {

                    Player p1 = Bukkit.getPlayer(duel.p1);
                    Player p2 = Bukkit.getPlayer(duel.p2);

                    if (p1 != null && p1.isOnline()) showAllFor(p1);
                    if (p2 != null && p2.isOnline()) showAllFor(p2);

                    duels.remove(duel.p1);
                    duels.remove(duel.p2);
                }
            }
        }, 20L, 20L);
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

        clearDuel(victim.getUniqueId());

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
        int currentKillerStreak = 0;
        if (killerValid) {
            // Get the correct streak based on arena type BEFORE incrementing
            if (arena.getType() == ArenaType.FFABUILD) {
                currentKillerStreak = statsManager.getStats(killer.getUniqueId()).getBuildStreak();
            } else {
                currentKillerStreak = statsManager.getStats(killer.getUniqueId()).getBridgeStreak();
            }
        }

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

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        String fromWorld = e.getFrom().getName();
        String toWorld = player.getWorld().getName();
        
        String lobbyWorld = plugin.getConfigManager().getLobbyWorldName();
        String bridgeFightWorld = plugin.getConfigManager().getBridgeFightWorldName();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // If player is entering BridgeFight world, handle duel visibility
            if (toWorld.equals(bridgeFightWorld)) {
                for (DuelSession duel : new HashSet<>(duels.values())) {
                    Player p1 = Bukkit.getPlayer(duel.p1);
                    Player p2 = Bukkit.getPlayer(duel.p2);

                    if (p1 == null || p2 == null) continue;

                    // If the player is not part of this duel, hide them from duelers
                    if (!duel.contains(player.getUniqueId())) {
                        p1.hidePlayer(player);
                        p2.hidePlayer(player);
                    }

                    // Player should see duelers
                    player.showPlayer(p1);
                    player.showPlayer(p2);
                }
            }
            // If player is leaving BridgeFight world, restore full visibility
            else if (fromWorld.equals(bridgeFightWorld)) {
                showAllFor(player);
                
                // Also update duelers to show this player if they're not in a duel
                for (DuelSession duel : new HashSet<>(duels.values())) {
                    Player p1 = Bukkit.getPlayer(duel.p1);
                    Player p2 = Bukkit.getPlayer(duel.p2);

                    if (p1 == null || p2 == null) continue;
                    if (!duel.contains(player.getUniqueId())) {
                        p1.showPlayer(player);
                        p2.showPlayer(player);
                    }
                }
            }
        }, 10);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Debug: Log movement event
        plugin.debug("PlayerMoveEvent triggered for " + player.getName() + ", in duel: " + isInDuel(player));
        
        // Only check if player is in a duel
        if (!isInDuel(player)) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Only check if the player actually moved to a different block (not just looking around)
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // Debug: Log block movement
        plugin.debug("Player " + player.getName() + " moved from block [" + from.getBlockX() + "," + from.getBlockY() + "," + from.getBlockZ() + 
                    "] to [" + to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ() + "]");
        
        // Check if WorldGuard is available
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            plugin.debug("WorldGuard not available for " + player.getName());
            return;
        }
        
        // Check if the target location is in a WorldGuard region
        boolean inRegion = WorldGuardUtils.isInAnyRegion(to);
        plugin.debug("Player " + player.getName() + " target location in region: " + inRegion);
        
        if (inRegion) {
            // Cancel the movement and push player back
            plugin.debug("Cancelling movement for " + player.getName() + " and pushing back");
            event.setCancelled(true);
            pushPlayerBackFromRegion(player, to);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player joiner = e.getPlayer();
        
        // Handle initial visibility for players joining in BridgeFight world
        if (joiner.getWorld().getName().equals(plugin.getConfigManager().getBridgeFightWorldName())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (DuelSession duel : new HashSet<>(duels.values())) {
                    Player p1 = Bukkit.getPlayer(duel.p1);
                    Player p2 = Bukkit.getPlayer(duel.p2);

                    if (p1 == null || p2 == null) continue;

                    // If the joiner is not part of this duel, hide them from duelers
                    if (!duel.contains(joiner.getUniqueId())) {
                        p1.hidePlayer(joiner);
                        p2.hidePlayer(joiner);
                    }

                    // Joiner should see duelers
                    joiner.showPlayer(p1);
                    joiner.showPlayer(p2);
                }
            }, 10);
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

    private boolean handleExplosionDeath(Player victim, Player killer) {
        if (killer == null) return false;

        // Check if the killer is the same as the victim (self-kill)
        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return true;
        }

        // Check if the killer is a fireball or TNT owned by the victim
        if (killer instanceof Fireball || killer.getType() == EntityType.PRIMED_TNT) {
            if (killer instanceof Fireball) {
                Fireball fireball = (Fireball) killer;
                if (fireball.getShooter() instanceof Player) {
                    Player shooter = (Player) fireball.getShooter();
                    return shooter.getUniqueId().equals(victim.getUniqueId());
                }
            } else if (killer.getType() == EntityType.PRIMED_TNT) {
                // For TNT, check if it was placed by the victim
                // This requires the TNT to be tracked when placed
                // You'll need to implement this tracking in a TNT listener
                // For now, we'll just check if the TNT is very close to the victim
                return killer.getLocation().distanceSquared(victim.getLocation()) < 9; // within 3 blocks
            }
        }

        return false;
    }

    // Modify the sendPlayerKillMessage method to handle self-explosion
    public void sendPlayerKillMessage(Player victim, Player killer, Arena arena, int newStreak) {
        // Resolve arena reliably
        arena = arenaManager.getArenaByLocation(killer != null ? killer.getLocation() : victim.getLocation());
        if (arena == null) return;

        if (arena.getType() == ArenaType.TOPFIGHT) return;

        if (recentlyHandled.contains(victim.getUniqueId())) return;
        recentlyHandled.add(victim.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyHandled.remove(victim.getUniqueId()), 2L);

        String message;

        // Check for self-explosion
        if (handleExplosionDeath(victim, killer)) {
            message = ChatColor.RED + victim.getName() + ChatColor.GRAY + "[0] died.";
        } else {
            // Normal kill message
            String killString = killer != null ? "was killed by" : "was killed";
            message = ChatColor.RED + victim.getName() + ChatColor.GRAY + " " + (killer != null ? killString : "was killed")
                    + ChatColor.GRAY + (killer != null ? " " + ChatColor.GREEN + killer.getName() + ChatColor.GRAY + "[" + newStreak + "]" : "");
        }

        // Rewards/effects for killer if appropriate
        if (killer != null && killer.isOnline() && arena.getType() == ArenaType.FFABUILD && !handleExplosionDeath(victim, killer)) {
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
                // Check if player is in an arena of the same type
                Arena playerArena = arenaManager.getArenaByLocationIgnoreY(player.getLocation());
                if (playerArena != null && playerArena.getType() == arena.getType()) {
                    player.sendMessage(message);
                }
            }
        }
    }

    private String fireballName = ChatColor.RED + "Fireball";

    private ItemStack giveFireballItem() {
        ItemStack item = new ItemStack(Material.FIREBALL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(fireballName);
        item.setItemMeta(meta);
        return item;
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
        if (streak > 0 && streak % 3 == 0) {
            ensureItem(killer, Material.FIREBALL, 1, giveFireballItem(), 2);
            ensureItem(killer, Material.EGG, 1, new ItemStack(Material.EGG), 2);

        }

        if (streak > 0 && streak % 5 == 0){
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
                givePotion(killer, PotionEffectType.INVISIBILITY, 45, 1, ChatColor.AQUA + "Invisibility II Potion (45s)");
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

    private final Map<UUID, DuelSession> duels = new HashMap<>();

    private static final long DUEL_TIMEOUT = 30000L; // 30 seconds

    private static class DuelSession {
        private final UUID p1;
        private final UUID p2;
        private long lastHit;

        DuelSession(UUID p1, UUID p2) {
            this.p1 = p1;
            this.p2 = p2;
            this.lastHit = System.currentTimeMillis();
        }

        private final Set<UUID> notified = new HashSet<>();


        boolean contains(UUID uuid) {
            return p1.equals(uuid) || p2.equals(uuid);
        }

        UUID getOpponent(UUID uuid) {
            return p1.equals(uuid) ? p2 : p1;
        }

        void refresh() {
            lastHit = System.currentTimeMillis();
        }

        boolean expired() {
            return System.currentTimeMillis() - lastHit >= DUEL_TIMEOUT;
        }

        boolean notifyOnce(UUID uuid) {
            return notified.add(uuid); // true only the first time
        }

    }

    private void hideOthersFor(Player p, DuelSession duel) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(p.getUniqueId())) continue;
            if (duel.contains(other.getUniqueId())) continue; // opponent stays visible

            p.hidePlayer(other);
        }
    }

    private void showAllFor(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(p.getUniqueId())) continue;
            p.showPlayer(other);
        }
    }

    /**
     * Check if a player is in an active duel
     */
    public boolean isInDuel(Player player) {
        return duels.containsKey(player.getUniqueId());
    }

    /**
     * Check if a player can enter a location (used for WorldGuard region checking)
     */
    public boolean canEnterLocation(Player player, Location location) {
        // If player is not in a duel, allow movement
        if (!isInDuel(player)) {
            return true;
        }

        // If WorldGuard is not available, allow movement
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return true;
        }

        // Check if the target location is in ANY WorldGuard region (block all duelers)
        if (WorldGuardUtils.isInAnyRegion(location)) {
            Set<String> regions = WorldGuardUtils.getRegionNamesAt(location);
            String regionList = String.join(", ", regions);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX) + 
                ChatColor.RED + " Cannot enter the platform while in duel: " + ChatColor.YELLOW + regionList);
            return false;
        }

        return true;
    }

    /**
     * Push player back from WorldGuard region with appropriate message
     */
    private void pushPlayerBackFromRegion(Player player, Location targetLocation) {
        plugin.debug("pushPlayerBackFromRegion called for " + player.getName());
        
        Set<String> regions = WorldGuardUtils.getRegionNamesAt(targetLocation);
        String regionList = String.join(", ", regions);
        
        plugin.debug("Regions found: " + regionList);
        
        // Send message to player
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX) +
                ChatColor.RED + " Cannot enter the safe zone platform during fight! ");
        
        // Calculate push-back direction (opposite to movement direction)
        Location currentLocation = player.getLocation();
        Location pushBackLocation = currentLocation.clone();
        
        // Push the player back by 3 blocks in the opposite direction they were moving
        Vector direction = currentLocation.toVector().subtract(targetLocation.toVector()).normalize();
        if (direction.length() == 0) {
            // If no clear direction, push backwards
            direction = player.getLocation().getDirection().multiply(-1);
        }
        direction.multiply(3); // Push back 3 blocks
        
        pushBackLocation.add(direction);
        
        plugin.debug("Pushing player from [" + currentLocation.getBlockX() + "," + currentLocation.getBlockY() + "," + currentLocation.getBlockZ() + 
                    "] to [" + pushBackLocation.getBlockX() + "," + pushBackLocation.getBlockY() + "," + pushBackLocation.getBlockZ() + "]");
        
        // Ensure the push-back location is safe (not in another region)
        if (WorldGuardUtils.isInAnyRegion(pushBackLocation)) {
            // If still in a region, try to find a safe location nearby
            plugin.debug("Push-back location still in region, finding safe location");
            pushBackLocation = findSafeLocationNearby(currentLocation, 5);
        }
        
        // Teleport player to push-back location
        player.teleport(pushBackLocation);
        plugin.debug("Teleport completed for " + player.getName());
    }

    /**
     * Find a safe location near the given location that is not in a WorldGuard region
     */
    private Location findSafeLocationNearby(Location center, int radius) {
        // Check in a spiral pattern around the center
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location testLocation = center.clone().add(x, 0, z);
                if (!WorldGuardUtils.isInAnyRegion(testLocation)) {
                    // Make sure the location is safe (not in void)
                    if (testLocation.getY() > 0) {
                        return testLocation;
                    }
                }
            }
        }
        
        // If no safe location found, return the original location
        return center;
    }

    public boolean handleDuelHit(Player damager, Player victim) {
        UUID d = damager.getUniqueId();
        UUID v = victim.getUniqueId();

        // Check if either player is in ANY WorldGuard region (prevent dueling in protected areas)
        if (WorldGuardUtils.isWorldGuardAvailable()) {
            boolean damagerInRegion = WorldGuardUtils.isInAnyRegion(damager.getLocation());
            boolean victimInRegion = WorldGuardUtils.isInAnyRegion(victim.getLocation());
            
            if (damagerInRegion || victimInRegion) {
                // Cancel the duel attempt if either player is in a WorldGuard region
                String regionName = damagerInRegion ? 
                    WorldGuardUtils.getRegionNamesAt(damager.getLocation()).iterator().next() :
                    WorldGuardUtils.getRegionNamesAt(victim.getLocation()).iterator().next();
                
                damager.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX) + 
                    ChatColor.RED + " Cannot enter the safe zone platform during fight! ");
                return false;
            }
        }

        DuelSession duelD = duels.get(d);
        DuelSession duelV = duels.get(v);

        // Case 1: neither is in a duel → create new duel
        // Case 1: neither is in a duel → create new duel
        if (duelD == null && duelV == null) {
            DuelSession duel = new DuelSession(d, v);
            duels.put(d, duel);
            duels.put(v, duel);

            // Hide others for both duelers
            hideOthersFor(damager, duel);
            hideOthersFor(victim, duel);

            // Send "now fighting" message ONCE
            damager.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX)
                    + ChatColor.GREEN + " You're now fighting " + ChatColor.RED + victim.getName());
            victim.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX)
                    + ChatColor.GREEN + " You're now fighting " + ChatColor.RED + damager.getName());

            duel.notifyOnce(d);
            duel.notifyOnce(v);

            return true;
        }

        // Case 2: both are in the SAME duel → refresh timer
        if (duelD != null && duelD == duelV) {
            duelD.refresh();

            // If one of them never got the message (edge case)
            if (duelD.notifyOnce(d)) {
                damager.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX)+ChatColor.GREEN + " You're now fighting " +
                        ChatColor.RED + Bukkit.getPlayer(duelD.getOpponent(d)).getName());
            }

            return true;
        }

        // Case 3: third-party interrupt → deny hit + message
        if (duelV != null && !duelV.contains(d)) {
            Player opponent = Bukkit.getPlayer(duelV.getOpponent(v));
            if (opponent != null) {
                damager.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX)+ChatColor.RED + " This player is fighting " +
                        ChatColor.YELLOW + opponent.getName());
            }
        }

        return false;
    }
}
