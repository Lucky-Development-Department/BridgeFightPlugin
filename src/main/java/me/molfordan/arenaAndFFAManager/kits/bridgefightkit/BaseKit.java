package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseKit implements Kit {

    protected final List<KitItem> items = new ArrayList<>();

    protected int requiredKills = 0;

    @Override
    public int getRequiredKills() {
        return requiredKills;
    }

    protected void give(Player player) {
        for (KitItem kitItem : items) {
            if (kitItem.getSlot() != null) {
                player.getInventory().setItem(kitItem.getSlot(), kitItem.build());
            } else {
                player.getInventory().addItem(kitItem.build());
            }
        }
    }
}
