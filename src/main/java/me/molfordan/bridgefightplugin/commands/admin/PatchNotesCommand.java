package me.molfordan.bridgefightplugin.commands.admin;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PatchNotesManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PatchNotesCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;

    public PatchNotesCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getPatchNotesManager().displayPage(sender, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        // Admin commands check
        if (sub.equals("reload") || sub.equals("create") || sub.equals("add") || sub.equals("finish") || sub.equals("cancel")) {
            if (!sender.isOp() && !sender.hasPermission("arenamap.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
        }

        if (sub.equals("reload")) {
            plugin.getPatchNotesManager().reload();
            sender.sendMessage(ChatColor.GREEN + "Patch notes archive reloaded!");
            return true;
        }

        if (sub.equals("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can create patch notes.");
                return true;
            }
            Player p = (Player) sender;
            if (plugin.getPatchNotesManager().hasActiveSession(p.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You already have an active session! Use /patchnotes finish or cancel.");
                return true;
            }
            String id = plugin.getPatchNotesManager().startSession(p.getUniqueId(), p.getName());
            sender.sendMessage(ChatColor.GREEN + "Started patch note session ID: " + ChatColor.WHITE + id);
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/patchnotes add <text>" + ChatColor.YELLOW + " to add notes.");
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/patchnotes finish" + ChatColor.YELLOW + " to archive.");
            return true;
        }

        if (sub.equals("add")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            PatchNotesManager.PatchNoteSession session = plugin.getPatchNotesManager().getSession(p.getUniqueId());
            if (session == null) {
                sender.sendMessage(ChatColor.RED + "No active session! Start one with /patchnotes create");
                return true;
            }

            if (args.length < 2) {
                // Toggle listening mode
                session.isListening = !session.isListening;
                if (session.isListening) {
                    sender.sendMessage(ChatColor.GREEN + "Chat listening mode enabled. Your chat messages will be added as notes.");
                    sender.sendMessage(ChatColor.YELLOW + "Type '" + ChatColor.GREEN + "/patchnotes finish" + ChatColor.YELLOW + "' to save.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Chat listening mode disabled.");
                }
                return true;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            plugin.getPatchNotesManager().addNoteToSession(p.getUniqueId(), sb.toString().trim());
            sender.sendMessage(ChatColor.GRAY + "Note added to session.");
            return true;
        }

        if (sub.equals("finish")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (!plugin.getPatchNotesManager().hasActiveSession(p.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "No active session to finish.");
                return true;
            }
            plugin.getPatchNotesManager().finishSession(p.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Patch notes archived successfully!");
            return true;
        }

        if (sub.equals("cancel")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            plugin.getPatchNotesManager().cancelSession(p.getUniqueId());
            sender.sendMessage(ChatColor.YELLOW + "Patch note session cancelled.");
            return true;
        }

        if (sub.equals("all")) {
            plugin.getPatchNotesManager().displayAll(sender);
            return true;
        }

        // Try parsing as page number
        try {
            int page = Integer.parseInt(sub);
            plugin.getPatchNotesManager().displayPage(sender, page);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Usage: /patchnotes <all|reload|create|add|finish|cancel|[page]>");
        }

        return true;
    }
}
