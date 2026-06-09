package me.molfordan.bridgefightplugin.bedfight;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class SlimeWorldAdapter {
    private final SlimePlugin swm;
    private final SlimeLoader fileLoader;

    public SlimeWorldAdapter() {
        this.swm = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        this.fileLoader = swm.getLoader("file");
    }

    public SlimeWorld loadTemplate(String worldName) throws IOException, WorldInUseException, UnknownWorldException, CorruptedWorldException, NewerFormatException {
        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setInt(SlimeProperties.SPAWN_X, 0);
        properties.setInt(SlimeProperties.SPAWN_Y, 100);
        properties.setInt(SlimeProperties.SPAWN_Z, 0);
        properties.setBoolean(SlimeProperties.ALLOW_ANIMALS, false);

        properties.setBoolean(SlimeProperties.ALLOW_MONSTERS, false);
        properties.setBoolean(SlimeProperties.PVP, true);
        properties.setString(SlimeProperties.DIFFICULTY, "normal");

        return swm.loadWorld(fileLoader, worldName, true, properties);
    }

    public World createMatchWorld(SlimeWorld template) {
        String matchName = "bf_" + UUID.randomUUID().toString().substring(0, 8);
        SlimeWorld matchWorld = template.clone(matchName);
        swm.generateWorld(matchWorld);
        World world = Bukkit.getWorld(matchName);
        if (world != null) {
            world.setGameRuleValue("doEntityDrops", "true");
            world.setGameRuleValue("doTileDrops", "true");
            world.setGameRuleValue("mobGriefing", "true");
            world.setGameRuleValue("keepInventory", "true");
        }

        return Bukkit.getWorld(matchName);
    }

    public void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
    }

    public void importWorld(File folder, String worldName) throws IOException, WorldAlreadyExistsException, InvalidWorldException, WorldInUseException, WorldLoadedException, WorldTooBigException {
        swm.importWorld(folder, worldName, fileLoader);
    }
}
