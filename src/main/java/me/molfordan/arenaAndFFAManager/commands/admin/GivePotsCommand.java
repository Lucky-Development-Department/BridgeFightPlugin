package me.molfordan.arenaAndFFAManager.commands.admin;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class GivePotsCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public GivePotsCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        
        if (!player.hasPermission("arenamap.admin") || !player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /givepots <type> <amount>");
            player.sendMessage(ChatColor.GRAY + "Available types: invis, speed, strength, resistance, jump");
            return true;
        }

        String type = args[0].toLowerCase();
        int amount;
        
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount specified.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
            return true;
        }

        // Limit amount to prevent inventory overflow or lag
        if (amount > 36) amount = 36;

        for (int i = 0; i < amount; i++) {
            switch (type) {
                case "invis":
                    plugin.getDeathMessageManager().givePotion(player, PotionEffectType.INVISIBILITY, 45, 1, ChatColor.AQUA + "Invisibility II Potion (45s)");
                    break;
                case "speed":
                    plugin.getDeathMessageManager().givePotion(player, PotionEffectType.SPEED, 30, 1, ChatColor.RED + "Speed Potion (30s)");
                    break;
                case "strength":
                    plugin.getDeathMessageManager().givePotion(player, PotionEffectType.INCREASE_DAMAGE, 30, 1, ChatColor.RED + "Strength Potion (30s)");
                    break;
                case "resistance":
                    plugin.getDeathMessageManager().givePotion(player, PotionEffectType.DAMAGE_RESISTANCE, 45, 2, ChatColor.AQUA + "Resistance Potion (45s)");
                    break;
                case "jump":
                    plugin.getDeathMessageManager().givePotion(player, PotionEffectType.JUMP, 45, 4, ChatColor.LIGHT_PURPLE + "Jump Boost V (45s)");
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Unknown potion type: " + type);
                    return true;
            }
        }

        player.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + type + " potion(s).");
        return true;
    }
}
