package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpectatorListener implements Listener {
    private final BridgeFightPlugin plugin;

    public SpectatorListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session == null) return;

        if (!session.isSpectator(player.getUniqueId()) && session.getPlayerState(player.getUniqueId()) != BedFightPlayerState.ENDED) return;

        event.setCancelled(true);
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();
        if (name.contains("Teleport")) {
            me.molfordan.bridgefightplugin.gui.SpectatorTeleportGUI.open(player, session);
        } else if (name.contains("Leave")) {
            player.performCommand("leave");
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session != null && (session.isSpectator(player.getUniqueId()) || session.getPlayerState(player.getUniqueId()) == BedFightPlayerState.ENDED)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session != null && (session.isSpectator(player.getUniqueId()) || session.getPlayerState(player.getUniqueId()) == BedFightPlayerState.ENDED)) {
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session != null && (session.isSpectator(player.getUniqueId()) || session.getPlayerState(player.getUniqueId()) == BedFightPlayerState.ENDED)) {
            event.setFoodLevel(20);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Teleport to Player")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (!event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) return;
            
            String targetName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                player.teleport(target);
                player.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName());
                player.closeInventory();
            }
            return;
        }

        BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session != null && (session.getPlayerState(player.getUniqueId()) == BedFightPlayerState.ENDED || session.isSpectator(player.getUniqueId()))) {
            event.setCancelled(true);
        }
    }

    public static void giveSpectatorItems(Player player) {
        player.getInventory().clear();

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GREEN + "Teleport (Right Click)");
        compass.setItemMeta(compassMeta);

        ItemStack leaveItem = new ItemStack(Material.BED);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        leaveMeta.setDisplayName(ChatColor.RED + "Leave (Right Click)");
        leaveItem.setItemMeta(leaveMeta);

        player.getInventory().setItem(0, compass);
        player.getInventory().setItem(8, leaveItem);
    }
}
