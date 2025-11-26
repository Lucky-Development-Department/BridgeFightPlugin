package me.molfordan.arenaAndFFAManager.region;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.config.RegionsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegionManager {

    private final ArenaAndFFAManager plugin;
    private final RegionsConfig regionsConfig;

    private final Map<String, CommandRegion> regions = new ConcurrentHashMap<>();

    private final Map<UUID, Location> pos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2 = new ConcurrentHashMap<>();

    public CommandRegionManager(ArenaAndFFAManager plugin, RegionsConfig regionsConfig) {
        this.plugin = plugin;
        this.regionsConfig = regionsConfig;
    }

    public void setPos1(Player p, Location loc) {
        pos1.put(p.getUniqueId(), loc.clone());
        p.sendMessage("§a[Region] pos1 set.");
    }

    public void setPos2(Player p, Location loc) {
        pos2.put(p.getUniqueId(), loc.clone());
        p.sendMessage("§a[Region] pos2 set.");
    }

    public boolean createEmptyRegion(String name, Player p) {
        Location a = pos1.get(p.getUniqueId());
        Location b = pos2.get(p.getUniqueId());
        if (a == null || b == null) return false;

        CommandRegion r = new CommandRegion(a.clone(), b.clone(), "", CommandRegion.Executor.CONSOLE);
        regions.put(name, r);
        saveRegionToConfig(name, r);
        return true;
    }

    public boolean deleteRegion(String name) {
        if (!regions.containsKey(name)) return false;

        regions.remove(name);

        FileConfiguration cfg = regionsConfig.getConfig();
        cfg.set("Regions." + name, null);
        regionsConfig.save();
        return true;
    }

    public Collection<CommandRegion> getRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public Set<String> getRegionNames() {
        return Collections.unmodifiableSet(regions.keySet());
    }

    public CommandRegion getRegion(String name) {
        return regions.get(name);
    }

    public boolean hasSelection(Player p) {
        return pos1.containsKey(p.getUniqueId()) && pos2.containsKey(p.getUniqueId());
    }

    public void loadAllFromConfig() {
        FileConfiguration cfg = regionsConfig.getConfig();
        if (!cfg.isConfigurationSection("Regions")) return;

        for (String key : cfg.getConfigurationSection("Regions").getKeys(false)) {
            String base = "Regions." + key;

            try {
                String worldName = cfg.getString(base + ".world", "");
                if (worldName.isEmpty()) continue;

                World w = Bukkit.getWorld(worldName);
                if (w == null) {
                    plugin.getLogger().warning("Region '" + key + "' skipped: world not found (" + worldName + ")");
                    continue;
                }

                Location p1 = new Location(
                        w,
                        cfg.getDouble(base + ".x1"),
                        cfg.getDouble(base + ".y1"),
                        cfg.getDouble(base + ".z1")
                );

                Location p2 = new Location(
                        w,
                        cfg.getDouble(base + ".x2"),
                        cfg.getDouble(base + ".y2"),
                        cfg.getDouble(base + ".z2")
                );

                String command = cfg.getString(base + ".command", "");

                CommandRegion.Executor executor;
                try {
                    executor = CommandRegion.Executor.valueOf(
                            cfg.getString(base + ".executor", "NULL").toUpperCase()
                    );
                } catch (IllegalArgumentException e) {
                    executor = CommandRegion.Executor.NULL;
                }

                CommandRegion region = new CommandRegion(p1, p2, command, executor);

                // Load flags
                if (cfg.isConfigurationSection(base + ".flags")) {
                    ConfigurationSection flagsSec = cfg.getConfigurationSection(base + ".flags");

                    for (String flagId : flagsSec.getKeys(false)) {
                        FlagType type = FlagType.fromId(flagId);
                        if (type == null) continue;

                        String val = flagsSec.getString(flagId, "");
                        if (val == null) continue;

                        region.setFlag(type, val);
                    }
                }

                regions.put(key, region);

            } catch (Exception ex) {
                plugin.getLogger().warning(
                        "Failed to load region '" + key + "' : " + ex.getMessage()
                );
            }
        }
    }


    public void saveRegionToConfig(String name, CommandRegion r) {
        FileConfiguration cfg = regionsConfig.getConfig();

        String base = "Regions." + name;

        cfg.set(base + ".world", r.getPos1().getWorld().getName());
        cfg.set(base + ".x1", r.getPos1().getX());
        cfg.set(base + ".y1", r.getPos1().getY());
        cfg.set(base + ".z1", r.getPos1().getZ());

        cfg.set(base + ".x2", r.getPos2().getX());
        cfg.set(base + ".y2", r.getPos2().getY());
        cfg.set(base + ".z2", r.getPos2().getZ());

        cfg.set(base + ".command", r.getCommand());
        cfg.set(base + ".executor", r.getExecutor().name());

        // SAVE FLAGS USING ENUM ID
        for (Map.Entry<FlagType, String> e : r.getFlags().entrySet()) {
            cfg.set(base + ".flags." + e.getKey().id(), e.getValue());
        }

        regionsConfig.save();
    }

    public void clearRegions() {
        regions.clear();
    }

}
