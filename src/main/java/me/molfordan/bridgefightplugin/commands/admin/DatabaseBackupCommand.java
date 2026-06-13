package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class DatabaseBackupCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public DatabaseBackupCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenamap.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
            return true;
        }

        // Handle Redis Flush
        if (args.length > 0 && args[0].equalsIgnoreCase("redis-flush")) {
            plugin.getStatsManager().clearCache();
            sender.sendMessage(ChatColor.GREEN + "Redis statistics cache has been flushed!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting database backup process...");

        // Run asynchronously to prevent server lag
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean localSuccess = plugin.getBackupManager().backupMySQL();
            boolean remoteSuccess = plugin.getBackupManager().syncToRemoteDatabase();
            
            // Send result back on main thread
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (localSuccess) {
                    sender.sendMessage(ChatColor.GREEN + "Local SQL backup completed successfully!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Local SQL backup failed!");
                }

                // Only show remote status if mirroring is actually enabled
                FileConfiguration dbCfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "database.yml"));
                if (dbCfg.getBoolean("mysql_remote_backup.enabled", false)) {
                    if (remoteSuccess) {
                        sender.sendMessage(ChatColor.GREEN + "Remote database mirroring completed successfully!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Remote database mirroring failed! Check console.");
                    }
                }
            });
        });

        return true;
    }
}
