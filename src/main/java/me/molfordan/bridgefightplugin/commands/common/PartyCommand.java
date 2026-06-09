package me.molfordan.bridgefightplugin.commands.common;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.PartyManager;
import me.molfordan.bridgefightplugin.object.Party;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PartyCommand implements CommandExecutor {

    private final BridgeFightPlugin plugin;
    private final PartyManager partyManager;
    private final java.util.List<String> subcommands = java.util.Arrays.asList(
            "create", "invite", "join", "leave", "kick", "promote", "chat", "info", "list", "disband", "challenge", "acceptchallenge", "split", "settings"
    );

    public PartyCommand(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        
        // Handle alias: /bfparty <player> -> /bfparty invite <player>
        if (!subcommands.contains(sub) && Bukkit.getPlayer(sub) != null) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "invite";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
            sub = "invite";
        }

        switch (sub) {
            case "create":
                if (plugin.getBedFightManager().isInMatch(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot create a party while in a match!");
                    return true;
                }
                if (partyManager.isInParty(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are already in a party!");
                } else {
                    partyManager.createParty(player);
                    player.sendMessage(ChatColor.GREEN + "Party created!");
                    partyManager.givePartyItems(player);
                }
                break;
            case "invite":
                if (!partyManager.isInParty(player.getUniqueId())) {
                    if (plugin.getBedFightManager().isInMatch(player)) {
                        player.sendMessage(ChatColor.RED + "You cannot do that during the match!");
                        return true;
                    }
                    partyManager.createParty(player);
                    player.sendMessage(ChatColor.GREEN + "Party created!");
                    partyManager.givePartyItems(player);
                }

                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You must be the party leader to invite players.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot invite yourself.");
                    return true;
                }
                
                Party party = partyManager.getParty(player.getUniqueId());
                if (party != null && party.getMembers().size() >= party.getMaxSize()) {
                    player.sendMessage(ChatColor.RED + "Your party is full!");
                    return true;
                }

                partyManager.addInvite(player.getUniqueId(), target.getUniqueId());
                
                String inviteMsg = ChatColor.GREEN + player.getName() + ChatColor.YELLOW + " invited you to their party! " + ChatColor.GREEN + "" + ChatColor.BOLD + "(Click here to join)";
                net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(inviteMsg);
                component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/bfparty join " + player.getName()));
                target.spigot().sendMessage(component);
                
                player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to the party!");
                break;
            case "join":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty join <leader>");
                    return true;
                }
                Player leader = Bukkit.getPlayer(args[1]);
                if (leader != null) {
                    Party targetParty = partyManager.getParty(leader.getUniqueId());
                    if (partyManager.hasInvite(player.getUniqueId(), leader.getUniqueId()) || (targetParty != null && targetParty.isOpen())) {
                        if (partyManager.isInParty(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "You must leave your current party first!");
                            return true;
                        }
                        if (partyManager.addMember(leader, player)) {
                            partyManager.removeInvite(player.getUniqueId(), leader.getUniqueId());
                            partyManager.givePartyItems(player);
                        } else {
                            player.sendMessage(ChatColor.RED + "That party is full!");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "No invitation found from that leader, and the party is not open.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Leader not found.");
                }
                break;
            case "kick":
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are not the party leader.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty kick <player>");
                    return true;
                }
                Player toKick = Bukkit.getPlayer(args[1]);
                if (toKick == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (toKick.equals(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot kick yourself. Use /bfparty leave instead.");
                    return true;
                }
                partyManager.kickPlayer(player, toKick.getUniqueId());
                break;
            case "promote":
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are not the party leader.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty promote <player>");
                    return true;
                }
                Player toPromote = Bukkit.getPlayer(args[1]);
                if (toPromote == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (toPromote.equals(player)) {
                    player.sendMessage(ChatColor.RED + "You are already the leader.");
                    return true;
                }
                partyManager.promotePlayer(player, toPromote.getUniqueId());
                break;
            case "chat":
                if (!partyManager.isInParty(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                
                // If no message or explicit "toggle", toggle the chat mode
                if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("toggle"))) {
                    partyManager.togglePartyChat(player.getUniqueId());
                    boolean toggled = partyManager.isPartyChatToggled(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "Party chat toggle: " + (toggled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                    return true;
                }
                
                // Send message
                StringBuilder chatMsg = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    chatMsg.append(args[i]).append(" ");
                }
                partyManager.broadcast(partyManager.getPartyLeader(player.getUniqueId()), ChatColor.AQUA + player.getName() + ": " + ChatColor.WHITE + chatMsg.toString().trim());
                break;
            case "split":
                if (plugin.getBedFightManager().isInMatch(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot do that during the match!");
                    return true;
                }
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can split the party.");
                    return true;
                }
                plugin.getMatchmakingService().addToQueue(player, QueueType.PARTY_SPLIT);
                break;
            case "settings":
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can change settings.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty settings <open|maxsize> <value>");
                    return true;
                }
                Party p = partyManager.getParty(player.getUniqueId());
                if (p == null) return true;
                if (args[1].equalsIgnoreCase("open")) {
                    boolean open = Boolean.parseBoolean(args[2]);
                    p.setOpen(open);
                    player.sendMessage(ChatColor.YELLOW + "Party is now " + (open ? ChatColor.GREEN + "OPEN" : ChatColor.RED + "CLOSED"));
                } else if (args[1].equalsIgnoreCase("maxsize")) {
                    try {
                        int size = Integer.parseInt(args[2]);
                        if (size < 2 || size > 16) {
                            player.sendMessage(ChatColor.RED + "Size must be between 2 and 16.");
                            return true;
                        }
                        p.setMaxSize(size);
                        player.sendMessage(ChatColor.YELLOW + "Party max size set to " + ChatColor.AQUA + size);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number.");
                    }
                }
                break;
            case "challenge":
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can challenge other parties.");
                    return true;
                }
                if (plugin.getBedFightManager().isInMatch(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot challenge other parties during the match!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty challenge <leader>");
                    return true;
                }
                Player targetLeader = Bukkit.getPlayer(args[1]);
                if (targetLeader == null || !partyManager.isLeader(targetLeader.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "That player is not a party leader.");
                    return true;
                }
                if (targetLeader.equals(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot challenge yourself.");
                    return true;
                }
                partyManager.addChallenge(player.getUniqueId(), targetLeader.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "Challenge sent to " + targetLeader.getName() + "'s party!");
                
                String chalMsg = ChatColor.LIGHT_PURPLE + "[Party] " + ChatColor.YELLOW + player.getName() + "'s party has challenged you to a BedFight! " + ChatColor.GREEN + "" + ChatColor.BOLD + "(Click here to accept)";
                net.md_5.bungee.api.chat.TextComponent chalComp = new net.md_5.bungee.api.chat.TextComponent(chalMsg);
                chalComp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/bfparty acceptchallenge " + player.getName()));
                targetLeader.spigot().sendMessage(chalComp);
                break;
            case "acceptchallenge":
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can accept challenges.");
                    return true;
                }
                if (plugin.getBedFightManager().isInMatch(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot accept challenge during the match!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bfparty acceptchallenge <leader>");
                    return true;
                }
                Player challenger = Bukkit.getPlayer(args[1]);
                if (challenger != null && partyManager.hasChallenge(challenger.getUniqueId(), player.getUniqueId())) {
                    partyManager.removeChallenge(challenger.getUniqueId());
                    
                    // Start Match
                    Set<UUID> team1 = partyManager.getPartyMembers(challenger.getUniqueId());
                    Set<UUID> team2 = partyManager.getPartyMembers(player.getUniqueId());
                    
                    me.molfordan.bridgefightplugin.object.Arena arena = null;
                    java.util.List<me.molfordan.bridgefightplugin.object.Arena> arenas = new java.util.ArrayList<>(plugin.getBedFightArenaManager().getArenas());
                    if (!arenas.isEmpty()) {
                        arena = arenas.get(new java.util.Random().nextInt(arenas.size()));
                    }
                    
                    if (arena != null) {
                        plugin.getBedFightManager().startMatch(arena, QueueType.PARTY_FIGHT, team1, team2);
                    } else {
                        player.sendMessage(ChatColor.RED + "No arenas available.");
                        challenger.sendMessage(ChatColor.RED + "No arenas available.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "No pending challenge from that leader.");
                }
                break;
            case "info":
            case "list":
                if (!partyManager.isInParty(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                UUID leaderId = partyManager.getPartyLeader(player.getUniqueId());
                Set<UUID> members = partyManager.getPartyMembers(leaderId);
                player.sendMessage(ChatColor.GOLD + "--- Party Info ---");
                player.sendMessage(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(leaderId).getName());
                player.sendMessage(ChatColor.YELLOW + "Members (" + members.size() + "):");
                for (UUID memberId : members) {
                    player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + Bukkit.getOfflinePlayer(memberId).getName());
                }
                break;
            case "disband":
                if (plugin.getBedFightManager().isInMatch(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot disband your party while in a match!");
                    return true;
                }
                if (!partyManager.isLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are not a party leader.");
                    return true;
                }
                plugin.getMatchmakingService().removeFromQueue(player);
                Set<UUID> partyMembers = partyManager.getPartyMembers(player.getUniqueId());
                if (partyMembers != null) {
                    for (UUID memberId : new HashSet<>(partyMembers)) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null) {
                            member.getInventory().clear();
                            plugin.getSpawnItem().giveSpawnItem(member);
                        }
                    }
                }
                partyManager.disbandParty(player);
                player.sendMessage(ChatColor.RED + "Party disbanded.");
                break;
            case "leave":
                if (!partyManager.isInParty(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                plugin.getMatchmakingService().removeFromQueue(player);
                partyManager.leaveParty(player);
                player.getInventory().clear();
                plugin.getSpawnItem().giveSpawnItem(player);
                player.sendMessage(ChatColor.YELLOW + "You left the party.");
                break;
            default:
                sendUsage(player);
                break;
        }
        return true;
    }

    private void sendUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "--- Party Commands ---");
        p.sendMessage(ChatColor.YELLOW + "/bfparty create" + ChatColor.WHITE + " - Create a party");
        p.sendMessage(ChatColor.YELLOW + "/bfparty invite <player>" + ChatColor.WHITE + " - Invite a player");
        p.sendMessage(ChatColor.YELLOW + "/bfparty join <leader>" + ChatColor.WHITE + " - Join a party");
        p.sendMessage(ChatColor.YELLOW + "/bfparty leave" + ChatColor.WHITE + " - Leave your party");
        p.sendMessage(ChatColor.YELLOW + "/bfparty kick <player>" + ChatColor.WHITE + " - Kick a member");
        p.sendMessage(ChatColor.YELLOW + "/bfparty promote <player>" + ChatColor.WHITE + " - Promote a member to leader");
        p.sendMessage(ChatColor.YELLOW + "/bfparty chat <msg|toggle>" + ChatColor.WHITE + " - Party chat");
        p.sendMessage(ChatColor.YELLOW + "/bfparty settings <open|maxsize>" + ChatColor.WHITE + " - Party settings");
        p.sendMessage(ChatColor.YELLOW + "/bfparty split" + ChatColor.WHITE + " - Split party for match");
        p.sendMessage(ChatColor.YELLOW + "/bfparty list/info" + ChatColor.WHITE + " - View party members");
        p.sendMessage(ChatColor.YELLOW + "/bfparty disband" + ChatColor.WHITE + " - Disband the party (Leader only)");
    }
}
