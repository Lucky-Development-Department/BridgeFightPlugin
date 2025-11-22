package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrivateWorldCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public PrivateWorldCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (sender == null) return true;



        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;

        if (!(player.isOp() || player.hasPermission("privateworld.access"))) return true;

        Location privateWorldLoc = plugin.getConfigManager().getPrivateWorldLocation();

        if (privateWorldLoc == null){
            player.sendMessage("&cFailed to send you to the PrivateWorld, maybe the world is null?");
            return true;
        }

        player.teleport(privateWorldLoc);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SURVIVAL);
        plugin.getSpawnItem().giveSword(player);

        return true;
    }
}
