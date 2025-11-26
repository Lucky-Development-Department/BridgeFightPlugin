package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Kit2 {
    private final String name;
    private final String displayName;
    private final int requiredKills;

    private final ItemStack weapon;
    private final ItemStack helmet;
    private final ItemStack chest;
    private final ItemStack legs;
    private final ItemStack boots;

    public Kit2(String name, String displayName, int requiredKills, ItemStack weapon, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots) {
        this.name = name;
        this.displayName = displayName;
        this.requiredKills = requiredKills;
        this.weapon = weapon;
        this.helmet = helmet;
        this.chest = chest;
        this.legs = legs;
        this.boots = boots;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getRequiredKills() { return requiredKills; }

    public void apply(Player p) {
        if (weapon != null) p.getInventory().setItem(0, weapon.clone());
        if (helmet != null) p.getInventory().setHelmet(helmet.clone());
        if (chest != null) p.getInventory().setChestplate(chest.clone());
        if (legs != null) p.getInventory().setLeggings(legs.clone());
        if (boots != null) p.getInventory().setBoots(boots.clone());
    }
}
