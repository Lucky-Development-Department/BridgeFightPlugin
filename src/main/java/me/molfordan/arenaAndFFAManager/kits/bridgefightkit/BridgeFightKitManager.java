package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.DefaultKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.DiamondArmorKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.GoldArmorKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.GoldSwordKit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BridgeFightKitManager {
    private final Map<String, Kit2> kits = new HashMap<>();
    private final ArenaAndFFAManager plugin;
    private final FileConfiguration config;

    public BridgeFightKitManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;

        File file = new File(plugin.getDataFolder(), "bridgefight_kits.yml");

        if (!file.exists()) {
            // If exists in jar, save normally
            if (plugin.getResource("bridgefight_kits.yml") != null) {
                plugin.saveResource("bridgefight_kits.yml", false);
            } else {
                // Otherwise create a new empty YAML file
                try {
                    plugin.getDataFolder().mkdirs();
                    file.createNewFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        loadDefaults();
        loadKits();
    }

    private void loadKits() {
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) return;

        for (String kitName : section.getKeys(false)) {
            ConfigurationSection kitSec = section.getConfigurationSection(kitName);

            String displayName = kitSec.getString("display-name", kitName);
            int requiredKills = kitSec.getInt("required-kills", 0);
            int sort = kitSec.getInt("sort", 999);

            ConfigurationSection items = kitSec.getConfigurationSection("items");

            Kit2 kit = new Kit2(
                    kitName,
                    displayName,
                    requiredKills,
                    loadItem(items, "weapon"),
                    loadItem(items, "helmet"),
                    loadItem(items, "chestplate"),
                    loadItem(items, "leggings"),
                    loadItem(items, "boots"),
                    sort
            );

            kits.put(kitName.toLowerCase(), kit);
        }
    }

    public void loadDefaults() {
        ConfigurationSection kitsSec = config.getConfigurationSection("kits");

        if (kitsSec == null) {
            kitsSec = config.createSection("kits");
        }

        // If "Default" kit already exists → do nothing
        if (!kitsSec.contains("Default")) {
            ConfigurationSection def = kitsSec.createSection("Default");

            def.set("display-name", "Default Kit");
            def.set("required-kills", 0);
            def.set("sort", 0);

            ConfigurationSection items = def.createSection("items");

            // weapon
            ConfigurationSection weapon = items.createSection("weapon");
            weapon.set("material", "STONE_SWORD");
            weapon.set("enchantment", "DAMAGE_ALL");
            weapon.set("enchantment-level", 1);
            weapon.set("unbreakable", true);

            // helmet
            ConfigurationSection helmet = items.createSection("helmet");
            helmet.set("material", "LEATHER_HELMET");
            helmet.set("enchantment", "PROTECTION_ENVIRONMENTAL");
            helmet.set("enchantment-level", 3);
            helmet.set("unbreakable", true);
            helmet.set("is-red-green-colored", true);

            // chestplate
            ConfigurationSection chest = items.createSection("chestplate");
            chest.set("material", "LEATHER_CHESTPLATE");
            chest.set("enchantment", "PROTECTION_ENVIRONMENTAL");
            chest.set("enchantment-level", 3);
            chest.set("unbreakable", true);
            chest.set("is-red-green-colored", true);

            // leggings
            ConfigurationSection legs = items.createSection("leggings");
            legs.set("material", "IRON_LEGGINGS");
            legs.set("enchantment", "PROTECTION_ENVIRONMENTAL");
            legs.set("enchantment-level", 3);
            legs.set("unbreakable", true);

            // boots
            ConfigurationSection boots = items.createSection("boots");
            boots.set("material", "IRON_BOOTS");
            boots.set("enchantment", "PROTECTION_ENVIRONMENTAL");
            boots.set("enchantment-level", 3);
            boots.set("unbreakable", true);
        }

        // Save file
        try {
            config.save(new File(plugin.getDataFolder(), "bridgefight_kits.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reload internal kit objects
        kits.clear();
        loadKits();
    }

    private ItemStack loadItem(ConfigurationSection items, String key) {
        if (items == null) return null;
        ConfigurationSection sec = items.getConfigurationSection(key);
        if (sec == null) return null;

        String materialName = sec.getString("material");
        if (materialName == null) return null;

        Material mat = Material.getMaterial(materialName.toUpperCase());
        if (mat == null) return null;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // unbreakable
        if (sec.getBoolean("unbreakable", false))
            meta.spigot().setUnbreakable(true);

        // enchant
        String ench = sec.getString("enchantment");
        int enchLevel = sec.getInt("enchantment-level", 1);
        if (ench != null) {
            Enchantment e = Enchantment.getByName(ench.toUpperCase());
            if (e != null) meta.addEnchant(e, enchLevel, true);
        }

        // leather color
        if (mat.name().startsWith("LEATHER_") &&
                sec.getBoolean("is-red-green-colored", false)) {

            LeatherArmorMeta lam = (LeatherArmorMeta) meta;
            lam.setColor(Color.fromRGB(255, 0, 0)); // red → green logic optional
        }

        item.setItemMeta(meta);
        return item;
    }

    public Kit2 get(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit2> getAllKits() {
        return kits.values();
    }

    public void createEmptyKit(String name) {
        Kit2 kit = new Kit2(
                name,
                name,
                0,
                null, null, null, null, null, 0
        );

        kits.put(name.toLowerCase(), kit);
        saveKit(kit);
    }

    public void saveKit(Kit2 kit) {
        ConfigurationSection kitsSec = config.getConfigurationSection("kits");
        if (kitsSec == null) kitsSec = config.createSection("kits");

        ConfigurationSection sec = kitsSec.createSection(kit.getName());

        sec.set("display-name", kit.getDisplayName());
        sec.set("required-kills", kit.getRequiredKills());
        sec.set("sort", kit.getSort());

        ConfigurationSection items = sec.createSection("items");

        saveItem(items, "weapon", kit.getWeapon());
        saveItem(items, "helmet", kit.getHelmet());
        saveItem(items, "chestplate", kit.getChest());
        saveItem(items, "leggings", kit.getLegs());
        saveItem(items, "boots", kit.getBoots());


        try {
            config.save(new File(plugin.getDataFolder(), "bridgefight_kits.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveItem(ConfigurationSection parent, String key, ItemStack item) {
        if (item == null) {
            parent.set(key, null);
            return;
        }

        ConfigurationSection sec = parent.createSection(key);

        sec.set("material", item.getType().name());

        if (item.getItemMeta() != null) {
            ItemMeta meta = item.getItemMeta();

            if (meta.spigot().isUnbreakable())
                sec.set("unbreakable", true);

            if (!meta.getEnchants().isEmpty()) {
                Enchantment ench = meta.getEnchants().keySet().iterator().next();
                sec.set("enchantment", ench.getName());
                sec.set("enchantment-level", meta.getEnchants().get(ench));
            }

            // leather color
            if (meta instanceof LeatherArmorMeta) {
                LeatherArmorMeta lam = (LeatherArmorMeta) meta;
                Color c = lam.getColor();
                if (c.equals(Color.fromRGB(255, 0, 0)))
                    sec.set("is-red-green-colored", true);
            }
        }
    }

    public void delete(String kitName) {
        if (kitName == null) return;

        // Remove from memory
        kits.remove(kitName.toLowerCase());

        // Remove from config
        ConfigurationSection kitsSec = config.getConfigurationSection("kits");
        if (kitsSec != null && kitsSec.contains(kitName)) {
            kitsSec.set(kitName, null);
        }

        // Save file
        try {
            config.save(new File(plugin.getDataFolder(), "bridgefight_kits.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reload internal kits
        kits.clear();
        loadKits();
    }

    public void reloadAllKits() {
        File file = new File(plugin.getDataFolder(), "bridgefight_kits.yml");

        if (!file.exists()) {
            if (plugin.getResource("bridgefight_kits.yml") != null) {
                plugin.saveResource("bridgefight_kits.yml", false);
            } else {
                try {
                    plugin.getDataFolder().mkdirs();
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Reload YAML
        try {
            config.load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Re-apply default section for "Default" kit if missing
        loadDefaults();

        // Reload live kit objects
        kits.clear();
        loadKits();
    }



}
