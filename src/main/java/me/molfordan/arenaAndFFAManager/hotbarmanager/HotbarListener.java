package me.molfordan.arenaAndFFAManager.hotbarmanager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.HotbarSessionManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class HotbarListener implements Listener {

    private final ArenaAndFFAManager plugin;
    private final HotbarSessionManager sessionManager;
    private final KitManager kitManager;

    public HotbarListener(ArenaAndFFAManager plugin, HotbarSessionManager sessionManager, KitManager kitManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        HotbarSession session = sessionManager.getSession(p);
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

        HotbarSession session = sessionManager.getSession(p);
        if (session == null) return;

        Inventory top = e.getView().getTopInventory();
        if (!top.equals(session.getInventory())) return;

        // Validate hotbar
        if (!session.isHotbarValid()) {
            p.sendMessage("§cYour hotbar layout is invalid! You cannot have duplicate unique items.");

            // Reopen GUI safely next tick
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                p.openInventory(session.getInventory());
            });
            return;
        }

        // Valid → close session
        sessionManager.closeSession(p);
        session.onClose();
        String lobbyWorldName = plugin.getConfigManager().getLobbyWorldName();
        String bridgeFightWorldName = plugin.getConfigManager().getBridgeFightWorldName();

        // Apply kit AFTER GUI closes (your giveKillRewards will handle ensureItem)
        if (p.getWorld().getName().equals(lobbyWorldName)) return;
        if (p.getWorld().getName().equals(bridgeFightWorldName)) return;
        // Apply BuildFFA kit
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            HotbarManager.resortInventory(p, plugin.getHotbarDataManager());
        });
    }
}
