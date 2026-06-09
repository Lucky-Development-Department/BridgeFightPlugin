package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PrivateWorldListener implements Listener {

    private final BridgeFightPlugin plugin;

    public PrivateWorldListener(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        String world = plugin.getConfigManager().getPrivateWorldWorldName();
        if (world == null) return;

        Player victim = (Player) event.getEntity();
        if (!victim.getWorld().getName().equals(world)) return;

        event.setDamage(0.0);
    }

}
