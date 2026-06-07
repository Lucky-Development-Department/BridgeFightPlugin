package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.molfordan.arenaAndFFAManager.ArenaAndFFAManager.plugin;

public class BedFightListener implements Listener {
    private final ArenaAndFFAManager plugin;
    private final BedFightManager bedFightManager;
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, Long> hitTimestamp = new ConcurrentHashMap<>();
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public BedFightListener(ArenaAndFFAManager plugin, BedFightManager bedFightManager) {
        this.plugin = plugin;
        this.bedFightManager = bedFightManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BED) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName() || !item.getItemMeta().getDisplayName().contains("Leave")) return;

        event.setCancelled(true);
        player.performCommand("leave");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(org.bukkit.event.player.PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.DIED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.PREPARE || session.getPlayerState(player.getUniqueId()) == BedFightState.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.PREPARE || session.getPlayerState(player.getUniqueId()) == BedFightState.SPECTATOR) {
            if (session.getPlayerState(player.getUniqueId()) == BedFightState.PREPARE) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            event.setCancelled(true);
            return;
        }

        if (event.getBlock().getY() >= session.getArena().getBuildLimitY()) {
            player.sendMessage(ChatColor.RED + "You've reached the build height limit!");
            event.setCancelled(true);
            return;
        }

        Location loc = event.getBlock().getLocation();
        Arena arena = session.getArena();
        
        if (!isWithinBoundary(loc, arena, 0)) {
            if (isWithinBoundary(loc, arena, 2)) {
                player.sendMessage(ChatColor.RED + "You cannot place blocks in the arena border area!");
                event.setCancelled(true);
                return;
            }
            player.sendMessage(ChatColor.RED + "Can't place blocks outside the map!");
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        block.setMetadata("bedfight_placed", new FixedMetadataValue(plugin, true));
        session.getPlacedBlocks().add(block.getLocation());
    }

    private boolean isWithinBoundary(Location loc, Arena arena, int buffer) {
        Location p1 = arena.getPos1();
        Location p2 = arena.getPos2();
        if (p1 == null || p2 == null) return false;

        int minX = Math.min(p1.getBlockX(), p2.getBlockX()) - buffer;
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + buffer;
        int minY = Math.min(p1.getBlockY(), p2.getBlockY()) - buffer;
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + buffer;
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()) - buffer;
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + buffer;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (type == Material.BED_BLOCK) {
            handleBedBreak(event, session, player);
            return;
        }

        if (isProtectedBedBlock(block, session)) {
            event.setCancelled(false);
            return;
        }

        if (block.hasMetadata("bedfight_placed")) {
            event.setCancelled(false);
            block.removeMetadata("bedfight_placed", plugin);
            session.getPlacedBlocks().remove(block.getLocation());
        } else {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only break player-placed blocks!");
        }
    }

    private boolean isProtectedBedBlock(Block block, BedFightSession session) {
        Material type = block.getType();
        if (type != Material.WOOD && type != Material.ENDER_STONE) return false;

        Location bLoc = block.getLocation();
        Location redBed = session.getRedBedLoc();
        Location blueBed = session.getBlueBedLoc();

        return isNearBed(bLoc, redBed) || isNearBed(bLoc, blueBed);
    }

    private boolean isNearBed(Location bLoc, Location bedLoc) {
        if (bedLoc == null || !bLoc.getWorld().equals(bedLoc.getWorld())) return false;

        Block bedBlock = bedLoc.getBlock();
        if (bedBlock.getType() != Material.BED_BLOCK) return isWithinBedProtection(bLoc, bedLoc);

        org.bukkit.material.Bed bed = (org.bukkit.material.Bed) bedBlock.getState().getData();
        Location otherHalf = bedBlock.getRelative(bed.isHeadOfBed() ? bed.getFacing().getOppositeFace() : bed.getFacing()).getLocation();

        return isWithinBedProtection(bLoc, bedLoc) || isWithinBedProtection(bLoc, otherHalf);
    }

    private boolean isWithinBedProtection(Location bLoc, Location bedLoc) {
        if (bedLoc == null || !bLoc.getWorld().equals(bedLoc.getWorld())) return false;

        int dx = Math.abs(bLoc.getBlockX() - bedLoc.getBlockX());
        int dy = bLoc.getBlockY() - bedLoc.getBlockY();
        int dz = Math.abs(bLoc.getBlockZ() - bedLoc.getBlockZ());

        return dx <= 2 && dz <= 2 && dy >= 0 && dy <= 2;
    }

    private void handleBedBreak(BlockBreakEvent event, BedFightSession session, Player player) {
        Block block = event.getBlock();
        String team = session.getTeam(player.getUniqueId());

        if (team.equals("RED")) {
            if (isBedBlockOf(block, session.getBlueBedLoc())) {
                session.setBlueBedAlive(false);
                broadcastBedBreak(session, "Blue", player);
                event.setCancelled(false);
            } else if (isBedBlockOf(block, session.getRedBedLoc())) {
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
        } else if (team.equals("BLUE")) {
            if (isBedBlockOf(block, session.getRedBedLoc())) {
                session.setRedBedAlive(false);
                broadcastBedBreak(session, "Red", player);
                event.setCancelled(false);
            } else if (isBedBlockOf(block, session.getBlueBedLoc())) {
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
        }
    }

    private boolean isBedBlockOf(Block block, Location registeredLoc) {
        if (block == null || registeredLoc == null || !block.getWorld().equals(registeredLoc.getWorld())) return false;
        if (isSameLocation(block.getLocation(), registeredLoc)) return true;

        org.bukkit.material.Bed bed = (org.bukkit.material.Bed) block.getState().getData();
        Location otherHalf;
        if (bed.isHeadOfBed()) {
            otherHalf = block.getRelative(bed.getFacing().getOppositeFace()).getLocation();
        } else {
            otherHalf = block.getRelative(bed.getFacing()).getLocation();
        }

        return isSameLocation(otherHalf, registeredLoc);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        BedFightSession session = bedFightManager.getSession(victim);
        if (session == null) return;

        // Save death location
        deathLocations.put(victim.getUniqueId(), victim.getLocation());
        plugin.getLogger().info("DEBUG: Saved death location for " + victim.getName() + " at " + victim.getLocation());

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        if (session.getPlayerState(victim.getUniqueId()) == BedFightState.PREPARE) {
            return;
        }

        victim.spigot().respawn();
        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);
        victim.setHealth(20.0);
        victim.setFoodLevel(20);

        String team = session.getTeam(victim.getUniqueId());
        boolean bedAlive = (team.equals("RED")) ? session.isRedBedAlive() : session.isBlueBedAlive();

        if (bedAlive) {
            session.setPlayerState(victim.getUniqueId(), BedFightState.DIED);

            ChatColor victimColor = team.equalsIgnoreCase("RED") ? ChatColor.RED : ChatColor.BLUE;
            ChatColor killerColor = ChatColor.WHITE;
            if (killer != null) {
                String killerTeam = session.getTeam(killer.getUniqueId());
                killerColor = (killerTeam != null && killerTeam.equalsIgnoreCase("RED")) ? ChatColor.RED : ChatColor.BLUE;
                session.getStats(killer.getUniqueId()).kills++;
                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 100f, 1f);
            }

            String msg = (killer != null) ?
                    String.format(BedFightMessages.REGULAR_KILL, victimColor + victim.getName(), killerColor + killer.getName()) :
                    String.format(BedFightMessages.REGULAR_DEATH, victimColor + victim.getName());
            broadcastMessage(session, ChatColor.YELLOW + msg);

            handleRespawnSequence(victim, session);
        } else {
            session.setPlayerState(victim.getUniqueId(), BedFightState.ENDED);
            session.setPlayerState(killer != null ? killer.getUniqueId() : null, BedFightState.ENDED);

            ChatColor victimColor = team.equalsIgnoreCase("RED") ? ChatColor.RED : ChatColor.BLUE;
            ChatColor killerColor = ChatColor.WHITE;
            if (killer != null) {
                String killerTeam = session.getTeam(killer.getUniqueId());
                killerColor = (killerTeam != null && killerTeam.equalsIgnoreCase("RED")) ? ChatColor.RED : ChatColor.BLUE;
                session.getStats(killer.getUniqueId()).finalKills++;
                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 100f, 1f);
            }

            String msg = (killer != null) ?
                    String.format(BedFightMessages.FINAL_KILL, victimColor + victim.getName(), killerColor + killer.getName()) :
                    String.format(BedFightMessages.FINAL_DEATH, victimColor + victim.getName());
            broadcastMessage(session, ChatColor.YELLOW + msg.replace("FINAL KILL", ChatColor.AQUA + "" + ChatColor.BOLD + "FINAL KILL" + ChatColor.YELLOW));

            if (team.equals("RED")) session.setRedEliminated(true);
            else session.setBlueEliminated(true);

            updateScoreboard(session);
            Player winner = team.equals("RED") ? Bukkit.getPlayer(session.getBluePlayer()) : Bukkit.getPlayer(session.getRedPlayer());
            bedFightManager.endMatch(session, winner);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;
        
        Location deathLoc = deathLocations.remove(player.getUniqueId());
        plugin.getLogger().info("DEBUG: Retrieving death location for " + player.getName() + ": " + deathLoc);

        if (deathLoc != null) {
            event.setRespawnLocation(deathLoc);
        } else {
            String team = session.getTeam(player.getUniqueId());
            if (team != null) {
                boolean bedAlive = (team.equals("RED")) ? session.isRedBedAlive() : session.isBlueBedAlive();
                if (bedAlive) {
                    event.setRespawnLocation(session.getSpawn(player.getUniqueId()));
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.PREPARE) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
            return;
        }

        Arena arena = session.getArena();
        if (event.getTo().getY() <= arena.getVoidLimit()) {
            if (session.getPlayerState(player.getUniqueId()) == BedFightState.PLAYING) {
                handleVoidFall(player, session);
            }
        }
    }

    private void updateScoreboard(BedFightSession session) {
        Player p1 = Bukkit.getPlayer(session.getRedPlayer());
        Player p2 = Bukkit.getPlayer(session.getBluePlayer());
        if (p1 != null) plugin.getBedFightScoreboard().updateScoreboard(p1);
        if (p2 != null) plugin.getBedFightScoreboard().updateScoreboard(p2);
    }

    private void handleVoidFall(Player player, BedFightSession session) {
        String team = session.getTeam(player.getUniqueId());
        boolean bedAlive = team.equals("RED") ? session.isRedBedAlive() : session.isBlueBedAlive();

        UUID attackerUUID = lastAttacker.get(player.getUniqueId());
        Player killer = (attackerUUID != null && System.currentTimeMillis() - hitTimestamp.getOrDefault(player.getUniqueId(), 0L) < 5000)
                ? Bukkit.getPlayer(attackerUUID) : null;

        ChatColor victimColor = team.equalsIgnoreCase("RED") ? ChatColor.RED : ChatColor.BLUE;
        ChatColor killerColor = ChatColor.WHITE;
        if (killer != null) {
            String killerTeam = session.getTeam(killer.getUniqueId());
            killerColor = (killerTeam != null && killerTeam.equalsIgnoreCase("RED")) ? ChatColor.RED : ChatColor.BLUE;
        }

        // Play sound for the opponent only
        Player p1 = Bukkit.getPlayer(session.getRedPlayer());
        Player p2 = Bukkit.getPlayer(session.getBluePlayer());
        Player opponent = player.equals(p1) ? p2 : p1;

        if (opponent != null) {
            opponent.playSound(opponent.getLocation(), Sound.ORB_PICKUP, 100f, 1f);
        }

        if (!bedAlive) {
            session.setPlayerState(player.getUniqueId(), BedFightState.BED_DESTROYED);
            // ... (rest of method)
            if (killer != null) {
                session.getStats(killer.getUniqueId()).voidFinalKills++;
            }

            String msg = (killer != null) ?
                    String.format(BedFightMessages.FINAL_VOID_KILL, victimColor + player.getName(), killerColor + killer.getName()) :
                    String.format(BedFightMessages.FINAL_VOID_DEATH, victimColor + player.getName());

            broadcastMessage(session, ChatColor.YELLOW + msg.replace("FINAL KILL", ChatColor.AQUA + "" + ChatColor.BOLD + "FINAL KILL" + ChatColor.YELLOW));

            if (team.equals("RED")) session.setRedEliminated(true);
            else session.setBlueEliminated(true);

            updateScoreboard(session);
            Player winner = team.equals("RED") ? Bukkit.getPlayer(session.getBluePlayer()) : Bukkit.getPlayer(session.getRedPlayer());
            bedFightManager.endMatch(session, winner);
            return;
        }

        session.setPlayerState(player.getUniqueId(), BedFightState.DIED);
        
        if (killer != null) {
            session.getStats(killer.getUniqueId()).voidKills++;
        }

        String msg = (killer != null) ?
                String.format(BedFightMessages.VOID_KILL, victimColor + player.getName(), killerColor + killer.getName()) :
                String.format(BedFightMessages.VOID_DEATH, victimColor + player.getName());

        broadcastMessage(session, ChatColor.YELLOW + msg);
        handleRespawnSequence(player, session);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        BedFightSession victimSession = bedFightManager.getSession(victim);
        BedFightSession attackerSession = bedFightManager.getSession(attacker);

        if ((victimSession != null && (victimSession.getPlayerState(victim.getUniqueId()) == BedFightState.PREPARE || victimSession.getPlayerState(victim.getUniqueId()) == BedFightState.DIED || victimSession.getPlayerState(victim.getUniqueId()) == BedFightState.SPECTATOR || victimSession.getPlayerState(victim.getUniqueId()) == BedFightState.ENDED)) ||
            (attackerSession != null && (attackerSession.getPlayerState(attacker.getUniqueId()) == BedFightState.PREPARE || attackerSession.getPlayerState(attacker.getUniqueId()) == BedFightState.DIED || attackerSession.getPlayerState(attacker.getUniqueId()) == BedFightState.SPECTATOR || attackerSession.getPlayerState(attacker.getUniqueId()) == BedFightState.ENDED))) {
            event.setCancelled(true);
            return;
        }

        if (victimSession != null && attackerSession != null && victimSession == attackerSession) {
            if (victimSession.getTeam(victim.getUniqueId()).equals(attackerSession.getTeam(attacker.getUniqueId()))) {
                event.setCancelled(true);
                return;
            }
        }

        if (victim.hasMetadata("bedfight_invincible")) {
            event.setCancelled(true);
        }

        if (attacker.hasMetadata("bedfight_invincible")) {
            attacker.removeMetadata("bedfight_invincible", plugin);
            attacker.sendMessage(ChatColor.YELLOW + "Invincibility removed!");
        }

        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
        hitTimestamp.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    private void handleRespawnSequence(Player player, BedFightSession session) {
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setMetadata("bedfight_invincible", new FixedMetadataValue(plugin, true));
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(20.0);
        player.setFoodLevel(20);

        Player opponent = Bukkit.getPlayer(session.getTeam(player.getUniqueId()).equals("RED") ? session.getBluePlayer() : session.getRedPlayer());
        if (opponent != null) {
            opponent.hidePlayer(player);
        }

        for (int i = 0; i < 3; i++) {
            final int secondsLeft = 3 - i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendTitle(player, ChatColor.RED + "YOU DIED", ChatColor.YELLOW + "Respawning in " + secondsLeft);
                player.sendMessage(ChatColor.YELLOW + ""+secondsLeft + "...");
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                updateScoreboard(session);
            }, i * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location spawn = session.getSpawn(player.getUniqueId());
            if (spawn == null || spawn.getWorld() == null) {
                plugin.getLogger().warning("Invalid spawn location for player " + player.getName() + ", falling back to match world spawn.");
                spawn = session.getMatchWorld().getSpawnLocation();
            }
            Location safeSpawn = spawn.clone().add(0, 1, 0);
            
            sendTitle(player, " ", "");
            safeSpawn.clone().add(0, 0, 0).getBlock().setType(Material.AIR);
            safeSpawn.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
            player.teleport(safeSpawn);

            player.setFlying(false);
            player.setAllowFlight(false);
            if (opponent != null) {
                opponent.showPlayer(player);
            }

            session.setPlayerState(player.getUniqueId(), BedFightState.RESPAWNED);
            updateScoreboard(session);

            plugin.getKitManager().applyBedFightKit(player, session.getTeam(player.getUniqueId()));
            player.sendMessage(ChatColor.YELLOW + "Respawned!");
        }, 3 * 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.removeMetadata("bedfight_invincible", plugin);

            session.setPlayerState(player.getUniqueId(), BedFightState.PLAYING);
            updateScoreboard(session);
        }, 5 * 20L);
    }
