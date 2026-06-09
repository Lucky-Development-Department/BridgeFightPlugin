package me.molfordan.bridgefightplugin.manager;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PatchNotesManager {

    private final BridgeFightPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    
    // session tracking
    private final Map<UUID, PatchNoteSession> activeSessions = new HashMap<>();

    public static class PatchNoteSession {
        public final String id;
        public final String date;
        public final String issuedBy;
        public final List<String> notes = new ArrayList<>();
        public boolean isListening = false;

        public PatchNoteSession(String id, String date, String issuedBy) {
            this.id = id;
            this.date = date;
            this.issuedBy = issuedBy;
        }
    }

    public PatchNotesManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        setupConfig();
    }

    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "patchnotes.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);
                
                // Add an example entry
                String id = generateShortId();
                String date = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date());
                String path = "archives." + id;
                config.set(path + ".date", date);
                config.set(path + ".issuedBy", "System");
                config.set(path + ".notes", Arrays.asList("&aArchive system initialized!", "&7Use &e/patchnotes create &7to start."));
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public String getLatestDate() {
        ConfigurationSection section = config.getConfigurationSection("archives");
        if (section == null || section.getKeys(false).isEmpty()) return "Unknown";
        
        List<String> ids = new ArrayList<>(section.getKeys(false));
        String latestId = ids.get(ids.size() - 1); // Last one added
        return config.getString("archives." + latestId + ".date", "Unknown");
    }

    public List<String> getLatestNotes() {
        ConfigurationSection section = config.getConfigurationSection("archives");
        if (section == null || section.getKeys(false).isEmpty()) return Collections.emptyList();

        List<String> ids = new ArrayList<>(section.getKeys(false));
        String latestId = ids.get(ids.size() - 1);
        return config.getStringList("archives." + latestId + ".notes");
    }

    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean hasActiveSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public String startSession(UUID uuid, String issuerName) {
        String id = generateShortId();
        String date = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date());
        activeSessions.put(uuid, new PatchNoteSession(id, date, issuerName));
        return id;
    }

    public PatchNoteSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public void addNoteToSession(UUID uuid, String note) {
        PatchNoteSession session = activeSessions.get(uuid);
        if (session != null) {
            session.notes.add(note);
        }
    }

    public void finishSession(UUID uuid) {
        PatchNoteSession session = activeSessions.remove(uuid);
        if (session == null || session.notes.isEmpty()) return;

        String path = "archives." + session.id;
        config.set(path + ".date", session.date);
        config.set(path + ".issuedBy", session.issuedBy);
        config.set(path + ".notes", session.notes);
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        reload();
    }

    public void cancelSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    public void displayPage(CommandSender sender, int page) {
        ConfigurationSection section = config.getConfigurationSection("archives");
        if (section == null || section.getKeys(false).isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No patch notes found.");
            return;
        }

        List<String> ids = new ArrayList<>(section.getKeys(false));
        Collections.reverse(ids); // Latest first

        int entriesPerPage = 3;
        int totalPages = (int) Math.ceil((double) ids.size() / entriesPerPage);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        sender.sendMessage(ChatColor.GOLD + "------- Patch Notes Archive (Page " + page + "/" + totalPages + ") -------");

        int start = (page - 1) * entriesPerPage;
        for (int i = start; i < start + entriesPerPage && i < ids.size(); i++) {
            String id = ids.get(i);
            String date = config.getString("archives." + id + ".date");
            String issuedBy = config.getString("archives." + id + ".issuedBy");
            List<String> notes = config.getStringList("archives." + id + ".notes");

            sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + id + ChatColor.GRAY + " | " + ChatColor.YELLOW + "Date: " + ChatColor.WHITE + date);
            sender.sendMessage(ChatColor.YELLOW + "Issued by: " + ChatColor.AQUA + issuedBy);
            for (String note : notes) {
                sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.translateAlternateColorCodes('&', note));
            }
            sender.sendMessage("");
        }

        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/patchnotes " + (page + 1) + ChatColor.GRAY + " for next page.");
        }
        sender.sendMessage(ChatColor.GOLD + "-----------------------------------------");
    }

    public void displayAll(CommandSender sender) {
        ConfigurationSection section = config.getConfigurationSection("archives");
        if (section == null) return;

        sender.sendMessage(ChatColor.GOLD + "------- All Patch Notes -------");
        List<String> ids = new ArrayList<>(section.getKeys(false));
        Collections.reverse(ids);

        for (String id : ids) {
            String date = config.getString("archives." + id + ".date");
            String issuedBy = config.getString("archives." + id + ".issuedBy");
            List<String> notes = config.getStringList("archives." + id + ".notes");

            sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + id + ChatColor.GRAY + " | " + ChatColor.YELLOW + "Date: " + ChatColor.WHITE + date);
            sender.sendMessage(ChatColor.YELLOW + "Issued by: " + ChatColor.AQUA + issuedBy);
            for (String note : notes) {
                sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.translateAlternateColorCodes('&', note));
            }
            sender.sendMessage("");
        }
        sender.sendMessage(ChatColor.GOLD + "------------------------------");
    }
}
