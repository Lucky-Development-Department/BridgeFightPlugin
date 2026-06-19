package me.molfordan.bridgefightplugin.manager;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BalanceManager {

    private final BridgeFightPlugin plugin;

    public BalanceManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the balance of a player by their UUID.
     * Always returns a valid balance since getStats loads/creates stats if not cached.
     * 
     * @param uuid The player's UUID
     * @return The coin balance
     */
    public int getBalance(UUID uuid) {
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        if (stats == null) return 0;
        return stats.getCoins();
    }

    /**
     * Get the balance of an online player.
     * 
     * @param player The player
     * @return The coin balance
     */
    public int getBalance(Player player) {
        if (player == null) return 0;
        return getBalance(player.getUniqueId());
    }

    /**
     * Set the balance of a player by UUID.
     * 
     * @param uuid The player's UUID
     * @param amount The new coin amount
     */
    public void setBalance(UUID uuid, int amount) {
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        if (stats != null) {
            stats.setCoins(amount);
            plugin.getStatsManager().savePlayerAsync(stats);
        }
    }

    /**
     * Set the balance of an online player.
     * 
     * @param player The player
     * @param amount The new coin amount
     */
    public void setBalance(Player player, int amount) {
        if (player == null) return;
        setBalance(player.getUniqueId(), amount);
    }

    /**
     * Add coins to a player's balance by UUID.
     * 
     * @param uuid The player's UUID
     * @param amount The amount of coins to add
     */
    public void addBalance(UUID uuid, int amount) {
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        if (stats != null) {
            stats.setCoins(stats.getCoins() + amount);
            plugin.getStatsManager().savePlayerAsync(stats);
        }
    }

    /**
     * Add coins to an online player's balance.
     * 
     * @param player The player
     * @param amount The amount of coins to add
     */
    public void addBalance(Player player, int amount) {
        if (player == null) return;
        addBalance(player.getUniqueId(), amount);
    }

    /**
     * Remove coins from a player's balance by UUID if they have enough.
     * 
     * @param uuid The player's UUID
     * @param amount The amount of coins to remove
     * @return true if successfully removed, false if insufficient funds
     */
    public boolean removeBalance(UUID uuid, int amount) {
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        if (stats != null) {
            int current = stats.getCoins();
            if (current >= amount) {
                stats.setCoins(current - amount);
                plugin.getStatsManager().savePlayerAsync(stats);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove coins from an online player's balance if they have enough.
     * 
     * @param player The player
     * @param amount The amount of coins to remove
     * @return true if successfully removed, false if insufficient funds
     */
    public boolean removeBalance(Player player, int amount) {
        if (player == null) return false;
        return removeBalance(player.getUniqueId(), amount);
    }

    /**
     * Check if a player has at least a certain balance by UUID.
     * 
     * @param uuid The player's UUID
     * @param amount The required amount
     * @return true if player has enough coins
     */
    public boolean hasBalance(UUID uuid, int amount) {
        return getBalance(uuid) >= amount;
    }

    /**
     * Check if an online player has at least a certain balance.
     * 
     * @param player The player
     * @param amount The required amount
     * @return true if player has enough coins
     */
    public boolean hasBalance(Player player, int amount) {
        if (player == null) return false;
        return hasBalance(player.getUniqueId(), amount);
    }
}
