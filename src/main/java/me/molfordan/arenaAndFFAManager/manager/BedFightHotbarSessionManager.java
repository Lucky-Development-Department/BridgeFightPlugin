package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.BedFightHotbarSession;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedFightHotbarSessionManager {
    private final ArenaAndFFAManager plugin;
    private final BedFightHotbarDataManager dataManager;
    private final KitManager kitManager;
    private final Map<UUID, BedFightHotbarSession> sessions = new HashMap<>();

    public BedFightHotbarSessionManager(ArenaAndFFAManager plugin, BedFightHotbarDataManager dataManager, KitManager kitManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.kitManager = kitManager;
    }

    public void openSession(Player player) {
        BedFightHotbarSession s = new BedFightHotbarSession(this.plugin, player, this.dataManager, this.kitManager);
        this.sessions.put(player.getUniqueId(), s);
        player.openInventory(s.getInventory());
    }

    public void closeSession(Player player) {
        BedFightHotbarSession s = this.sessions.remove(player.getUniqueId());
        if (s != null)
            s.onClose();
    }

    public BedFightHotbarSession getSession(Player player) {
        return this.sessions.get(player.getUniqueId());
    }
}
