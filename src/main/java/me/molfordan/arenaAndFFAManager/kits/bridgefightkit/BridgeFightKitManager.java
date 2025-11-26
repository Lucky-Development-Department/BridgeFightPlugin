package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.DefaultKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.DiamondArmorKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.GoldArmorKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits.GoldSwordKit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BridgeFightKitManager {
    private final Map<String, Kit> kits = new HashMap<>();

    public BridgeFightKitManager() {
        register(new DefaultKit());
        register(new DiamondArmorKit());
        register(new GoldSwordKit());
        register(new GoldArmorKit());
    }

    public void register(Kit kit) {
        kits.put(kit.getName().toLowerCase(), kit);
    }

    public Kit get(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getAllKits() {
        return kits.values();
    }
}
