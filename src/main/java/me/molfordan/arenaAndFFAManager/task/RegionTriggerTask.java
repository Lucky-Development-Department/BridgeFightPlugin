package me.molfordan.arenaAndFFAManager.task;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.region.CommandRegion;
import me.molfordan.arenaAndFFAManager.region.CommandRegionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

public class RegionTriggerTask extends BukkitRunnable {

    private final CommandRegionManager manager;
    private final Map<Player, Boolean> inside = new WeakHashMap<>();

    public RegionTriggerTask(CommandRegionManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inAny = false;

            for (CommandRegion r : manager.getRegions()) {
                if (r.isInside(p.getLocation())) {
                    inAny = true;

                    if (!inside.getOrDefault(p, false)) {

                        String raw = r.getCommand().replace("%player%", p.getName());
                        String cmd = raw;

                        // ================================
                        //  CONNECT: FLAG SUPPORT
                        // ================================
                        if (cmd.toLowerCase().startsWith("connect:")) {
                            String server = cmd.substring("connect:".length());
                            sendToServer(p, server.trim());
                            inside.put(p, true);
                            break;
                        }

                        // ================================
                        //  NORMAL COMMAND EXECUTION
                        // ================================
                        if (r.getExecutor() == CommandRegion.Executor.CONSOLE) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        } else {
                            if (cmd.startsWith("/")) cmd = cmd.substring(1);
                            p.performCommand(cmd);
                        }

                        inside.put(p, true);
                    }
                    break;
                }
            }

            if (!inAny) inside.put(p, false);
        }
    }

    // ================================================
    //  SEND PLAYER TO VELOCITY/BUNGEE SERVER
    // ================================================
    public void sendToServer(Player player, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect"); // Lowercase = Velocity + Bungee compatible
            out.writeUTF(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.sendPluginMessage(ArenaAndFFAManager.getPlugin(), "nebula:main", b.toByteArray());
        player.sendPluginMessage(ArenaAndFFAManager.getPlugin(), "BungeeCord", b.toByteArray());
    }
}
