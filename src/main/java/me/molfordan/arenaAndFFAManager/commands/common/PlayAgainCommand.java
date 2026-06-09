package me.molfordan.arenaAndFFAManager.commands.common;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.queue.enums.QueueType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public class PlayAgainCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public PlayAgainCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        long endTime = plugin.getBedFightManager().getGameEndTime(player.getUniqueId());
        
        if (System.currentTimeMillis() - endTime > 45000) {
            player.sendMessage(ChatColor.RED + "The play again period has expired!");
            return true;
        }

        // Manually perform cleanup instead of /leave command
        plugin.getBedFightManager().removePlayerFromSession(player);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard());

        MetadataValue meta = player.getMetadata("lastQueueType").stream().findFirst().orElse(null);
        if (meta != null) {
            try {
                QueueType type = QueueType.valueOf(meta.asString());
                plugin.getMatchmakingService().addToQueue(player, type);
                player.removeMetadata("lastQueueType", plugin);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Could not determine last queue type.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Could not find your last queue!");
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getBedFightManager().isInMatch(player)) return;

            if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                plugin.getMatchmakingService().giveLeaveItem(player);
                return;
            }
            
            if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
                plugin.getPartyManager().givePartyItems(player);
            } else {
                plugin.getSpawnItem().giveSpawnItem(player);
            }
        }, 1L);

        return true;
    }
}
