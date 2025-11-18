package me.molfordan.arenaAndFFAManager.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.*;

public class HeadUtils {

    // Textured head representing "?"
    // Custom "?" texture head (question mark head)

    private static final String UNKNOWN_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODgyOTQ0NmI4NWMwMDU5NjRiYmZhMjc2YjMwMTgxN2Y4Y2NhZGYxYTUyMDY3MWYyM2IzM2Y2OGYzOGQ2In19fQ==";
    private static final String QUESTION_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJl"
                    + "cy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmJmNjUyMjEyNzQ4YjA1OWU0"
                    + "ZWI4NTM3OTM1NWNlMzkwMTBmNWY1MGE3OWQ4NjlhM2I0MzRiYmEwMmYz"
                    + "NiJ9fX0=";

    public static ItemStack getHead(UUID uuid, String name, int value, int position) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        String displayName;
        String loreName;

        if (uuid != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            meta.setOwner(op.getName()); // 1.8 way
            displayName = "§e#" + position + " §f" + op.getName();
            loreName = op.getName();
        } else if (name != null) {
            meta.setOwner(name); // fallback
            displayName = "§e#" + position + " §f" + name;
            loreName = name;
        } else {
            // Unknown / empty
            applyTexture(meta, UNKNOWN_TEXTURE);
            displayName = "§7#" + position + " Unknown";
            loreName = "Unknown";
        }

        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Name: §f" + loreName);
        if (uuid != null) {
            lore.add("§7UUID: §f" + uuid.toString());
        }
        lore.add("§7Kills: §b" + value);
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
    }

    private static SkullMeta applyTexture(SkullMeta meta, String value) {

        try {
            UUID id = new UUID(value.hashCode(), value.hashCode());
            Object profile = Class.forName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class)
                    .newInstance(id, "texture");

            Object propertyMap = profile.getClass().getMethod("getProperties").invoke(profile);
            Object property = Class.forName("com.mojang.authlib.properties.Property")
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", value);

            propertyMap.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", property);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

        } catch (Throwable ignored) {}

        return meta;
    }
}
