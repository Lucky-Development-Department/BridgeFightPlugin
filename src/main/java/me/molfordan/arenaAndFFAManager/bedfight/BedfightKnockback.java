package me.molfordan.arenaAndFFAManager.bedfight;

import com.hpfxd.pandaspigot.config.PandaSpigotWorldConfig;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
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
            bedfightKnockback(world);
            return;
        }
        String buildFFA = ArenaAndFFAManager.getInstance().getConfigManager().getBuildFFAWorldName();
        String bridgeFight = ArenaAndFFAManager.getInstance().getConfigManager().getBridgeFightWorldName();
        if (world.getName().equals(bridgeFight) || world.getName().equals(buildFFA)) {

            defaultKnockback(world);
        }
    }



    @EventHandler
    public void onKnockbackReload(SpaceframeKnockbackReloadEvent event){
        World world = event.getWorld();
        if (world.getName().startsWith("bf_")) {
            bedfightKnockback(world);
            return;
        }
        String buildFFA = ArenaAndFFAManager.getInstance().getConfigManager().getBuildFFAWorldName();
        String bridgeFight = ArenaAndFFAManager.getInstance().getConfigManager().getBridgeFightWorldName();
        if (world.getName().equals(bridgeFight) || world.getName().equals(buildFFA)) {

            defaultKnockback(world);
        }
    }

    private void defaultKnockback(World world){

        try{
            if (world instanceof SpaceframeWorld){
                SpaceframeWorld spaceframeWorld = (SpaceframeWorld) world;
                PandaSpigotWorldConfig config = spaceframeWorld.getPandaSpigotWorldConfig();
                if (config != null) {
                    config.knockback.consistent = false;
                    config.knockback.frictionHorizontal = 2.0;
                    config.knockback.frictionVertical = 2.0;
                    config.knockback.horizontal = 0.4;
                    config.knockback.extraHorizontal = 0.5;
                    config.knockback.extraAirVertical = 0.07;
                    config.knockback.extraVertical = 0.07;
                    config.knockback.vertical = 0.4;
                    config.knockback.airVertical = 0.43925;
                    config.knockback.straight = false;
                    config.knockback.wTap = true;
                    config.knockback.verticalLimit = 0.4;
                    config.knockback.splitHitDetection = 65;
                }
            }
        }catch (NoClassDefFoundError e){
            // This happens if the Spaceframe API classes aren't available at runtime
            System.err.println("BedfightKnockback: Spaceframe API not fully available, skipping knockback application.");
        }
    }

    private void bedfightKnockback(World world) {

        try {
            if (world instanceof SpaceframeWorld){
                SpaceframeWorld spaceframeWorld = (SpaceframeWorld) world;
                PandaSpigotWorldConfig config = spaceframeWorld.getPandaSpigotWorldConfig();
                if (config != null) {
                    config.knockback.consistent = true;
                    config.knockback.horizontal = 0.53;
                    config.knockback.extraHorizontal = 0.327125;
                    config.knockback.extraAirVertical = 0;
                    config.knockback.extraVertical = 0;
                    config.knockback.vertical = 0.361375;
                    config.knockback.airVertical = 0.361375;
                    config.knockback.straight = true;
                    config.knockback.wTap = true;
                    config.knockback.verticalLimit = 0.4;
                    config.knockback.splitHitDetection = 25;
                }
            }
        } catch (NoClassDefFoundError e) {
            // This happens if the Spaceframe API classes aren't available at runtime
            System.err.println("BedfightKnockback: Spaceframe API not fully available, skipping knockback application.");
        }
    }
}
