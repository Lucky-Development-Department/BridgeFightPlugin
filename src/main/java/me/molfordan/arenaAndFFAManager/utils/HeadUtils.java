package me.molfordan.arenaAndFFAManager.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HeadUtils {

    private static final String UNKNOWN_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODgyOTQ0NmI4NWMwMDU5NjRiYmZhMjc2YjMwMTgxN2Y4Y2NhZGYxYTUyMDY3MWYyM2IzM2Y2OGYzOGQ2In19fQ==";

    /**
     * Create the initial head immediately, async texture update delivered via callback.
     * Callback is executed on the main thread (SkinFetcher already does this).
     */
    public static ItemStack getHead(UUID uuid,
                                    String playerName,
                                    int value,
                                    int position,
                                    HeadUpdateCallback callback) {

        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        String display = playerName != null ? playerName : "Unknown";
        meta.setDisplayName("§e#" + position + " §f" + display);

        List<String> lore = new ArrayList<>();
        lore.add("§7Name: §f" + display);
        lore.add("§7Value: §b" + value);
        meta.setLore(lore);

        // immediate placeholder
        meta.setOwner(display);  // ← makes cracked servers happy
        head.setItemMeta(meta);

        // async attempt to fetch texture
        SkinFetcher.getTexture(uuid, playerName, base64 -> {

            // if no base64 → DO NOTHING, keep setOwner()
            if (base64 == null) {
                if (callback != null) callback.updated(head);
                return;
            }

            ItemStack updated = head.clone();
            if (applyTexture(updated, base64)) {
                callback.updated(updated);
            } else {
                // fallback: setOwner
                SkullMeta sm = (SkullMeta) updated.getItemMeta();
                sm.setOwner(display);
                updated.setItemMeta(sm);
                callback.updated(updated);
            }
        });

        return head;
    }


    /**
     * Apply Base64 to an ItemStack skull. Returns true if applied successfully.
     * This method searches the meta implementation class and superclasses for a "profile" field.
     */
    public static boolean applyTexture(ItemStack head, String base64) {
        if (head == null || base64 == null) return false;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return false;

        try {
            // Create GameProfile and add textures property
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", base64));

            // Search for "profile" field in meta's class hierarchy
            Class<?> clazz = meta.getClass();
            Field profileField = null;
            while (clazz != null) {
                try {
                    profileField = clazz.getDeclaredField("profile");
                    profileField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (profileField == null) {
                // can't find profile field — log once and return false
                Bukkit.getLogger().warning("[HeadUtils] Could not find 'profile' field on SkullMeta implementation (" + meta.getClass().getName() + "). Textures will not apply.");
                return false;
            }

            // inject profile
            profileField.set(meta, profile);

            // set updated meta back to item
            head.setItemMeta(meta);
            return true;

        } catch (Throwable t) {
            Bukkit.getLogger().warning("[HeadUtils] Failed to apply skull texture: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }

    public interface HeadUpdateCallback {
        void updated(ItemStack finishedHead);
    }
}
