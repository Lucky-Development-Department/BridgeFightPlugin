package me.molfordan.arenaAndFFAManager.bedfight;

import net.faydev.spaceframe.SpaceframeKnockbackReloadEvent;
import net.faydev.spaceframe.SpaceframeWorld;
import net.faydev.spaceframe.config.KnockbackConfig;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class BedfightKnockback implements Listener {

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        if (world.getName().startsWith("bf_")) {
            applyKnockback(world);
        }
    }

    @EventHandler
    public void onKnockbackReload(SpaceframeKnockbackReloadEvent event){
        World world = event.getWorld();
        if (world.getName().startsWith("bf_")) {
            applyKnockback(world);
        }
    }

    private void applyKnockback(World world) {
        try {
            if (world instanceof SpaceframeWorld){
                SpaceframeWorld spaceframeWorld = (SpaceframeWorld) world;
                KnockbackConfig config = spaceframeWorld.getKnockbackConfig();
                if (config != null) {
                    config.consistent = true;
                    config.horizontal = 0.53;
                    config.extraHorizontal = 0.327125;
                    config.extraAirVertical = 0;
                    config.extraVertical = 0;
                    config.vertical = 0.361375;
                    config.airVertical = 0.361375;
                    config.straight = true;
                    config.wTap = true;
                    config.verticalLimit = 0.4;
                    config.splitHitDetection = 50;
                }
            }
        } catch (NoClassDefFoundError e) {
            // This happens if the Spaceframe API classes aren't available at runtime
            System.err.println("BedfightKnockback: Spaceframe API not fully available, skipping knockback application.");
        }
    }
}
