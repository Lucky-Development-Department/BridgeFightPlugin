package me.molfordan.arenaAndFFAManager.utils;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinFetcher {

    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    // Primary username-based API (works for cracked players too)
    private static final String ASHCON_API = "https://api.ashcon.app/mojang/v2/user/";
    // Fallback: CraftHead profile (provides a texture URL we convert to Mojang Base64)
    private static final String CRAFTHEAD_API = "https://crafthead.net/profile/";

    /**
     * Fetch a Base64 texture string for a given playerName (uuid is ignored for offline/cracked servers).
     * Callback is always executed on the main server thread and may receive null on failure.
     */
    public static void getTexture(UUID uuid, String playerName, SkinCallback callback) {
        if (playerName == null || playerName.isEmpty()) {
            runOnMain(() -> callback.done(null));
            return;
        }

        final String key = playerName.toLowerCase();
        if (cache.containsKey(key)) {
            runOnMain(() -> callback.done(cache.get(key)));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("ArenaAndFFAManager"),
                () -> {
                    String base64 = null;

                    // 1) Try Ashcon (returns properties -> value)
                    base64 = fetchFromAshcon(playerName);

                    // 2) If Ashcon didn't provide a properties.value, try CraftHead -> extract texture URL -> convert to Mojang Base64
                    if (base64 == null) {
                        base64 = fetchFromCraftHead(playerName);
                    }

                    // 3) Cache result by player name (lowercase)
                    if (base64 != null) cache.put(key, base64);

                    final String result = base64;
                    runOnMain(() -> callback.done(result));
                }
        );
    }

    private static String fetchFromAshcon(String name) {
        try {
            URL url = new URL(ASHCON_API + name);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "ArenaAndFFAManager/1.0");

            if (con.getResponseCode() != 200) return null;

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            String json = sb.toString();

            // Ashcon returns a compact JSON which usually contains properties -> value
            // Search for the first "value":"...base64..."
            int idx = json.indexOf("\"value\":\"");
            if (idx != -1) {
                int start = idx + "\"value\":\"".length();
                int end = json.indexOf('"', start);
                if (end > start) {
                    return json.substring(start, end);
                }
            }

        } catch (Exception ignored) {}

        return null;
    }

    private static String fetchFromCraftHead(String nameOrId) {
        try {
            URL url = new URL(CRAFTHEAD_API + nameOrId);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "ArenaAndFFAManager/1.0");

            if (con.getResponseCode() != 200) return null;

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            String json = sb.toString();

            // CraftHead returns a JSON with "textures":{ "SKIN": { "url": "http://textures.minecraft.net/texture/..." } }
            int idx = json.indexOf("\"url\":\"");
            if (idx != -1) {
                int start = idx + "\"url\":\"".length();
                int end = json.indexOf('"', start);
                if (end > start) {
                    String textureUrl = json.substring(start, end);
                    // convert to Mojang style Base64 JSON
                    String mojangJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
                    try {
                        return Base64.getEncoder().encodeToString(mojangJson.getBytes("UTF-8"));
                    } catch (Exception e) {
                        // fallback, but unlikely to happen
                        return Base64.getEncoder().encodeToString(mojangJson.getBytes());
                    }
                }
            }

        } catch (Exception ignored) {}

        return null;
    }

    private static void runOnMain(Runnable r) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ArenaAndFFAManager"), r);
    }

    public interface SkinCallback {
        void done(String base64);
    }
}
