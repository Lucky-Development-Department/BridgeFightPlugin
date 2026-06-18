package me.molfordan.bridgefightplugin.commands.bridgefight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PlatformManager;
import me.molfordan.bridgefightplugin.kits.KitManager;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlatformCommand extends BukkitCommand {

    private final BridgeFightPlugin plugin;
    private final KitManager kitManager;
    private final PlatformManager platformManager;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public PlatformCommand(String name, BridgeFightPlugin plugin, KitManager kitManager, PlatformManager platformManager) {
        super(name);
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.platformManager = platformManager;

        this.setPermissionMessage("§cYou don't have permission.");
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        String cmd = this.getName().toLowerCase(); // Use registered name instead of alias

        // =============== SET PLATFORM SPAWN ===============
        if (cmd.startsWith("set")) {
            if (!p.hasPermission("arenamap.admin")) {
                p.sendMessage("§cYou don't have permission.");
                return true;
            }

            String plat = cmd.substring(3); // remove "set"
            PlatformRegion region = platformManager.getPlatform(plat);

            if (region == null) {
                p.sendMessage("§cPlatform §e" + plat + " §cdoes not exist.");
                return true;
            }

            // Explicitly set type if it's a new platform or type not set
            if (plat.startsWith("boxingplat")) {
                region.setType(me.molfordan.bridgefightplugin.object.enums.PlatformType.BOXINGPLAT);
            } else if (plat.startsWith("bigplat")) {
                region.setType(me.molfordan.bridgefightplugin.object.enums.PlatformType.BIGPLAT);
            } else if (plat.startsWith("plat")) {
                region.setType(me.molfordan.bridgefightplugin.object.enums.PlatformType.PLAT);
            }

            Location loc = p.getLocation();
            region.setSpawn(loc);

            saveLocation("platforms." + plat + ".spawn", loc);
            p.sendMessage("§aSpawn for §e" + plat + " §ahas been set!");
            return true;
        }

        // =============== TELEPORT ===============
        if (cmd.startsWith("plat") || cmd.startsWith("bigplat") || cmd.startsWith("boxingplat")) {
            if (plugin.getBridgeFightBanManager().isPlayerBanned(p.getUniqueId())) {

                long expire = plugin.getBridgeFightBanManager().getBanExpire(p.getUniqueId());
                String reason = plugin.getBridgeFightBanManager().getBanReason(p.getUniqueId());

                if (expire == -1) {
                    // permanent ban
                    p.sendMessage("§cYou are permanently banned from BridgeFight.");
                    p.sendMessage("§7Reason: §f" + reason);
                    return true;
                }

                long remaining = expire - System.currentTimeMillis();
                String formatted = formatRemaining(remaining);

                p.sendMessage("§cYou are banned from BridgeFight.");
                p.sendMessage("§7Reason: §f" + reason);
                p.sendMessage("§7Remaining: §e" + formatted);
                return true;
            }

            PlayerStats playerStats = plugin.getStatsManager().getStats(p.getUniqueId());
            if (playerStats.getBuildKills() < 350) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cYou need atleast 350 BuildFFA kills or higher to join the bridge fight!" ));
                return true;
            }

            long now = System.currentTimeMillis();
            long cd = cooldown.getOrDefault(p.getUniqueId(), 0L);

            if (now < cd) {
                long remaining = (cd - now) / 1000;
                p.sendMessage("§cYou must wait §e" + remaining + "s §cbefore using this command again.");
                return true;
            }

            // set new cooldown
            cooldown.put(p.getUniqueId(), now + 3000);


            String plat = cmd.toLowerCase();
            PlatformRegion region = platformManager.getExistingPlatform(plat);  // FIXED

            if (region == null || region.getSpawn() == null) {
                p.sendMessage("§cSpawn is not set for " + plat + ".");
                return true;
            }

            Location loc = region.getSpawn().clone();

            if (loc.getWorld() == null) {
                p.sendMessage("§cWorld for " + plat + " is not loaded!");
                return true;
            }

            p.teleport(loc);
            kitManager.applyBridgeFightKit(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.sendMessage("§aTeleported to §e" + plat + "§a!");
            return true;
        }

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

    private String formatRemaining(long millis) {
        if (millis <= 0) return "0s";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();

        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
