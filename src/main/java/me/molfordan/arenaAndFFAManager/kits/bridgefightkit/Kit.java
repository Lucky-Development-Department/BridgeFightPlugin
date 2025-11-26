package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import org.bukkit.entity.Player;

public interface Kit {
    String getName();
    void apply(Player player);

    int getRequiredKills();
}
