package me.molfordan.arenaAndFFAManager.commands.admin;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleBridgeEggCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;
    private static boolean bridgeEggEnabled = true;

    public ToggleBridgeEggCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!(sender.isOp() || sender.hasPermission("arenamap.admin"))) {
            return true;
        }

        bridgeEggEnabled = !bridgeEggEnabled;

        if (!bridgeEggEnabled) {
            // Cancel all active bridge egg tasks
            plugin.getEggBridgeManager().cancelAllTasks();
            
            String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getServerPrefix() + 
                ChatColor.RED + "Bridge egg has been " + ChatColor.BOLD + "DISABLED");
            
            if (sender instanceof Player) {
                sender.sendMessage(message);
            }
            
            // Notify all admins
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp() || player.hasPermission("arenamap.admin")) {
                    player.sendMessage(message);
                }
            }
        } else {
            String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getServerPrefix() + 
                ChatColor.GREEN + "Bridge egg has been " + ChatColor.BOLD + "ENABLED");
            
            if (sender instanceof Player) {
                sender.sendMessage(message);
            }
            
            // Notify all admins
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp() || player.hasPermission("arenamap.admin")) {
                    player.sendMessage(message);
                }
            }
        }

        return true;
    }

    public static boolean isBridgeEggEnabled() {
        return bridgeEggEnabled;
    }
}
