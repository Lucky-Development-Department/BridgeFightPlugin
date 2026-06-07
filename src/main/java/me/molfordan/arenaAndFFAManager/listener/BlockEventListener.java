package me.molfordan.arenaAndFFAManager.listener;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.molfordan.arenaAndFFAManager.*;
import me.molfordan.arenaAndFFAManager.hotbarmanager.BlockHotbarSorter;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.SerializableBlockState;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.ProtocolLibrary;

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

    private final Map<String, Long> cancelledPlacements = new ConcurrentHashMap<>();

    public BlockEventListener(ArenaManager manager,
                              ArenaAndFFAManager plugin,
                              LadderRestorer ladderRestorer,
                              PersistentRestoreManager persistentManager) {
        this.manager = manager;
        this.plugin = plugin;
        this.ladderRestorer = ladderRestorer;
        this.persistentManager = persistentManager;
        registerPacketListener();
        registerSoundListener();
    }

    private void registerSoundListener() {
        if (ProtocolLibrary.getProtocolManager() == null) return;
        
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, 
            PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event == null || event.getPacket() == null) return;
                
                // Defensive check for player to avoid ProtocolLib internal NPEs
                try {
                    if (event.getPlayer() == null) return;
                } catch (Exception e) {
                    return;
                }
                
                try {
                    if (event.getPacket().getStrings().size() == 0) return;
                    String soundName = event.getPacket().getStrings().read(0);
                    if (soundName == null) return;
                    
                    if (soundName.startsWith("dig.") || soundName.startsWith("step.")) {
                        if (event.getPacket().getIntegers().size() < 3) return;
                        
                        Integer ix = event.getPacket().getIntegers().read(0);
                        Integer iy = event.getPacket().getIntegers().read(1);
                        Integer iz = event.getPacket().getIntegers().read(2);
                        
                        if (ix == null || iy == null || iz == null) return;
                        
                        double x = ix / 8.0;
                        double y = iy / 8.0;
                        double z = iz / 8.0;
                        
                        String key = (int)x + "," + (int)y + "," + (int)z;
                        Long time = cancelledPlacements.get(key);
                        if (time != null && System.currentTimeMillis() - time < 200) {
                            event.setCancelled(true);
                        }
                    }
                } catch (Exception e) {
                    // Silently ignore - common during disconnects
                }
            }
        });
    }

    public void registerPacketListener() {
        if (ProtocolLibrary.getProtocolManager() == null) return;
        
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
            plugin, PacketType.Play.Client.BLOCK_PLACE) {
            
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event == null || event.getPacket() == null) return;
                
                // Defensive check for player to avoid ProtocolLib internal NPEs
                try {
                    if (event.getPlayer() == null) return;
                } catch (Exception e) {
                    return;
                }
                
                try {
                    Player player = event.getPlayer();
                    if (manager.isBypassing(player.getUniqueId())) return;
                    
                    // Get block location from packet
                    if (event.getPacket().getBlockPositionModifier().size() == 0) return;
                    BlockPosition pos = event.getPacket().getBlockPositionModifier().read(0);
                    if (pos == null || pos.getY() < 0) return; 

                    // In 1.8, Integers: 0 = direction
                    if (event.getPacket().getIntegers().size() == 0) return;
                    int direction = event.getPacket().getIntegers().read(0);
                    if (direction == 255) return; // Interaction, not placement

                    // Check if holding a custom item or non-block
                    ItemStack itemInHand = player.getItemInHand();
                    if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                        // If it's not a block material OR it has a custom name/lore, OR it's TNT,
                        // we treat it as a potential interaction and let it pass to the server.
                        // The actual placement (if it is a block) will still be blocked by onBlockPlace.
                        if (!itemInHand.getType().isBlock() || 
                            (itemInHand.hasItemMeta() && (itemInHand.getItemMeta().hasDisplayName() || itemInHand.getItemMeta().hasLore())) ||
                            itemInHand.getType() == Material.TNT) {
                            return;
                        }
                    }

                    Location clickedLoc = new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
                    Location targetLoc = clickedLoc.clone();
                    switch (direction) {
                        case 0: targetLoc.add(0, -1, 0); break;
                        case 1: targetLoc.add(0, 1, 0); break;
                        case 2: targetLoc.add(0, 0, -1); break;
                        case 3: targetLoc.add(0, 0, 1); break;
                        case 4: targetLoc.add(-1, 0, 0); break;
                        case 5: targetLoc.add(1, 0, 0); break;
                    }

                    Arena arena = manager.getArenaByLocation(targetLoc);
                    
                    boolean cancel = false;
                    if (arena == null) {
                        Arena shellArena = findShellArena(targetLoc, 2);
                        if (shellArena != null) {
                            cancel = true;
                        }
                    } else if (arena.getType() == ArenaType.DUEL
                            || arena.getType() == ArenaType.TOPFIGHT
                            || arena.getType() == ArenaType.FFABUILD) {
                        if (targetLoc.getBlockY() > arena.getBuildLimitY()) {
                            cancel = true;
                        }
                    }

                    if (cancel) {
                        event.setCancelled(true);
                        String key = targetLoc.getBlockX() + "," + targetLoc.getBlockY() + "," + targetLoc.getBlockZ();
                        cancelledPlacements.put(key, System.currentTimeMillis());
                        
                        // Force a sync to clear ghost block - Double Tap
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                Block b = targetLoc.getBlock();
                                player.sendBlockChange(targetLoc, b.getType(), b.getData());
                                player.updateInventory();
                            }
                        }, 1L);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                Block b = targetLoc.getBlock();
                                player.sendBlockChange(targetLoc, b.getType(), b.getData());
                            }
                        }, 3L);
                    }
                } catch (Exception e) {
                    // Silently ignore
                }
            }
        });
    }



    /* ==========================================================
       BLOCK PLACE EVENT
    ========================================================== */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        String worldName = event.getPlayer().getWorld().getName();
        if (!worldName.startsWith("bf_")) {
            return;
        }

        Player player = event.getPlayer();
        if (manager.isBypassing(player.getUniqueId())) return;

        Block block = event.getBlock();
        block.setMetadata("player_blocks", new FixedMetadataValue(plugin, true));
        Location loc = block.getLocation();
        BlockState replacedState = event.getBlockReplacedState();

        // First: if inside an arena (original cuboid), proceed as before
        Arena arena = manager.getArenaByLocation(loc);

        // If not inside arena, check if inside the 2-block shell (expanded cuboid minus original cuboid)
        if (arena == null) {
            Arena shellArena = findShellArena(loc, 2);
            if (shellArena != null) {
                // Inside the 2-block invisible border -> cancel placement
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place blocks in the arena border area.");
                cancelledPlacements.put(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(), System.currentTimeMillis());
                syncPlacement(player, loc, replacedState.getType(), replacedState.getRawData());
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
                if (event.getBlock().getType() == Material.TNT){
                    return;
                }
                event.setCancelled(true);
                cancelledPlacements.put(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(), System.currentTimeMillis());
                syncPlacement(player, loc, replacedState.getType(), replacedState.getRawData());
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
                        refundBlock(placer, arena, placedState.getType(), placedState.getRawData());
                    }
                }
                persistentManager.removeRestore(arena.getName() + "|" + coordsKey);
            });
        }
    }

    private void refundBlock(Player player, Arena arena, Material type, byte data) {
        if (player == null || !player.isOnline()
                || player.getGameMode() != GameMode.SURVIVAL
                || !arena.isInside(player.getLocation(), true)) {
            return;
        }

        short finalData = data;
        if (type == Material.LADDER) {
            finalData = 0;
        }

        ItemStack refund = new ItemStack(type, 1, finalData);
        BlockHotbarSorter blockSorter = new BlockHotbarSorter(plugin.getHotbarDataManager());

        // Sort first, returns TRUE if handled
        boolean handled = blockSorter.sort(player, refund);

        if (!handled) {
            ItemStack leftover = addToMainInventory(player, refund);
            if (leftover != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    private ItemStack addToMainInventory(Player player, ItemStack stack) {

        // Try stacking into main inventory (slots 9–35)
        for (int i = 9; i <= 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;

            if (item.isSimilar(stack)) {
                int max = item.getMaxStackSize();
                int free = max - item.getAmount();

                if (free > 0) {
                    int move = Math.min(free, stack.getAmount());
                    item.setAmount(item.getAmount() + move);
                    stack.setAmount(stack.getAmount() - move);

                    if (stack.getAmount() <= 0) return null;
                }
            }
        }

        // Try empty slots (9–35)
        for (int i = 9; i <= 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) {
                player.getInventory().setItem(i, stack);
                return null; // Stored fully
            }
        }

        return stack; // Nothing stored → leftover
    }

    /* ==========================================================
       BLOCK BREAK EVENT (Fixed version for 1.8)
    ========================================================== */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        String worldName = event.getPlayer().getWorld().getName();
        if (!worldName.startsWith("bf_")) {
            return;
        }

        if (event.getBlock().hasMetadata("egg_bridge_block")) {
            return;
        }
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
        BlockState brokenState = block.getState();
        String coordsKey = getCoordsKey(loc);
        String mapKey = makeArenaKey(arena, loc);

        plugin.debug("Block broken by " + player.getName()
                + " in arena=" + arena.getName() + " at " + coordsKey);

        // Handle refund IF it was a player-placed block
        UUID placerId = ffabuildPlacers.remove(mapKey);
        if (placerId != null) {
            Player placer = Bukkit.getPlayer(placerId);
            refundBlock(placer, arena, brokenState.getType(), brokenState.getRawData());
            
            // Cancel the automatic removal task since we just broke it
            stopBlockBreakingAnimation(mapKey);
            BukkitRunnable existingTask = scheduledRestores.remove(mapKey);
            if (existingTask != null) {
                existingTask.cancel();
            }
        }

        // Break the block manually (since event.setDropItems(false) doesn't exist in 1.8)
        block.setType(Material.AIR);

        // Prevent item drops (remove entities next tick)
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeItemsAt(loc), 1L);

        // Retrieve saved original state from arena data
        SerializableBlockState saved = arena.getOriginalBlocksMap().get(coordsKey);
        
        // If it was a player-placed block (not in originalBlocks), it should restore to AIR
        Material restoreType = saved != null ? saved.getType() : Material.AIR;
        byte restoreData = saved != null ? saved.getData() : (byte) 0;

        // If it's already AIR and no original block to restore, we don't need to schedule anything
        if (restoreType == Material.AIR && block.getType() == Material.AIR) {
             persistentManager.removeRestore(arena.getName() + "|" + coordsKey);
             return;
        }

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

        // Start block breaking animation
        startBlockBreakingAnimation(loc, mapKey);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } finally {
                    scheduledRestores.remove(mapKey);
                    // Stop the breaking animation
                    stopBlockBreakingAnimation(mapKey);
                }
            }
        };
        scheduledRestores.put(mapKey, task);
        task.runTaskLater(plugin, 20L * 30);
    }

    private void removeItemsAt(Location loc) {
        if (loc.getWorld().getName().startsWith("bf_")) {
            plugin.getLogger().info("Debug ItemRemoval: Skipping removal in " + loc.getWorld().getName());
            return;
        }
        for (Item item : loc.getWorld().getEntitiesByClass(Item.class)) {
            Location itemLoc = item.getLocation();
            if (itemLoc.getBlockX() == loc.getBlockX()
                    && itemLoc.getBlockY() == loc.getBlockY()
                    && itemLoc.getBlockZ() == loc.getBlockZ()) {
                plugin.getLogger().info("Debug ItemRemoval: Removing item " + item.getItemStack().getType() + " at " + itemLoc);
                item.remove();
            }
        }
    }

    private final Map<String, BukkitRunnable> breakingAnimations = new ConcurrentHashMap<>();

    private void startBlockBreakingAnimation(Location loc, String mapKey) {
        final long startTime = System.currentTimeMillis();
        final long duration = 30000L; // 30 seconds
        
        // Generate a unique-ish ID for this location to avoid flickering and collisions.
        // We use a large negative offset because real entity IDs are positive.
        final int animationId = -1000000 - (((loc.getBlockX() & 0xFFF) << 20) | ((loc.getBlockZ() & 0xFFF) << 8) | (loc.getBlockY() & 0xFF));

        BukkitRunnable animationTask = new BukkitRunnable() {
            private int lastStage = -1;
            private int ticksSinceLastSend = 0;

            @Override
            public void run() {
                if (loc == null || loc.getWorld() == null) {
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                int stage = (int) ((elapsed * 10) / duration);
                if (stage > 9) stage = 9;

                // Optimization: Only send if stage changed OR every 2 seconds to catch new players
                ticksSinceLastSend += 10;
                if (stage == lastStage && ticksSinceLastSend < 40) {
                    return;
                }
                
                lastStage = stage;
                ticksSinceLastSend = 0;

                double radiusSq = 20 * 20;
                for (Player player : loc.getWorld().getPlayers()) {
                    if (player.isOnline() && player.getLocation().distanceSquared(loc) <= radiusSq) {
                        sendBlockBreakAnimation(player, loc, stage, animationId);
                    }
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                // Clear the animation for nearby players when the task is stopped
                if (loc != null && loc.getWorld() != null) {
                    for (Player player : loc.getWorld().getPlayers()) {
                        if (player.isOnline() && player.getLocation().distanceSquared(loc) <= 400) {
                            sendBlockBreakAnimation(player, loc, 10, animationId); // Stage 10 clears it
                        }
                    }
                }
            }
        };
        
        breakingAnimations.put(mapKey, animationTask);
        animationTask.runTaskTimer(plugin, 5L, 10L); // Update every 10 ticks (0.5s)
    }

    private void stopBlockBreakingAnimation(String mapKey) {
        BukkitRunnable animationTask = breakingAnimations.remove(mapKey);
        if (animationTask != null) {
            animationTask.cancel();
        }
    }

    private void sendBlockBreakAnimation(Player player, Location loc, int stage, int animationId) {
        if (player == null || loc == null || !player.isOnline()) {
            return;
        }
        
        try {
            org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer cp = (org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player;
            net.minecraft.server.v1_8_R3.EntityPlayer ep = cp.getHandle();
            
            if (ep != null && ep.playerConnection != null) {
                net.minecraft.server.v1_8_R3.BlockPosition nmsPos = 
                    new net.minecraft.server.v1_8_R3.BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                
                net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation packet = 
                    new net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation(animationId, nmsPos, stage);
                
                ep.playerConnection.sendPacket(packet);
            }
        } catch (Throwable ignored) {
            // Fail silently
        }
    }

    /* ==========================================================
       ITEM SPAWN / DROP PREVENTION
    ========================================================== */
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (stack == null) return;
        System.out.println("Item spawn detected: " + stack.getType());

        // Allow items to spawn in BedFight worlds (bf_)
        if (event.getLocation().getWorld().getName().startsWith("bf_")) {
            System.out.println("Allowing item spawn in BedFight world");
            return;
        }
        
        Location loc = event.getLocation();
        Arena arena = manager.getArenaByLocation(loc);
        System.out.println("Item spawn detected in arena: " + arena);
        if (arena == null || arena.getType() != ArenaType.FFABUILD) return;
        System.out.println("Item spawn detected in FFABUILD arena");

        // In FFABUILD, we generally don't want items dropping from blocks.
        // If it was a player-placed block, refund it to the placer.
        String mapKey = makeArenaKey(arena, loc);
        UUID placerId = ffabuildPlacers.remove(mapKey);
        if (placerId != null) {
            Player placer = Bukkit.getPlayer(placerId);
            refundBlock(placer, arena, stack.getType(), (byte) stack.getDurability());

            // Cancel any scheduled auto-removal for this block
            stopBlockBreakingAnimation(mapKey);
            BukkitRunnable task = scheduledRestores.remove(mapKey);
            if (task != null) {
                task.cancel();
                System.out.println("Cancelled scheduled auto-removal for block");
            }
        }

        // Always cancel item spawns in FFABUILD to keep the arena clean
        event.setCancelled(true);
        System.out.println("Item spawn cancelled in FFABUILD arena");
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

    private void syncPlacement(Player player, Location loc, Material mat, byte data) {
        player.sendBlockChange(loc, mat, data);
        
        // Double tap sync with a tiny delay to ensure client prediction is cleared
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendBlockChange(loc, mat, data);
                player.updateInventory();
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        // Prevent these blocks from breaking due to adjacent block changes (floating blocks)
        if (type == Material.CACTUS || type == Material.SUGAR_CANE_BLOCK || 
            type == Material.TRAP_DOOR || type == Material.IRON_TRAPDOOR) {
            event.setCancelled(true);
        }
    }
}
