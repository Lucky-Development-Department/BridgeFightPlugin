package me.molfordan.arenaAndFFAManager.object;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SerializableAs("SerializableBlockState")
public class SerializableBlockState implements ConfigurationSerializable {
    private final Material type;
    private final byte data;

    public SerializableBlockState(Block block) {
        this.type = block.getType();
        this.data = block.getData();
    }

    // ADD THIS ↓↓↓
    public SerializableBlockState(Material type, byte data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("data", data);
        return map;
    }

    public static SerializableBlockState deserialize(Map<String, Object> map) {
        if (map == null || !map.containsKey("type")) return null;

        String typeName = (String) map.get("type");
        Material material = Material.matchMaterial(typeName);
        if (material == null) return null;

        byte data = 0;
        Object dataObj = map.get("data");
        if (dataObj instanceof Number) {
            data = ((Number) dataObj).byteValue();
        } else if (dataObj instanceof String) {
            try {
                data = Byte.parseByte((String) dataObj);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return new SerializableBlockState(material, data);
    }

    @SuppressWarnings("deprecation")
    public void apply(Block block) {
        if (block == null) return;
        block.setType(type);
        block.setData(data); // deprecated in modern versions but valid in 1.8.9
    }

    public SerializableBlockState clone() {
        return new SerializableBlockState(type, data);
    }

    public Material getType() {
        return type;
    }

    public byte getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerializableBlockState)) return false;
        SerializableBlockState that = (SerializableBlockState) o;
        return data == that.data && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data);
    }

    @Override
    public String toString() {
        return "SerializableBlockState{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}
