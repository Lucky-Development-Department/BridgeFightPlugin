package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.PlatformManager;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPlatformPosCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;
    private final PlatformManager platformManager;

    public SetPlatformPosCommand(ArenaAndFFAManager plugin, PlatformManager pm) {
        this.plugin = plugin;
        this.platformManager = pm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("arenamap.admin")) {
            p.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length != 1) {
            p.sendMessage("§cUsage: /" + label + " <plat>");
            return true;
        }

        String plat = args[0].toLowerCase();
        PlatformRegion region = platformManager.getPlatform(plat);

        if (region == null) {
            p.sendMessage("§cPlatform §e" + plat + " §cdoes not exist.");
            return true;
        }

        Location loc = p.getLocation();
        String posKey = label.equalsIgnoreCase("setpos1") ? "pos1" : "pos2";

        if (posKey.equals("pos1")) region.setPos1(loc);
        else region.setPos2(loc);

        saveLocation("platforms." + plat + "." + posKey, loc);

        p.sendMessage("§aSet " + posKey + " for §e" + plat + "§a!");
        return true;
    }

    private void saveLocation(String baseKey, Location loc) {
        plugin.getBridgeFightConfig().getConfig().set(baseKey + ".world", loc.getWorld().getName());
        plugin.getBridgeFightConfig().getConfig().set(baseKey + ".x", loc.getX());
        plugin.getBridgeFightConfig().getConfig().set(baseKey + ".y", loc.getY());
        plugin.getBridgeFightConfig().getConfig().set(baseKey + ".z", loc.getZ());
        plugin.getBridgeFightConfig().getConfig().set(baseKey + ".yaw", loc.getYaw());
        plugin.getBridgeFightConfig().getConfig().set(baseKey + ".pitch", loc.getPitch());
        plugin.getBridgeFightConfig().save();
    }
}