public void broadcastMessage(BedFightSession session, String message) {
    for (UUID uuid : session.getAllPlayers()) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendMessage(message);
    }
    for (UUID uuid : session.getSpectators()) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendMessage(message);
    }
    plugin.getLogger().info("BedFight Msg: " + ChatColor.stripColor(message));
}

    private void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle);
    }

    private boolean isSameLocation(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ() &&
                loc1.getWorld().getName().equals(loc2.getWorld().getName());
    }

    private void broadcastBedBreak(BedFightSession session, String color, Player breaker) {
        ChatColor bedTeamColor = color.equalsIgnoreCase("RED") ? ChatColor.RED : ChatColor.BLUE;

        String breakerTeam = session.getTeam(breaker.getUniqueId());
        ChatColor breakerColor = (breakerTeam != null && breakerTeam.equalsIgnoreCase("RED")) ? ChatColor.RED : ChatColor.BLUE;

        String msg = ChatColor.YELLOW + "" + ChatColor.BOLD + "BED DESTRUCTION " + ChatColor.GRAY + "" + ChatColor.BOLD + " » " +
                bedTeamColor + color + " Bed" + ChatColor.YELLOW + " was destroyed by " + breakerColor + breaker.getName() + ChatColor.YELLOW + "!";

        broadcastMessage(session, msg);
        
        // Send title to the team whose bed was destroyed
        for (UUID uuid : session.getPlayersByTeam(color)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                sendTitle(p, ChatColor.RED + "" + ChatColor.BOLD + "BED DESTROYED", ChatColor.WHITE + "you will no longer respawn");
            }
        }
        
        // Play sounds
        breaker.playSound(breaker.getLocation(), Sound.ENDERDRAGON_GROWL, 1f, 1f);
        
        for (UUID uuid : session.getPlayersByTeam(color)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.playSound(p.getLocation(), Sound.WITHER_DEATH, 1f, 1f);
        }

        plugin.getLogger().info("Bed Destruction: " + color + " bed destroyed by " + breaker.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.getPlayerState(player.getUniqueId()) == BedFightState.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        if (session.isSpectator(player.getUniqueId())) {
            session.getSpectators().remove(player.getUniqueId());
            return;
        }

        UUID opponentUUID = session.getRedPlayer().equals(player.getUniqueId()) ? session.getBluePlayer() : session.getRedPlayer();
        Player opponent = Bukkit.getPlayer(opponentUUID);

        String msg = String.format(BedFightMessages.DISCONNECT, player.getName());
        broadcastMessage(session, ChatColor.RED + msg);

        bedFightManager.endMatch(session, opponent);
    }
}
