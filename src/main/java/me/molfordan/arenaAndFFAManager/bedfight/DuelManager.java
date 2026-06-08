package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.queue.enums.QueueType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelManager {
    private final ArenaAndFFAManager plugin;
    private final Map<UUID, UUID> requests = new HashMap<>(); // Target -> Sender
    private final Map<UUID, BukkitRunnable> requestTimeouts = new HashMap<>(); // Target -> Runnable

    public DuelManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target) {
        requests.put(target.getUniqueId(), sender.getUniqueId());
        
        var arena = plugin.getBedFightArenaManager().getArenas().stream().findAny().orElse(null);
        String mapName = (arena != null) ? arena.getName() : "Random";

        sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Bedfight Duel Sent");
        sender.sendMessage(ChatColor.YELLOW + "● To: " + ChatColor.GREEN + target.getName());
        sender.sendMessage(ChatColor.YELLOW + "● Map: " + ChatColor.WHITE + mapName);

        // Cancel previous timeout if it exists
        if (requestTimeouts.containsKey(target.getUniqueId())) {
            requestTimeouts.get(target.getUniqueId()).cancel();
        }

        // Setup timeout
        BukkitRunnable timeout = new BukkitRunnable() {
            @Override
            public void run() {
                if (requests.containsKey(target.getUniqueId()) && requests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
                    requests.remove(target.getUniqueId());
                    target.sendMessage(ChatColor.RED + "Duel request from " + sender.getName() + " has expired!");
                    sender.sendMessage(ChatColor.RED + "Duel request to " + target.getName() + " has expired!");
                }
            }
        };
        timeout.runTaskLater(plugin, 30 * 20L);
        requestTimeouts.put(target.getUniqueId(), timeout);

        // Received message
        target.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Bedfight Duel Request");
        target.sendMessage(ChatColor.YELLOW + "● From: " + ChatColor.GREEN + sender.getName() + " (" + getPing(sender) + "ms)");
        target.sendMessage(ChatColor.YELLOW + "● Map: " + ChatColor.WHITE + mapName);

        TextComponent accept = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "(Click to accept)");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to accept duel").create()));
        
        target.spigot().sendMessage(accept);
    }

    private int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            return -1;
        }
    }

    public void acceptRequest(Player accepter, Player sender) {
        if (!requests.containsKey(accepter.getUniqueId()) || !requests.get(accepter.getUniqueId()).equals(sender.getUniqueId())) {
            accepter.sendMessage(ChatColor.RED + "No active duel request from " + sender.getName());
            return;
        }

        requests.remove(accepter.getUniqueId());
        
        // Play sound
        accepter.playSound(accepter.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
        sender.playSound(sender.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);

        accepter.sendMessage(ChatColor.GREEN + "Duel accepted!");
        sender.sendMessage(ChatColor.GREEN + accepter.getName() + " accepted your duel!");

        // Start Duel Match - Picking random arena
        var arena = plugin.getBedFightArenaManager().getArenas().stream().findAny().orElse(null);
        if (arena == null) {
            accepter.sendMessage(ChatColor.RED + "No arenas available for duels.");
            sender.sendMessage(ChatColor.RED + "No arenas available for duels.");
            return;
        }
        
        java.util.Set<UUID> redTeam = new java.util.HashSet<>(java.util.Collections.singletonList(sender.getUniqueId()));
        java.util.Set<UUID> blueTeam = new java.util.HashSet<>(java.util.Collections.singletonList(accepter.getUniqueId()));

        plugin.getBedFightManager().startMatch(arena, QueueType.DUEL, redTeam, blueTeam);
    }
}
