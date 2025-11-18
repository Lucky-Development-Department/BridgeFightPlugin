package me.molfordan.arenaAndFFAManager.listener;


import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.manager.CombatManager;
import me.molfordan.arenaAndFFAManager.utils.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class TeleportSnowballListener implements Listener {

    private final Map<String, Integer> snowballMap = new HashMap<>();

    private CombatManager combatManager;

    private CombatLogListener combatLogListener;

    public TeleportSnowballListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    // When a player throws a teleport snowball, increment their count
    @EventHandler
    public void onPlayerThrow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();

        if (hand == null || hand.getType() != Material.SNOW_BALL || !hand.hasItemMeta()) return;

        ItemMeta meta = hand.getItemMeta();
        if (!meta.hasDisplayName()) return;

        if (CustomItem.getTeleportSnowball().getItemMeta().getDisplayName().equals(meta.getDisplayName())) {
            String name = player.getName();
            snowballMap.put(name, snowballMap.getOrDefault(name, 0) + 1);
        }
    }

    // When the snowball hits a player, teleport if possible
    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Snowball snowball = (Snowball) event.getDamager();
        if (!(snowball.getShooter() instanceof Player)) return;

        Player shooter = (Player) snowball.getShooter();
        Player victim = (Player) event.getEntity();
        String name = shooter.getName();

        Arena arena = combatManager.getTaggedArena(shooter);

        if (arena == null) return;

        int count = snowballMap.getOrDefault(name, 0);
        if (count > 0) {
            // Teleport
            Location shooterLoc = shooter.getLocation().clone();
            Location victimLoc = victim.getLocation().clone();

            shooter.teleport(victimLoc);
            victim.teleport(shooterLoc);

            // Decrease counter
            if (count == 1) {
                snowballMap.remove(name);
            } else {
                snowballMap.put(name, count - 1);
            }
        }


        if (shooter.getGameMode() == GameMode.CREATIVE) return;

        combatManager.tag(shooter, victim, arena);


    }

    // Clean up on world change or quit
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        snowballMap.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snowballMap.remove(event.getPlayer().getName());
    }
}
