package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.*;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.restore.PendingRestore;
import me.molfordan.arenaAndFFAManager.restore.PersistentRestoreManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockEventListener implements Listener {

    private final ArenaManager manager;
    private final ArenaAndFFAManager plugin;
    private final LadderRestorer ladderRestorer;
    private final PersistentRestoreManager persistentManager;

    private final Map<String, BukkitRunnable> scheduledRestores = new ConcurrentHashMap<>();
    private final Map<String, UUID> ffabuildPlacers = new ConcurrentHashMap<>();
    private final Map<String, Integer> ladderRestoreAttempts = new ConcurrentHashMap<>();

    public BlockEventListener(ArenaManager manager,
                              ArenaAndFFAManager plugin,
                              LadderRestorer ladderRestorer,
                              PersistentRestoreManager persistentManager) {
        this.manager = manager;
        this.plugin = plugin;
        this.ladderRestorer = ladderRestorer;
        this.persistentManager = persistentManager;
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (manager.isBypassing(player.getUniqueId())) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        Arena arena = manager.getArenaByLocation(loc);
        if (arena == null) return;

        if (arena.getType() == ArenaType.FFABUILD || arena.getType() == ArenaType.FFA) {
            Material material = block.getType();
            if (material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.ENDER_CHEST) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot interact with chests in this arena.");

            }
        }
    }

    /* ==========================================================
       BLOCK PLACE EVENT
    ========================================================== */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (manager.isBypassing(player.getUniqueId())) return;

        Block block = event.getBlock();
        Location loc = block.getLocation();

        // First: if inside an arena (original cuboid), proceed as before
        Arena arena = manager.getArenaByLocation(loc);

        // If not inside arena, check if inside the 2-block shell (expanded cuboid minus original cuboid)
        if (arena == null) {
            Arena shellArena = findShellArena(loc, 2);
            if (shellArena != null) {
                // Inside the 2-block invisible border -> cancel placement
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place blocks in the arena border area.");
                return;
            }
            // Not inside any arena or shell -> not our concern
            return;
        }

        // From here, 'arena' is guaranteed to be the arena whose original cuboid contains the loc.
        // === Build limit check ===
        if (arena.getType() == ArenaType.DUEL
                || arena.getType() == ArenaType.TOPFIGHT
                || arena.getType() == ArenaType.FFABUILD) {
            if (loc.getBlockY() > arena.getBuildLimitY()) {
                event.setCancelled(true);
                return;
            }
        }

        // === The rest of your existing logic (unchanged) ===
        if (arena.getType() == ArenaType.FFABUILD || arena.getType() == ArenaType.TOPFIGHT) {
            String coordsKey = getCoordsKey(loc);
            String mapKey = makeArenaKey(arena, loc);
            ffabuildPlacers.put(mapKey, player.getUniqueId());
            BlockState placedState = block.getState();

            plugin.debug("Block placed by " + player.getName()
                    + " in arena=" + arena.getName() + " at " + coordsKey);

            PendingRestore pr = new PendingRestore(arena.getName(), loc.getWorld().getName(),
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    Material.AIR, (byte) 0, 30);
            persistentManager.addRestore(pr);

            scheduleRestore(mapKey, loc, () -> {
                Block current = loc.getBlock();
                if (current.getType() == placedState.getType()) {
                    current.setType(Material.AIR);
                    plugin.debug("Restored placed block in " + arena.getName() + " at " + coordsKey);
                    UUID placerId = ffabuildPlacers.remove(mapKey);
                    if (placerId != null) {
                        Player placer = Bukkit.getPlayer(placerId);
                        if (placer != null && placer.isOnline()
                                && placer.getGameMode() == GameMode.SURVIVAL
                                && arena.isInside(placer.getLocation(), true)) {
                            ItemStack refund = new ItemStack(placedState.getType(), 1, placedState.getRawData());
                            placer.getInventory().addItem(refund).values()
                                    .forEach(overflow -> placer.getWorld().dropItemNaturally(placer.getLocation(), overflow));
                        }
                    }
                }
                persistentManager.removeRestore(arena.getName() + "|" + coordsKey);
            });
        }
    }

    /* ==========================================================
       BLOCK BREAK EVENT (Fixed version for 1.8)
    ========================================================== */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (manager.isBypassing(player.getUniqueId())) return;

        Block block = event.getBlock();
        Location loc = block.getLocation();
        Arena arena = manager.getArenaByLocation(loc);
        if (arena == null || arena.getType() != ArenaType.FFABUILD) return;

        // Build limit
        if (loc.getBlockY() > arena.getBuildLimitY()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't break blocks above the build limit.");
            return;
        }

        // Disallowed types
        if (block.getType() == Material.PISTON_BASE
                || block.getType() == Material.PISTON_EXTENSION
                || block.getType() == Material.IRON_PLATE) {
            event.setCancelled(true);
            return;
        }

        // Save block state BEFORE breaking
        BlockState originalState = block.getState();
        String coordsKey = getCoordsKey(loc);
        String mapKey = makeArenaKey(arena, loc);

        plugin.debug("Block broken by " + player.getName()
                + " in arena=" + arena.getName() + " at " + coordsKey);

        // Break the block manually (since event.setDropItems(false) doesn't exist)
        block.setType(Material.AIR);
        block.getWorld().playEffect(loc, Effect.STEP_SOUND, originalState.getType());

        // Prevent item drops (remove entities next tick)
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeItemsAt(loc), 1L);

        // Retrieve saved original state from arena data
        SerializableBlockState saved = arena.getOriginalBlocksMap().get(coordsKey);
        Material restoreType = saved != null ? saved.getType() : originalState.getType();
        byte restoreData = saved != null ? saved.getData() : originalState.getRawData();

        // Schedule restore after 30s
        PendingRestore pr = new PendingRestore(
                arena.getName(),
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                restoreType,
                restoreData,
                30
        );
        persistentManager.addRestore(pr);

        // Only one restore per coordinate key
        if (scheduledRestores.containsKey(mapKey)) return;

        BukkitRunnable restoreTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Block target = loc.getBlock();
                    target.setType(restoreType);
                    target.setData(restoreData);
                    persistentManager.removeRestore(arena.getName() + "|" + coordsKey);
                    plugin.debug("Restored block in " + arena.getName() + " at " + coordsKey);
                } finally {
                    scheduledRestores.remove(mapKey);
                }
            }
        };
        scheduledRestores.put(mapKey, restoreTask);
        restoreTask.runTaskLater(plugin, 20L * 30); // 30 seconds delay

        // Ladder check after breaking
        checkNearbyLadders(arena, loc);
    }

    /* ==========================================================
       LADDER HANDLING
    ========================================================== */
    private void checkNearbyLadders(Arena arena, Location brokenLoc) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = brokenLoc.getBlock().getRelative(face);
            String coordsKey = getCoordsKey(adjacent.getLocation());
            SerializableBlockState s = arena.getOriginalBlocksMap().get(coordsKey);
            if (s != null && s.getType() == Material.LADDER) {
                BlockFace attachedFace = ladderRestorer.getAttachedFace(s.getData());
                Block support = adjacent.getRelative(attachedFace.getOppositeFace());
                scheduleLadderRestore(adjacent.getLocation(), s.getData(), support.getLocation(), arena);
            }
        }
    }

    private void scheduleLadderRestore(Location ladderLoc, byte ladderData, Location supportLoc, Arena arena) {
        String coordsKey = getCoordsKey(ladderLoc);
        String mapKey = makeArenaKey(arena, ladderLoc);
        if (scheduledRestores.containsKey(mapKey)) return;

        PendingRestore pr = new PendingRestore(arena.getName(), ladderLoc.getWorld().getName(),
                ladderLoc.getBlockX(), ladderLoc.getBlockY(), ladderLoc.getBlockZ(),
                Material.LADDER, ladderData, 30);
        persistentManager.addRestore(pr);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                tryLadderRestore(ladderLoc, ladderData, supportLoc, mapKey, arena.getName());
            }
        };
        scheduledRestores.put(mapKey, task);
        task.runTaskLater(plugin, 20L * 30);
        plugin.debug("Scheduled ladder restore at " + coordsKey);
    }

    private void tryLadderRestore(Location ladderLoc, byte ladderData, Location supportLoc,
                                  String mapKey, String arenaName) {
        if (supportLoc.getBlock().getType().isSolid()) {
            Block block = ladderLoc.getBlock();
            block.setType(Material.LADDER);
            block.setData(ladderData);
            ladderRestorer.trackLadder(ladderLoc, ladderData);
            scheduledRestores.remove(mapKey);
            ladderRestoreAttempts.remove(mapKey);
            persistentManager.removeRestore(mapKey);
        } else {
            int attempts = ladderRestoreAttempts.getOrDefault(mapKey, 0);
            if (attempts >= 30) {
                scheduledRestores.remove(mapKey);
                ladderRestoreAttempts.remove(mapKey);
                persistentManager.removeRestore(mapKey);
                return;
            }
            ladderRestoreAttempts.put(mapKey, attempts + 1);
            new BukkitRunnable() {
                @Override
                public void run() {
                    tryLadderRestore(ladderLoc, ladderData, supportLoc, mapKey, arenaName);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    /* ==========================================================
       RESTORE SCHEDULING / ITEM REMOVAL
    ========================================================== */
    private void scheduleRestore(String mapKey, Location loc, Runnable action) {
        if (scheduledRestores.containsKey(mapKey)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } finally {
                    scheduledRestores.remove(mapKey);
                }
            }
        };
        scheduledRestores.put(mapKey, task);
        task.runTaskLater(plugin, 20L * 30);
    }

    private void removeItemsAt(Location loc) {
        for (Item item : loc.getWorld().getEntitiesByClass(Item.class)) {
            Location itemLoc = item.getLocation();
            if (itemLoc.getBlockX() == loc.getBlockX()
                    && itemLoc.getBlockY() == loc.getBlockY()
                    && itemLoc.getBlockZ() == loc.getBlockZ()) {
                item.remove();
            }
        }
    }

    /* ==========================================================
       LADDER DROP PREVENTION
    ========================================================== */
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.LADDER) {
            Location loc = event.getLocation();
            Arena arena = manager.getArenaByLocation(loc);
            if (arena != null && arena.getType() == ArenaType.FFABUILD) {
                event.setCancelled(true);
            }
        }
    }

    /* ==========================================================
       KEY HELPERS
    ========================================================== */
    private String getCoordsKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private String makeArenaKey(Arena arena, Location loc) {
        return arena.getName() + "|" + getCoordsKey(loc);
    }

    private Arena findShellArena(Location loc, int buffer) {
        if (loc == null) return null;
        for (Arena arena : manager.getAllArenas()) {
            if (arena.getPos1() == null || arena.getPos2() == null) continue;
            if (!arena.getWorldName().equals(loc.getWorld().getName())) continue;

            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            int minX = Math.min(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int maxX = Math.max(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int minY = Math.min(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
            int maxY = Math.max(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
            int minZ = Math.min(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());
            int maxZ = Math.max(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());

            // expanded bounds
            int eMinX = minX - buffer;
            int eMaxX = maxX + buffer;
            int eMinY = minY - buffer;
            int eMaxY = maxY + buffer;
            int eMinZ = minZ - buffer;
            int eMaxZ = maxZ + buffer;

            boolean insideExpanded = (x >= eMinX && x <= eMaxX && y >= eMinY && y <= eMaxY && z >= eMinZ && z <= eMaxZ);
            boolean insideOriginal = (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ);

            // If inside expanded but NOT inside original -> it's in the shell
            if (insideExpanded && !insideOriginal) {
                return arena;
            }
        }
        return null;
    }
}
