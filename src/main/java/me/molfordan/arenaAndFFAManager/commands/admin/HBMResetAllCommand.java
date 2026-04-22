package me.molfordan.arenaAndFFAManager.commands.admin;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class HBMResetAllCommand implements CommandExecutor, TabCompleter {

    private final ArenaAndFFAManager plugin;

    public HBMResetAllCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        sender.sendMessage("§aResetting all Hotbar Manager data...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getHotbarDataManager().resetAll();
            sender.sendMessage("§aSuccessfully reset all Hotbar Manager data from the database.");
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return new ArrayList<>();
    }
}
