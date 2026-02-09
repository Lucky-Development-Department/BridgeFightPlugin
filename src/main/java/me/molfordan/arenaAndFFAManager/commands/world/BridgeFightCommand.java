package me.molfordan.arenaAndFFAManager.commands.world;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.listener.LobbyListener;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BridgeFightCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final ArenaAndFFAManager plugin;
    private final LobbyListener lobbyListener;

    public BridgeFightCommand(ConfigManager configManager, ArenaAndFFAManager plugin, LobbyListener lobbyListener){
        this.configManager = configManager;
        this.plugin = plugin;
        this.lobbyListener = lobbyListener;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) return true;

        String prefix = configManager.getServerPrefix();

        Player player = (Player) sender;

        Location bridgeFightLoc = configManager.getBridgeFightLocation();

        String worldName = configManager.getBridgeFightWorldName();

        if (bridgeFightLoc == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cFailed to send you to the BridgeFight, Please Contact Admins."));
            return true;
        }
        /*

        if (worldName == null) return true;

        if (player.getWorld().getName().equals(worldName)){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cYou are already connected to this server"));
            return true;
        }

         */
        if (plugin.getBridgeFightBanManager().isPlayerBanned(player.getUniqueId())){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cYou are banned from BridgeFight"));
            return true;
        }



        // Add player to bridge fight spawn recipients list
        lobbyListener.addBridgeFightSpawnRecipient(player.getUniqueId());
        
        player.teleport(bridgeFightLoc);
        player.getInventory().clear();
        if (player.getInventory().getArmorContents() != null) {
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " sending you to BridgeFight...."));

        // Remove the direct call to giveBridgeFightSpawnItem since it will be handled by LobbyListener
        /*
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSpawnItem().giveBridgeFightSpawnItem(player);
        }, 1);
        */

        return true;
    }
}
