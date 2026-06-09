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
        // This is tricky, I need to map the yaml to the Spaceframe config.
        // Actually, just reading the YAML should be enough.
        
        try{
            if (world instanceof SpaceframeWorld){
                SpaceframeWorld spaceframeWorld = (SpaceframeWorld) world;
                PandaSpigotWorldConfig config = spaceframeWorld.getPandaSpigotWorldConfig();
                if (config != null) {
                    org.bukkit.configuration.ConfigurationSection c = ArenaAndFFAManager.plugin.getKnockbackConfig().getConfig().getConfigurationSection("build-bridge");
                    config.knockback.consistent = c.getBoolean("consistent");
                    config.knockback.frictionHorizontal = c.getDouble("frictionHorizontal");
                    config.knockback.frictionVertical = c.getDouble("frictionVertical");
                    config.knockback.horizontal = c.getDouble("horizontal");
                    config.knockback.airVerticalResistance = c.getDouble("airVerticalResistance");
                    config.knockback.extraHorizontal = c.getDouble("extraHorizontal");
                    config.knockback.extraAirVertical = c.getDouble("extraAirVertical");
                    config.knockback.extraVertical = c.getDouble("extraVertical");
                    config.knockback.vertical = c.getDouble("vertical");
                    config.knockback.airVertical = c.getDouble("airVertical");
                    config.knockback.straight = c.getBoolean("straight");
                    config.knockback.wTap = c.getBoolean("wTap");
                    config.knockback.verticalLimit = c.getDouble("verticalLimit");
                    config.knockback.splitHitDetection = c.getInt("splitHitDetection");
                }
            }
        }catch (NoClassDefFoundError e){
            System.err.println("BedfightKnockback: Spaceframe API not fully available, skipping knockback application.");
        }
    }

    private void bedfightKnockback(World world) {
        try {
            if (world instanceof SpaceframeWorld){
                SpaceframeWorld spaceframeWorld = (SpaceframeWorld) world;
                PandaSpigotWorldConfig config = spaceframeWorld.getPandaSpigotWorldConfig();
                if (config != null) {
                    org.bukkit.configuration.ConfigurationSection c = ArenaAndFFAManager.plugin.getKnockbackConfig().getConfig().getConfigurationSection("bedfight");
                    config.knockback.consistent = c.getBoolean("consistent");
                    config.knockback.horizontal = c.getDouble("horizontal");
                    config.knockback.extraHorizontal = c.getDouble("extraHorizontal");
                    config.knockback.extraAirVertical = c.getDouble("extraAirVertical");
                    config.knockback.extraVertical = c.getDouble("extraVertical");
                    config.knockback.vertical = c.getDouble("vertical");
                    config.knockback.airVertical = c.getDouble("airVertical");
                    config.knockback.airVerticalResistance = c.getDouble("airVerticalResistance");
                    config.knockback.straight = c.getBoolean("straight");
                    config.knockback.wTap = c.getBoolean("wTap");
                    config.knockback.verticalLimit = c.getDouble("verticalLimit");
                    config.knockback.splitHitDetection = c.getInt("splitHitDetection");
                }
            }
        } catch (NoClassDefFoundError e) {
            System.err.println("BedfightKnockback: Spaceframe API not fully available, skipping knockback application.");
        }
    }
}
