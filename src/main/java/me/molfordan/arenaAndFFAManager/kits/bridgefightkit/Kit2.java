package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Kit2 {
    private String name;
    private String displayName;
    private int requiredKills;

    private ItemStack weapon;
    private ItemStack helmet;
    private ItemStack chest;
    private ItemStack legs;
    private ItemStack boots;

    public Kit2(String name, String displayName, int requiredKills, ItemStack weapon, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots, int sort) {
        this.name = name;
        this.displayName = displayName;
        this.requiredKills = requiredKills;
        this.weapon = weapon;
        this.helmet = helmet;
        this.chest = chest;
        this.legs = legs;
        this.boots = boots;
        this.sort = sort;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setName(String name) { this.name = name; }
    public int getRequiredKills() { return requiredKills; }

    public void apply(Player p) {
        if (weapon != null) p.getInventory().setItem(0, weapon.clone());
        if (helmet != null) p.getInventory().setHelmet(helmet.clone());
        if (chest != null) p.getInventory().setChestplate(chest.clone());
        if (legs != null) p.getInventory().setLeggings(legs.clone());
        if (boots != null) p.getInventory().setBoots(boots.clone());
    }

    public void applyToPlayer(Player p) {
        if (weapon != null) p.getInventory().setItem(0, weapon.clone());
        if (helmet != null) p.getInventory().setHelmet(helmet.clone());
        if (chest != null) p.getInventory().setChestplate(chest.clone());
        if (legs != null) p.getInventory().setLeggings(legs.clone());
        if (boots != null) p.getInventory().setBoots(boots.clone());
    }

    public ItemStack getWeapon() { return weapon; }
    public void setWeapon(ItemStack weapon) { this.weapon = weapon; }
    public ItemStack getHelmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }
    public ItemStack getChest() { return chest; }
    public void setChest(ItemStack chest) { this.chest = chest; }
    public ItemStack getLegs() { return legs; }
    public void setLegs(ItemStack legs) { this.legs = legs; }
    public ItemStack getBoots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }

    private int sort;

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }


    public void setRequiredKills(int requiredKills) { this.requiredKills = requiredKills; }

    public void loadFromPlayerInventory(Player p) {

        // Read armor
        ItemStack[] armor = p.getInventory().getArmorContents();

        this.helmet = cloneAndUnbreakable(armor[3]);
        this.chest = cloneAndUnbreakable(armor[2]);
        this.legs = cloneAndUnbreakable(armor[1]);
        this.boots = cloneAndUnbreakable(armor[0]);

        // Read weapon (slot 0)
        this.weapon = cloneAndUnbreakable(p.getInventory().getItem(0));
    }

    private ItemStack cloneAndUnbreakable(ItemStack item) {
        if (item == null) return null;

        ItemStack copy = item.clone();

        if (copy.getItemMeta() != null) {
            ItemMeta meta = copy.getItemMeta();

            if (!meta.spigot().isUnbreakable()) {
                meta.spigot().setUnbreakable(true);
            }

            copy.setItemMeta(meta);
        }

        return copy;
    }





    public void saveToPlayerInventory(Player p) {
        p.getInventory().setHelmet(helmet);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().setItem(0, weapon);
    }


}
