package me.molfordan.bridgefightplugin.cosmetics;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.cosmetics.gui.CosmeticsGUI;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import me.molfordan.bridgefightplugin.manager.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsCommand implements CommandExecutor, TabCompleter {

    private final CosmeticsGUI cosmeticsGUI;
    private final CosmeticsManager cosmeticsManager;

    public CosmeticsCommand(CosmeticsGUI cosmeticsGUI, CosmeticsManager cosmeticsManager) {
        this.cosmeticsGUI = cosmeticsGUI;
        this.cosmeticsManager = cosmeticsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bridgefight.cosmetics.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload cosmetics.");
                return true;
            }

            cosmeticsManager.loadConfig();
            sender.sendMessage(ChatColor.GREEN + "Reloaded cosmetics.yml.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("bridgefight.cosmetics.reset")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reset cosmetics.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /cosmetics reset <player>");
                return true;
            }

            String targetName = args[1];
            OfflinePlayer target = null;
            for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {
                if (off.getName() != null && off.getName().equalsIgnoreCase(targetName)) {
                    target = off;
                    break;
                }
            }

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player has never joined before.");
                return true;
            }

            StatsManager statsManager = BridgeFightPlugin.getInstance().getStatsManager();
            PlayerStats stats = statsManager.getOrLoad(target.getUniqueId(), target.getName() == null ? "Unknown" : target.getName());
            if (stats == null) {
                sender.sendMessage(ChatColor.RED + "Failed to load player stats.");
                return true;
            }

            stats.setPurchasedKillMessages(new ArrayList<>());
            stats.setPurchasedKillEffects(new ArrayList<>());
            stats.setPurchasedTrails(new ArrayList<>());
            stats.setSelectedKillMessage("default");
            stats.setSelectedKillEffect("none");
            stats.setSelectedTrail("none");

            statsManager.savePlayerAsync(stats);

            sender.sendMessage(ChatColor.GREEN + "Successfully reset all purchased cosmetics for " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Usage: /cosmetics reload or /cosmetics reset <player>");
            return true;
        }

        Player player = (Player) sender;
        cosmeticsGUI.openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("bridgefight.cosmetics.reload") && "reload".startsWith(args[0].toLowerCase())) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("bridgefight.cosmetics.reset") && "reset".startsWith(args[0].toLowerCase())) {
                suggestions.add("reset");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            if (sender.hasPermission("bridgefight.cosmetics.reset")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(p.getName());
                    }
                }
            }
        }
        return suggestions;
    }
}
