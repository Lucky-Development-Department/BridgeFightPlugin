package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.manager.CombatManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BypassCommandsCommand implements CommandExecutor {

    private CombatManager combatManager;

    public BypassCommandsCommand(CombatManager combatManager) {
        this.combatManager = combatManager;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender == null) return true;

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;



        if (!player.isOp()) {
            player.sendMessage("You must be an operator to use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {

            String status = combatManager.isBypassing(player.getUniqueId())
                    ? ChatColor.GREEN + "ENABLED"
                    : ChatColor.RED + "DISABLED";

            player.sendMessage(ChatColor.GOLD + "Command bypass Status: " + status);
            return true;
        }

        combatManager.toggleBypass(player.getUniqueId());

        String status = combatManager.isBypassing(player.getUniqueId())
                ? ChatColor.GREEN + "ENABLED"
                : ChatColor.RED + "DISABLED";



        player.sendMessage(ChatColor.GOLD + "Command bypass: " + status);
        return true;
    }
}
