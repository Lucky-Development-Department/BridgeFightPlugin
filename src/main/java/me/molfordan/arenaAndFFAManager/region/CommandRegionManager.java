package me.molfordan.arenaAndFFAManager.region;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.config.RegionsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegionManager {

    private final ArenaAndFFAManager plugin;
    private final RegionsConfig regionsConfig;

    // region name -> CommandRegion
    private final Map<String, CommandRegion> regions = new ConcurrentHashMap<>();

    // per-player selection storage (UUID key)
    private final Map<UUID, Location> pos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2 = new ConcurrentHashMap<>();

    public CommandRegionManager(ArenaAndFFAManager plugin, RegionsConfig regionsConfig) {
        this.plugin = plugin;
        this.regionsConfig = regionsConfig;
    }

    // ----- selection API -----
    public void setPos1(Player p, Location loc) {
        pos1.put(p.getUniqueId(), loc.clone());
        p.sendMessage("§a[Region] pos1 set.");
    }

    public void setPos2(Player p, Location loc) {
        pos2.put(p.getUniqueId(), loc.clone());
        p.sendMessage("§a[Region] pos2 set.");
    }

    public Optional<Location> getPlayerPos1(UUID playerId) {
        return Optional.ofNullable(pos1.get(playerId));
    }
    public Optional<Location> getPlayerPos2(UUID playerId) {
        return Optional.ofNullable(pos2.get(playerId));
    }

    // ----- create/save/delete -----
    public boolean createRegionAndSave(String name, Player owner, String command, CommandRegion.Executor executor) {
        UUID id = owner.getUniqueId();
        Location a = pos1.get(id);
        Location b = pos2.get(id);
        if (a == null || b == null) return false;

        CommandRegion r = new CommandRegion(a.clone(), b.clone(), command, convert(executor));
        regions.put(name, r);
        saveRegionToConfig(name, r);
        plugin.debug("[Regions] created region: " + name);
        return true;
    }

    public boolean deleteRegion(String name) {
        if (!regions.containsKey(name)) return false;
        regions.remove(name);

        FileConfiguration cfg = regionsConfig.getConfig();
        cfg.set("Regions." + name, null);
        regionsConfig.save();
        plugin.debug("[Regions] deleted region: " + name);
        return true;
    }

    // ----- lookup / iteration -----
    public Collection<CommandRegion> getRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public Set<String> getRegionNames() {
        return Collections.unmodifiableSet(regions.keySet());
    }

    public CommandRegion getRegion(String name) {
        return regions.get(name);
    }

    // ----- config load/save -----
    public void loadAllFromConfig() {
        FileConfiguration cfg = regionsConfig.getConfig();
        if (cfg == null || !cfg.isConfigurationSection("Regions")) {
            plugin.debug("[Regions] No Regions section found.");
            return;
        }

        for (String key : cfg.getConfigurationSection("Regions").getKeys(false)) {
            String base = "Regions." + key;
            try {
                String world = cfg.getString(base + ".world", "");
                if (world.isEmpty()) {
                    plugin.debug("[Regions] region " + key + " missing world, skipping");
                    continue;
                }
                org.bukkit.World w = Bukkit.getWorld(world);
                if (w == null) {
                    plugin.debug("[Regions] world not loaded for region " + key + " -> skipping: " + world);
                    continue;
                }

                double x1 = cfg.getDouble(base + ".x1");
                double y1 = cfg.getDouble(base + ".y1");
                double z1 = cfg.getDouble(base + ".z1");

                double x2 = cfg.getDouble(base + ".x2");
                double y2 = cfg.getDouble(base + ".y2");
                double z2 = cfg.getDouble(base + ".z2");

                String command = cfg.getString(base + ".command", "");
                String exec = cfg.getString(base + ".executor", "CONSOLE").toUpperCase();

                CommandRegion.Executor executor = CommandRegion.Executor.valueOf(exec);

                CommandRegion r = new CommandRegion(new Location(w, x1, y1, z1),
                        new Location(w, x2, y2, z2),
                        command, executor);
                regions.put(key, r);
                plugin.debug("[Regions] loaded region: " + key);
            } catch (Exception ex) {
                plugin.debug("[Regions] failed to load region " + key + ": " + ex.getMessage());
            }
        }
    }

    private void saveRegionToConfig(String name, CommandRegion r) {
        FileConfiguration cfg = regionsConfig.getConfig();
        if (r.getPos1() == null || r.getPos2() == null) return;

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
        regionsConfig.save();
    }

    private CommandRegion.Executor convert(CommandRegion.Executor e) {
        return e == null ? CommandRegion.Executor.CONSOLE : e;
    }
}
