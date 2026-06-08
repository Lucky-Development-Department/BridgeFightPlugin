package me.molfordan.arenaAndFFAManager.commands.common;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PartyTabCompleter implements TabCompleter {

    private final List<String> subcommands = Arrays.asList(
            "create", "invite", "join", "leave", "kick", "promote", "chat", "info", "list", "disband", "challenge", "acceptchallenge", "split", "settings"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    matches.add(sub);
                }
            }
            return matches;
        }
        
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("join") || sub.equals("kick") || sub.equals("promote") || sub.equals("challenge") || sub.equals("acceptchallenge")) {
                List<String> matches = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        matches.add(p.getName());
                    }
                }
                return matches;
            }
            if (sub.equals("chat")) {
                return Arrays.asList("toggle");
            }
            if (sub.equals("settings")) {
                return Arrays.asList("open", "maxsize");
            }
        }
        
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("settings")) {
                if (args[1].equalsIgnoreCase("open")) {
                    return Arrays.asList("true", "false");
                }
                if (args[1].equalsIgnoreCase("maxsize")) {
                    return Arrays.asList("2", "4", "8", "16");
                }
            }
        }
        
        return Collections.emptyList();
    }
}
