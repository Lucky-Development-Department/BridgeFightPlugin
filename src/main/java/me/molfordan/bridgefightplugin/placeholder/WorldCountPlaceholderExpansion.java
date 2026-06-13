package me.molfordan.bridgefightplugin.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldCountPlaceholderExpansion extends PlaceholderExpansion {

    private final BridgeFightPlugin plugin;

    public WorldCountPlaceholderExpansion(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "worldcount";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Molfordan";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equals("bf_total")) {
            long count = Bukkit.getWorlds().stream()
                    .filter(w -> w.getName().startsWith("bf_"))
                    .mapToLong(w -> w.getPlayers().size())
                    .sum();
            return String.valueOf(count);
        }
        return null;
    }
}
