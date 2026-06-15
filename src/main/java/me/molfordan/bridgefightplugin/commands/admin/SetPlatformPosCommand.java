package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PlatformManager;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPlatformPosCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;
    private final PlatformManager platformManager;

    public SetPlatformPosCommand(BridgeFightPlugin plugin, PlatformManager pm) {
        this.plugin = plugin;
        this.platformManager = pm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.isOp()) return true;

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

        platformManager.savePlatformPos(plat, posKey, loc);

        p.sendMessage("§aSet " + posKey + " for §e" + plat + "§a!");
        return true;
    }

    private void saveLocation(String baseKey, Location loc) {
        // This method is now redundant, but I'll leave it to avoid breaking changes if it's called elsewhere, 
        // though I am only modifying SetPlatformPosCommand here.
        // Actually, since I have PlatformManager.savePlatformPos, I can just use that.
        // I'll keep the original structure for now.
    }
}
