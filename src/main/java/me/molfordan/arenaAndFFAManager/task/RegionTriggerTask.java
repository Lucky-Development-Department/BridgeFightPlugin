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

                    // Only trigger once per enter
                    if (!inside.getOrDefault(p, false)) {

                        String cmd = r.getCommand();
                        CommandRegion.Executor exec = r.getExecutor();

                        // ======================================================
                        // IGNORE NULL EXECUTOR OR EMPTY COMMAND
                        // ======================================================
                        if (exec == CommandRegion.Executor.NULL ||
                                cmd == null ||
                                cmd.trim().isEmpty()) {

                            inside.put(p, true);
                            break;
                        }

                        // Handle %player%
                        String prepared = cmd.replace("%player%", p.getName());

                        // ======================================================
                        // CONNECT:server SUPPORT
                        // ======================================================
                        if (prepared.toLowerCase().startsWith("connect:")) {
                            String server = prepared.substring("connect:".length()).trim();
                            sendToServer(p, server);
                            inside.put(p, true);
                            break;
                        }

                        // ======================================================
                        // NORMAL COMMAND EXECUTION
                        // ======================================================
                        if (exec == CommandRegion.Executor.CONSOLE) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
                        } else {
                            if (prepared.startsWith("/")) {
                                prepared = prepared.substring(1);
                            }
                            p.performCommand(prepared);
                        }

                        inside.put(p, true);
                    }
                    break;
                }
            }

            // Reset enter state when outside all regions
            if (!inAny) inside.put(p, false);
        }
    }

    // ============================================================
    // SEND PLAYER TO VELOCITY/BUNGEE SERVER
    // ============================================================
    public void sendToServer(Player player, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendPluginMessage(ArenaAndFFAManager.getPlugin(), "nebula:main", b.toByteArray());
        player.sendPluginMessage(ArenaAndFFAManager.getPlugin(), "BungeeCord", b.toByteArray());
    }
}
