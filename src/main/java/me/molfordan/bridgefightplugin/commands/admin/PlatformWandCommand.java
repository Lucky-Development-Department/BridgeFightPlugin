package me.molfordan.bridgefightplugin.commands.admin;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlatformWandCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        
        Player p = (Player) sender;
        if (!p.isOp()) return true;

        if (args.length != 1) {
            p.sendMessage("§cUsage: /platformwand <name>");
            return true;
        }

        String name = args[0];
        ItemStack wand = new ItemStack(Material.STONE_AXE);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("§aPlatform Wand: " + name);
        wand.setItemMeta(meta);
        
        p.getInventory().addItem(wand);
        p.sendMessage("§aYou received a Platform Wand for §e" + name + "§a.");
        return true;
    }
}
