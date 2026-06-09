package me.molfordan.bridgefightplugin.bedfight;

import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.queue.enums.QueueType;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class BedFightSession {
    private final Arena arena;
    private final World matchWorld;
    private final QueueType queueType;
    private final Map<String, Set<UUID>> teams = new HashMap<>();
    private final Set<UUID> redTeamInitial;
    private final Set<UUID> blueTeamInitial;
    
    private final List<Location> placedBlocks = new ArrayList<>();
    private Set<UUID> spectators = new HashSet<>();
    private Map<UUID, BedFightState> playerStates = new HashMap<>();
    private Map<UUID, BedFightStats> playerStats = new HashMap<>();
    private boolean active = true;

    private boolean redBedAlive = true;
    private boolean blueBedAlive = true;
    
    private boolean redEliminated = false;
    private boolean blueEliminated = false;
    
    private Location redSpawnLoc;
    private Location blueSpawnLoc;
    private Location redBedLoc;
    private Location blueBedLoc;

    public BedFightSession(Arena arena, World matchWorld, QueueType queueType, Set<UUID> redTeam, Set<UUID> blueTeam) {
        this.arena = arena;
        this.matchWorld = matchWorld;
        this.queueType = queueType;
        this.redTeamInitial = new HashSet<>(redTeam);
        this.blueTeamInitial = new HashSet<>(blueTeam);
        
        teams.put("RED", new HashSet<>(redTeam));
        teams.put("BLUE", new HashSet<>(blueTeam));
        
        for (UUID uuid : redTeam) {
            playerStates.put(uuid, BedFightState.PREPARE);
            playerStats.put(uuid, new BedFightStats());
        }
        for (UUID uuid : blueTeam) {
            playerStates.put(uuid, BedFightState.PREPARE);
            playerStats.put(uuid, new BedFightStats());
        }
        
        initializeLocations();
    }

    public Set<UUID> getInitialTeamPlayers(String team) {
        return team.equalsIgnoreCase("RED") ? redTeamInitial : blueTeamInitial;
    }

    public String getInitialTeam(UUID uuid) {
        if (redTeamInitial.contains(uuid)) return "RED";
        if (blueTeamInitial.contains(uuid)) return "BLUE";
        return null;
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

    public QueueType getQueueType() {
        return queueType;
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
        return teams.get("RED").contains(uuid) || teams.get("BLUE").contains(uuid);
    }
    
    public String getTeam(UUID uuid) {
        if (teams.get("RED").contains(uuid)) return "RED";
        if (teams.get("BLUE").contains(uuid)) return "BLUE";
        return null;
    }
    
    public Set<UUID> getPlayersByTeam(String team) {
        return teams.getOrDefault(team.toUpperCase(), new HashSet<>());
    }
    
    public Set<UUID> getAllPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(teams.get("RED"));
        all.addAll(teams.get("BLUE"));
        return all;
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
        // Ensure player is removed from teams
        teams.get("RED").remove(uuid);
        teams.get("BLUE").remove(uuid);
        
        spectators.add(uuid);
        playerStates.put(uuid, BedFightState.SPECTATOR);
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }
    
    public boolean isParticipant(UUID uuid) {
        return isPlayerInSession(uuid);
    }

    public BedFightScoreboardState getTeamScoreboardState(String team) {
        boolean bedAlive = team.equalsIgnoreCase("RED") ? redBedAlive : blueBedAlive;
        boolean eliminated = team.equalsIgnoreCase("RED") ? redEliminated : blueEliminated;

        if (bedAlive) return BedFightScoreboardState.PLAYING;
        if (eliminated) return BedFightScoreboardState.ELIMINATED;
        return BedFightScoreboardState.BED_DESTROYED;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
