package me.molfordan.arenaAndFFAManager.object;

import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

@SerializableAs("Arena")
public class Arena implements ConfigurationSerializable {

    private String name;
    private String worldName;
    private ArenaType type;
    private Location pos1, pos2, center;
    private int buildLimitY = 256;
    private int voidLimitY = 0;
    private boolean finished = false;

    // Matches "blocks:" in arenas.yml
    private Map<String, SerializableBlockState> originalBlocks = new HashMap<>();

    public Arena(String name, World world) {
        this.name = name;
        this.worldName = world.getName();
    }

    @SuppressWarnings("unchecked")
    public Arena(Map<String, Object> data) {
        this.name = (String) data.get("name");
        this.worldName = (String) data.get("world");
        this.type = ArenaType.fromString((String) data.get("type"));

        Object pos1Obj = data.get("pos1");
        if (pos1Obj instanceof Location) this.pos1 = (Location) pos1Obj;

        Object pos2Obj = data.get("pos2");
        if (pos2Obj instanceof Location) this.pos2 = (Location) pos2Obj;

        Object centerObj = data.get("center");
        if (centerObj instanceof Location) this.center = (Location) centerObj;

        this.buildLimitY = (int) data.getOrDefault("buildLimit", 256);
        this.voidLimitY = (int) data.getOrDefault("voidLimit", 0);
        this.finished = (boolean) data.getOrDefault("finished", false);

        Object blocksObj = data.get("blocks");
        if (blocksObj instanceof Map) {
            Map<String, Object> blocksMap = (Map<String, Object>) blocksObj;
            for (Map.Entry<String, Object> entry : blocksMap.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Map) {
                    SerializableBlockState state = SerializableBlockState.deserialize((Map<String, Object>) val);
                    if (state != null) originalBlocks.put(key, state);
                }
            }
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("world", worldName);
        if (pos1 != null) map.put("pos1", pos1);
        if (pos2 != null) map.put("pos2", pos2);
        if (center != null) map.put("center", center);
        map.put("buildLimit", buildLimitY);
        map.put("voidLimit", voidLimitY);
        map.put("finished", finished);
        map.put("type", type != null ? type.name() : null);

        if (type == ArenaType.FFABUILD && !originalBlocks.isEmpty()) {
            Map<String, Object> blockMap = new HashMap<>();
            for (Map.Entry<String, SerializableBlockState> entry : originalBlocks.entrySet()) {
                blockMap.put(entry.getKey(), entry.getValue().serialize());
            }
            map.put("blocks", blockMap);
        }

        return map;
    }

    public void captureCurrentState() {
        if (type != ArenaType.FFABUILD || pos1 == null || pos2 == null) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        originalBlocks.clear();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR) {
                        String locKey = x + "," + y + "," + z;
                        originalBlocks.put(locKey, new SerializableBlockState(block));
                    }
                }
            }
        }


    }

    public boolean isInside(Location loc, boolean ignoreY) {
        if (loc == null || pos1 == null || pos2 == null) return false;

        // Match world
        if (!loc.getWorld().getName().equalsIgnoreCase(worldName)) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        boolean inX = x >= minX && x <= maxX;
        boolean inZ = z >= minZ && z <= maxZ;
        boolean inXZ = inX && inZ;
        boolean inY = y >= minY && y <= maxY;

        return ignoreY ? inXZ : (inXZ && inY);
    }



    // --- Getters/Setters ---

    public Map<String, SerializableBlockState> getOriginalBlocksMap() {
        return originalBlocks;
    }

    public String getName() {
        return name;
    }

    public ArenaType getType() {
        return type;
    }

    public void setType(ArenaType type) {
        this.type = type;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public int getBuildLimitY() {
        return buildLimitY;
    }

    public void setBuildLimitY(int buildLimitY) {
        this.buildLimitY = buildLimitY;
    }

    public int getVoidLimit() {
        return voidLimitY;
    }

    public void setVoidLimit(int voidLimitY) {
        this.voidLimitY = voidLimitY;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }


}
