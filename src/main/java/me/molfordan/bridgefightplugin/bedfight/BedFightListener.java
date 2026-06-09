package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.Arena;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BedFightListener implements Listener {
    private final BridgeFightPlugin plugin;
    private final BedFightManager bedFightManager;
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, Long> hitTimestamp = new ConcurrentHashMap<>();
    private final Map<UUID, Long> killMessageTimestamp = new ConcurrentHashMap<>();
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public BedFightListener(BridgeFightPlugin plugin, BedFightManager bedFightManager) {
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
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;

        // Prevent armor removal
        if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
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

    private void sendActionBar(Player player, String message) {
        try {
            String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> chatComponentText = Class.forName("net.minecraft.server." + nmsVersion + ".ChatComponentText");
            Class<?> iChatBaseComponent = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
            Class<?> packetPlayOutChat = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");
            
            Object component = chatComponentText.getConstructor(String.class).newInstance(message);
            Object packet = packetPlayOutChat.getConstructor(iChatBaseComponent, byte.class).newInstance(component, (byte) 2);
            
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + nmsVersion + ".Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBedPickup(PlayerPickupItemEvent event){
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;
        if (event.getItem().equals(Material.BED)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        BedFightSession session = bedFightManager.getSession(victim);
        if (session == null) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        if (session.getPlayerState(victim.getUniqueId()) == BedFightState.PREPARE) {
            return;
        }

        victim.spigot().respawn();
        processDeath(victim, killer, session, false);
    }

    private void processDeath(Player victim, Player killer, BedFightSession session, boolean isVoid) {
        if (session.getPlayerState(victim.getUniqueId()) == BedFightState.DIED ||
                session.getPlayerState(victim.getUniqueId()) == BedFightState.SPECTATOR_DUEL ||
                session.getPlayerState(victim.getUniqueId()) == BedFightState.ENDED) {
            return;
        }

        // Save death location
        deathLocations.put(victim.getUniqueId(), victim.getLocation());

        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);
        victim.setHealth(20.0);
        victim.setFoodLevel(20);
        victim.setFireTicks(0);
        victim.setFallDistance(0);

        String team = session.getTeam(victim.getUniqueId());
        boolean bedAlive = (team.equals("RED")) ? session.isRedBedAlive() : session.isBlueBedAlive();
        ChatColor victimColor = team.equalsIgnoreCase("RED") ? ChatColor.RED : ChatColor.BLUE;

        if (bedAlive) {
            session.setPlayerState(victim.getUniqueId(), BedFightState.DIED);

            ChatColor killerColor = ChatColor.WHITE;
            if (killer != null && killer != victim) {
                String killerTeam = session.getTeam(killer.getUniqueId());
                killerColor = (killerTeam != null && killerTeam.equalsIgnoreCase("RED")) ? ChatColor.RED : ChatColor.BLUE;

                BedFightStats killerStats = session.getStats(killer.getUniqueId());
                if (isVoid) killerStats.voidKills++;
                else killerStats.kills++;

                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 100f, 1f);
                killMessageTimestamp.put(killer.getUniqueId(), System.currentTimeMillis() + 2000);

                String actionText = isVoid ? ChatColor.RED + "" + ChatColor.BOLD + "VOID KILL! " : ChatColor.RED + "" + ChatColor.BOLD + "KILL! ";
                sendActionBar(killer, actionText + victimColor + victim.getName());
            }

            String msgFormat;
            if (isVoid) {
                msgFormat = (killer != null && killer != victim) ? BedFightMessages.VOID_KILL : BedFightMessages.VOID_DEATH;
            } else {
                msgFormat = (killer != null && killer != victim) ? BedFightMessages.REGULAR_KILL : BedFightMessages.REGULAR_DEATH;
            }

            String msg = (killer != null && killer != victim) ?
                    String.format(msgFormat, victimColor + victim.getName(), killerColor + killer.getName()) :
                    String.format(msgFormat, victimColor + victim.getName());

            broadcastMessage(session, ChatColor.YELLOW + msg);
            handleRespawnSequence(victim, session);
        } else {
            // Bed destroyed, set to ended (eliminated)
            session.addSpectator(victim.getUniqueId());
            session.setPlayerState(victim.getUniqueId(), BedFightState.SPECTATOR_DUEL);

            victim.setGameMode(GameMode.ADVENTURE);
            victim.setAllowFlight(true);
            victim.setFlying(true);

            SpectatorListener.giveSpectatorItems(victim);

            for (UUID uuid : session.getAllPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.hidePlayer(victim);
            }
            for (UUID specId : session.getSpectators()) {
                Player spec = Bukkit.getPlayer(specId);
                if (spec != null) victim.showPlayer(spec);
            }

            ChatColor killerColor = ChatColor.WHITE;
            if (killer != null && killer != victim) {
                String killerTeam = session.getTeam(killer.getUniqueId());
                killerColor = (killerTeam != null && killerTeam.equalsIgnoreCase("RED")) ? ChatColor.RED : ChatColor.BLUE;

                BedFightStats killerStats = session.getStats(killer.getUniqueId());
                if (isVoid) killerStats.voidFinalKills++;
                else killerStats.finalKills++;

                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 100f, 1f);
                killMessageTimestamp.put(killer.getUniqueId(), System.currentTimeMillis() + 2000);

                String actionText = isVoid ? ChatColor.RED + "" + ChatColor.BOLD + "FINAL VOID KILL! " : ChatColor.RED + "" + ChatColor.BOLD + "FINAL KILL! ";
                sendActionBar(killer, actionText + victimColor + victim.getName());

                String msgFormat = isVoid ? BedFightMessages.FINAL_VOID_KILL : BedFightMessages.FINAL_KILL;
                String msg = String.format(msgFormat, victimColor + victim.getName(), killerColor + killer.getName());
                broadcastMessage(session, ChatColor.YELLOW + msg.replace("FINAL KILL", ChatColor.AQUA + "" + ChatColor.BOLD + "FINAL KILL" + ChatColor.YELLOW));
            } else {
                String msgFormat = isVoid ? BedFightMessages.FINAL_VOID_DEATH : BedFightMessages.FINAL_DEATH;
                String msg = String.format(msgFormat, victimColor + victim.getName());
                broadcastMessage(session, ChatColor.YELLOW + msg.replace("FINAL KILL", ChatColor.AQUA + "" + ChatColor.BOLD + "FINAL KILL" + ChatColor.YELLOW));
            }

            checkTeamElimination(session, team);
        }

        updateScoreboard(session);
    }

    private void checkTeamElimination(BedFightSession session, String team) {
        boolean allEliminated = true;
        plugin.getLogger().info("DEBUG: Checking elimination for team: " + team);
        for (UUID memberId : new ArrayList<>(session.getPlayersByTeam(team))) {
            BedFightState state = session.getPlayerState(memberId);
            plugin.getLogger().info("DEBUG: Player " + memberId + " state: " + state);
            if (state != BedFightState.ENDED && state != BedFightState.SPECTATOR_DUEL) {
                allEliminated = false;
                break;
            }
        }

        if (allEliminated) {
            plugin.getLogger().info("DEBUG: Team " + team + " eliminated!");
            if (team.equals("RED")) session.setRedEliminated(true);
            else session.setBlueEliminated(true);

            updateScoreboard(session);
            
            String winnerTeam = team.equals("RED") ? "BLUE" : "RED";
            Set<UUID> winnerTeamPlayers = session.getPlayersByTeam(winnerTeam);
            Player winner = winnerTeamPlayers.isEmpty() ? null : Bukkit.getPlayer(winnerTeamPlayers.iterator().next());
            
            bedFightManager.endMatch(session, winner);
        } else {
            plugin.getLogger().info("DEBUG: Team " + team + " still has active players.");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        BedFightSession session = bedFightManager.getSession(player);
        if (session == null) return;
        
        // Use saved death location if it exists, otherwise fallback to team spawn
        Location deathLoc = deathLocations.remove(player.getUniqueId());
        if (deathLoc != null) {
            event.setRespawnLocation(deathLoc);
        } else {
            Location spawn = session.getSpawn(player.getUniqueId());
            if (spawn != null) {
                event.setRespawnLocation(spawn);
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
        Set<UUID> allToUpdate = new HashSet<>(session.getAllPlayers());
        allToUpdate.addAll(session.getSpectators());

        for (UUID uuid : allToUpdate) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getBedFightScoreboard().updateScoreboard(p);
        }
    }

    private void handleVoidFall(Player player, BedFightSession session) {
        UUID attackerUUID = lastAttacker.get(player.getUniqueId());
        Player killer = (attackerUUID != null && System.currentTimeMillis() - hitTimestamp.getOrDefault(player.getUniqueId(), 0L) < 5000)
                ? Bukkit.getPlayer(attackerUUID) : null;

        processDeath(player, killer, session, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        BedFightSession session = bedFightManager.getSession(victim);
        if (session == null) return;

        if (session.getPlayerState(victim.getUniqueId()) == BedFightState.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        // Death detection: if health drops to 0.5 or below (exclude fall damage)
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL && 
            victim.getHealth() - event.getFinalDamage() <= 0.5) {
            event.setCancelled(true);

            Player killer = null;
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof Player) {
                    killer = (Player) e.getDamager();
                }
            }

            if (killer == null) {
                UUID attackerUUID = lastAttacker.get(victim.getUniqueId());
                if (attackerUUID != null && System.currentTimeMillis() - hitTimestamp.getOrDefault(victim.getUniqueId(), 0L) < 5000) {
                    killer = Bukkit.getPlayer(attackerUUID);
                }
            }

            processDeath(victim, killer, session, event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (!event.getEntity().getWorld().getName().startsWith("bf_")) return;

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

        updateHealthBar(attacker, victim, event.getFinalDamage());
    }

    private void updateHealthBar(Player attacker, Player victim, double finalDamage) {
        // Check if a kill message was recently sent
        if (System.currentTimeMillis() < killMessageTimestamp.getOrDefault(attacker.getUniqueId(), 0L)) {
            return; // Skip health bar update
        }

        // Enemy Health Bar
        double health = Math.max(0, victim.getHealth() - finalDamage);
        double maxHealth = victim.getMaxHealth();
        int hearts = 10;
        double healthPerHeart = maxHealth / hearts;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < hearts; i++) {
            double heartHealth = (i + 1) * healthPerHeart;
            if (health >= heartHealth) {
                bar.append(ChatColor.DARK_RED).append("❤");
            } else if (health > heartHealth - healthPerHeart) {
                bar.append(ChatColor.RED).append("❤");
            } else {
                bar.append(ChatColor.GRAY).append("❤");
            }
        }

        sendActionBar(attacker, ChatColor.YELLOW + victim.getName() + " " + bar.toString());
    }

    private void handleRespawnSequence(Player player, BedFightSession session) {
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setMetadata("bedfight_invincible", new FixedMetadataValue(plugin, true));
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(20.0);
        player.setFoodLevel(20);

        String team = session.getTeam(player.getUniqueId());
        String opponentTeam = team.equals("RED") ? "BLUE" : "RED";
        for (UUID uuid : session.getPlayersByTeam(opponentTeam)) {
            Player opponent = Bukkit.getPlayer(uuid);
            if (opponent != null) opponent.hidePlayer(player);
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
            for (UUID uuid : session.getPlayersByTeam(opponentTeam)) {
                Player opponent = Bukkit.getPlayer(uuid);
                if (opponent != null) opponent.showPlayer(player);
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
        // Use a set to store unique recipients to prevent duplicate messages
        Set<UUID> recipients = new HashSet<>(session.getAllPlayers());
        recipients.addAll(session.getSpectators());
        
        for (UUID uuid : recipients) {
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

        // Increment bed break stat
        session.getStats(breaker.getUniqueId()).bedsBroken++;

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

        String team = session.getTeam(player.getUniqueId());
        String msg = String.format(BedFightMessages.DISCONNECT, player.getName());
        broadcastMessage(session, ChatColor.RED + msg);

        session.setPlayerState(player.getUniqueId(), BedFightState.ENDED);
        checkTeamElimination(session, team);
    }
}
