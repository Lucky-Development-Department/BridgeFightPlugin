package me.molfordan.bridgefightplugin.cosmetics.crate;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class SetupCrateCommand implements CommandExecutor {

    public static final String SETUP_BLOCK_NAME = ChatColor.GREEN + "" + ChatColor.BOLD + "Cosmetic Crate Setup Block";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("bridgefight.setupcrate")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        ItemStack setupBlock = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = setupBlock.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SETUP_BLOCK_NAME);
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Place this block to register a Cosmetic Crate location."));
            setupBlock.setItemMeta(meta);
        }

        player.getInventory().addItem(setupBlock);
        player.sendMessage(ChatColor.GREEN + "You have been given a Cosmetic Crate Setup Block. Place it to register a crate!");
        return true;
    }
}
