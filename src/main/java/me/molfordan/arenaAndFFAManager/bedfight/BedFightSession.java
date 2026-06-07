package me.molfordan.arenaAndFFAManager.bedfight;

import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class BedFightSession {
    private final Arena arena;
    private final World matchWorld;
    private final UUID redPlayer;
    private final UUID bluePlayer;
    
    private final List<Location> placedBlocks = new ArrayList<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, BedFightState> playerStates = new HashMap<>();
    private final Map<UUID, BedFightStats> playerStats = new HashMap<>();
    
    private boolean redBedAlive = true;
    private boolean blueBedAlive = true;
    
    private boolean redEliminated = false;
    private boolean blueEliminated = false;
    
    private Location redSpawnLoc;
    private Location blueSpawnLoc;
    private Location redBedLoc;
    private Location blueBedLoc;

    public BedFightSession(Arena arena, World matchWorld, Player red, Player blue) {
        this.arena = arena;
        this.matchWorld = matchWorld;
        this.redPlayer = red.getUniqueId();
        this.bluePlayer = blue.getUniqueId();
        
        playerStates.put(red.getUniqueId(), BedFightState.PREPARE);
        playerStates.put(blue.getUniqueId(), BedFightState.PREPARE);
        playerStats.put(red.getUniqueId(), new BedFightStats());
        playerStats.put(blue.getUniqueId(), new BedFightStats());
        
        initializeLocations();
    }

    private void initializeLocations() {
        this.redSpawnLoc = cloneToWorld(arena.getRedSpawn(), matchWorld);
        this.blueSpawnLoc = cloneToWorld(arena.getBlueSpawn(), matchWorld);
        this.redBedLoc = cloneToWorld(arena.getRedBed(), matchWorld);
        this.blueBedLoc = cloneToWorld(arena.getBlueBed(), matchWorld);
    }

    private Location cloneToWorld(Location loc, World world) {
        if (loc == null) return null;
        return new Location(world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public World getMatchWorld() {
        return matchWorld;
    }

    public Location getRedSpawnLoc() {
        return redSpawnLoc;
    }

    public Location getBlueSpawnLoc() {
        return blueSpawnLoc;
    }

    public Location getRedBedLoc() {
        return redBedLoc;
    }

    public Location getBlueBedLoc() {
        return blueBedLoc;
    }

    public Arena getArena() {
        return arena;
    }

    public UUID getRedPlayer() {
        return redPlayer;
    }

    public UUID getBluePlayer() {
        return bluePlayer;
    }

    public boolean isRedBedAlive() {
        return redBedAlive;
    }

    public void setRedBedAlive(boolean redBedAlive) {
        this.redBedAlive = redBedAlive;
    }

    public boolean isBlueBedAlive() {
        return blueBedAlive;
    }

    public void setBlueBedAlive(boolean blueBedAlive) {
        this.blueBedAlive = blueBedAlive;
    }

    public boolean isRedEliminated() {
        return redEliminated;
    }

    public void setRedEliminated(boolean redEliminated) {
        this.redEliminated = redEliminated;
    }

    public boolean isBlueEliminated() {
        return blueEliminated;
    }

    public void setBlueEliminated(boolean blueEliminated) {
        this.blueEliminated = blueEliminated;
    }
    
    public BedFightStats getStats(UUID uuid) {
        return playerStats.get(uuid);
    }
    
    public BedFightState getPlayerState(UUID uuid) {
        return playerStates.getOrDefault(uuid, BedFightState.PLAYING);
    }

    public void setPlayerState(UUID uuid, BedFightState state) {
        playerStates.put(uuid, state);
    }
    
    public boolean isPlayerInSession(UUID uuid) {
        return redPlayer.equals(uuid) || bluePlayer.equals(uuid);
    }
    
    public String getTeam(UUID uuid) {
        if (redPlayer.equals(uuid)) return "RED";
        if (bluePlayer.equals(uuid)) return "BLUE";
        return null;
    }
    
    public List<UUID> getPlayersByTeam(String team) {
        List<UUID> players = new ArrayList<>();
        if (team.equalsIgnoreCase("RED")) players.add(redPlayer);
        else if (team.equalsIgnoreCase("BLUE")) players.add(bluePlayer);
        return players;
    }
    
    public List<UUID> getAllPlayers() {
        return Arrays.asList(redPlayer, bluePlayer);
    }
    
    public Location getSpawn(UUID uuid) {
        String team = getTeam(uuid);
        if (team == null) return null;
        return team.equals("RED") ? redSpawnLoc : blueSpawnLoc;
    }

    public List<Location> getPlacedBlocks() {
        return placedBlocks;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
        playerStates.put(uuid, BedFightState.SPECTATOR);
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }
}
