package me.molfordan.arenaAndFFAManager.hotbarmanager;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class HotbarUtils {

    public static String identifyCategory(ItemStack item) {
        if (item == null) return null;
        Material m = item.getType();

        switch (m) {
            case WOOL:
            case SANDSTONE:
            case CLAY_BRICK:
                return "blocks";

            case IRON_SWORD:
            case DIAMOND_SWORD:
            case STICK:
                return "melee";

            case IRON_PICKAXE:
            case DIAMOND_PICKAXE:
                return "pickaxe";

            case IRON_AXE:
            case DIAMOND_AXE:
                return "axe";

            case SHEARS:
                return "shears";

            case ENDER_PEARL:
                return "ender_pearl";

            case SNOW_BALL:
                return "snowball";

            case GOLDEN_APPLE:
                return "golden_apple";

            default:
                return null; // irrelevant item, ignore
        }
    }
}
