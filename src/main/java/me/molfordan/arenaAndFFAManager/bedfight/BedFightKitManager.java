package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.database.connectors.MySQLConnector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BedFightKitManager {
    private final ArenaAndFFAManager plugin;

    public BedFightKitManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void saveKit(String ownerUuid, String name, ItemStack[] items) {
        try {
            Connection conn = ((MySQLConnector) plugin.getDatabaseManager().getConnector()).getConnection();
            String query = "REPLACE INTO bedfight_kits (owner_uuid, name, items) VALUES (?, ?, ?);";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, ownerUuid);
                stmt.setString(2, name);
                stmt.setString(3, serializeItems(items));
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save kit: " + e.getMessage());
        }
    }

    public ItemStack[] loadKit(String ownerUuid, String name) {
        try {
            Connection conn = ((MySQLConnector) plugin.getDatabaseManager().getConnector()).getConnection();
            String query = "SELECT items FROM bedfight_kits WHERE owner_uuid = ? AND name = ?;";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, ownerUuid);
                stmt.setString(2, name);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return deserializeItems(rs.getString("items"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load kit: " + e.getMessage());
        }
        return null;
    }

    private String serializeItems(ItemStack[] items) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeInt(items.length);
        for (ItemStack item : items) dataOutput.writeObject(item);
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    private ItemStack[] deserializeItems(String data) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack[] items = new ItemStack[dataInput.readInt()];
        for (int i = 0; i < items.length; i++) items[i] = (ItemStack) dataInput.readObject();
        dataInput.close();
        return items;
    }
}
