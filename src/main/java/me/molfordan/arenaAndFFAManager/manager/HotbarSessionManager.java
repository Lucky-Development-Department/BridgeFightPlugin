package me.molfordan.arenaAndFFAManager.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarSession;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import org.bukkit.entity.Player;

public class HotbarSessionManager {
    private final ArenaAndFFAManager plugin;

    private final HotbarDataManager dataManager;

    private final KitManager kitManager;

    private final Map<UUID, HotbarSession> sessions = new HashMap<>();

    public HotbarSessionManager(ArenaAndFFAManager plugin, HotbarDataManager dataManager, KitManager kitManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.kitManager = kitManager;
    }

    public void openSession(Player player) {
        Map<Integer, String> layout = this.dataManager.load(player.getUniqueId());
        if (layout == null)
            layout = new HashMap<>();
        HotbarSession s = new HotbarSession(this.plugin, player, this.dataManager, this.kitManager);
        this.sessions.put(player.getUniqueId(), s);
        player.openInventory(s.getInventory());
    }

    public void closeSession(Player player) {
        HotbarSession s = this.sessions.remove(player.getUniqueId());
        if (s != null)
            s.onClose();
    }

    public HotbarSession getSession(Player player) {
        return this.sessions.get(player.getUniqueId());
    }
}
