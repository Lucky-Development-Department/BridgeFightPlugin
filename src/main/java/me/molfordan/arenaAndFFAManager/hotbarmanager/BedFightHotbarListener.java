package me.molfordan.arenaAndFFAManager.hotbarmanager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.BedFightHotbarSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class BedFightHotbarListener implements Listener {

    private final ArenaAndFFAManager plugin;
    private final BedFightHotbarSessionManager sessionManager;

    public BedFightHotbarListener(ArenaAndFFAManager plugin, BedFightHotbarSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        BedFightHotbarSession session = sessionManager.getSession(p);
        if (session == null) return;

        Inventory top = e.getView().getTopInventory();
        if (!top.equals(session.getInventory())) return;

        e.setCancelled(true);
        session.handleClick(e.getRawSlot(), e.getCurrentItem(), e.getCursor(), e.isShiftClick());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();

        BedFightHotbarSession session = sessionManager.getSession(p);
        if (session == null) return;

        Inventory top = e.getView().getTopInventory();
        if (!top.equals(session.getInventory())) return;

        if (!session.isHotbarValid()) {
            p.sendMessage("§cYour hotbar layout is invalid! You cannot have duplicate unique items. Please fix it.");
            // Reopen GUI safely next tick
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                p.openInventory(session.getInventory());
            });
            return; // Do NOT close session!
        }

        // Final save before closing
        session.saveHotbarLayoutToDB();

        // Apply sorting if player is in a BedFight world
        if (p.getWorld().getName().startsWith("bf_")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                HotbarManager.resortInventory(p, plugin.getBedFightHotbarDataManager().load(p.getUniqueId()));
            });
        }

        sessionManager.closeSession(p);
    }
}
