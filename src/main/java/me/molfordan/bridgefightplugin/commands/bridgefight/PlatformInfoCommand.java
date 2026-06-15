package me.molfordan.bridgefightplugin.commands.bridgefight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PlatformManager;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class PlatformInfoCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public PlatformInfoCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("arenamap.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /platform info");
            return true;
        }

        Player player = (Player) sender;
        PlatformManager pm = plugin.getPlatformManager();
        PlatformRegion region = pm.fromLocation(player.getLocation());

        if (region == null) {
            player.sendMessage(ChatColor.RED + "You are not in a platform.");
            return true;
        }

        String platformName = "Unknown";
        for (Map.Entry<String, PlatformRegion> entry : pm.getAllPlatforms().entrySet()) {
            if (entry.getValue() == region) {
                platformName = entry.getKey();
                break;
            }
        }

        player.sendMessage(ChatColor.GREEN + "--- Platform Info ---");
        player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + platformName);
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + (region.getType() != null ? region.getType().name() : "None"));
        return true;
    }
}
