package me.molfordan.arenaAndFFAManager;

import me.molfordan.arenaAndFFAManager.commands.*;
import me.molfordan.arenaAndFFAManager.config.BridgeFightConfig;
import me.molfordan.arenaAndFFAManager.config.RegionsConfig;
import me.molfordan.arenaAndFFAManager.database.DatabaseManager;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboard;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboardMain;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarListener;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarSorter;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import me.molfordan.arenaAndFFAManager.listener.*;
import me.molfordan.arenaAndFFAManager.manager.*;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.placeholder.LeaderboardPlaceholderExpansion;
import me.molfordan.arenaAndFFAManager.region.CommandRegionManager;
import me.molfordan.arenaAndFFAManager.restore.DailyArenaRestorer;
import me.molfordan.arenaAndFFAManager.restore.PersistentRestoreManager;
import me.molfordan.arenaAndFFAManager.spawnitem.SpawnItem;
import me.molfordan.arenaAndFFAManager.task.CombatTagDisplayTask;
import me.molfordan.arenaAndFFAManager.utils.CommandRegister;
import org.bukkit.Bukkit;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class ArenaAndFFAManager extends JavaPlugin {
    public static ArenaAndFFAManager plugin;
    private ArenaManager arenaManager;
    private ConfigManager configManager;
    private HotbarSorter hotbarSorter;
    private BridgeFightBanManager bridgeFightBanManager;
    private CombatManager combatManager;
    private DeathMessageManager deathMessageManager;
    private PersistentRestoreManager persistentRestoreManager;
    private HotbarSessionManager hotbarSessionManager;
    private HotbarDataManager hotbarDataManager;
    private GUILeaderboardMain guiLeaderboardMain;
    private KitManager kitManager;
    public FileConfiguration configuration;
    private BridgeFightConfig bridgeFightConfig;
    private PlatformManager platformManager;
    private CommandRegionManager regionManager;
    private RegionsConfig regionsConfig;
    private StatsManager statsManager;
    private DatabaseManager databaseManager;
    private GUILeaderboard guiLeaderboard;
    private LeaderboardPlaceholderExpansion placeholderLeaderboardExpansion;
    private DailyArenaRestorer dailyArenaRestorer;
    private FrozenManager frozenManager;
    private SpawnItem spawnItem;
    private static final String LOBBY_PATH = "lobby";
    private static final String BUILDFFA_PATH = "buildffa";
    private static final String BRIDGEFIGHT_PATH = "bridgefight";

    @Override
    public void onEnable() {
        plugin = this;
        this.databaseManager = new DatabaseManager(this);

        this.configManager = new ConfigManager(this);
        this.statsManager = new StatsManager(this);
        this.bridgeFightConfig = new BridgeFightConfig(this);
        bridgeFightConfig.load();
        this.platformManager = new PlatformManager();
        ConfigurationSerialization.registerClass(Arena.class);
        ConfigurationSerialization.registerClass(SerializableBlockState.class);
        LadderRestorer ladderRestorer = new LadderRestorer();
        this.regionsConfig = new me.molfordan.arenaAndFFAManager.config.RegionsConfig(this);
        this.regionManager = new me.molfordan.arenaAndFFAManager.region.CommandRegionManager(this, regionsConfig);
        this.combatManager = new CombatManager(this);
        this.arenaManager = new ArenaManager(getDataFolder(), this, ladderRestorer);
        this.persistentRestoreManager = new PersistentRestoreManager(this);
        this.hotbarDataManager = new HotbarDataManager(this);
        this.kitManager = new KitManager(hotbarDataManager);
        this.hotbarSessionManager = new HotbarSessionManager(this, hotbarDataManager, kitManager);
        this.deathMessageManager = new DeathMessageManager(this,combatManager, arenaManager, hotbarDataManager, statsManager);
        this.hotbarSorter = new HotbarSorter(hotbarDataManager);
        this.bridgeFightBanManager = new BridgeFightBanManager(getDataFolder());
        this.placeholderLeaderboardExpansion = new LeaderboardPlaceholderExpansion(this);
        this.guiLeaderboard = new GUILeaderboard(this);
        this.guiLeaderboardMain = new GUILeaderboardMain(this);
        this.spawnItem = new SpawnItem(this);
        this.frozenManager = new FrozenManager(this);
        persistentRestoreManager.loadAll();
        persistentRestoreManager.startAutoSave();
        String lobbyWorld = configManager.getLobbyWorldName();
        String buildFFAWorld = configManager.getBuildFFAWorldName();
        String bridgeFightWorld = configManager.getBridgeFightWorldName();
        double bridgeFightVoidLimit = configManager.getBridgeFightVoidLimit();
        configManager.loadWorld(lobbyWorld, WorldType.NORMAL);
        configManager.loadWorld(buildFFAWorld, WorldType.NORMAL);
        configManager.loadWorld(bridgeFightWorld, WorldType.NORMAL);
        platformManager.loadFromConfig(bridgeFightConfig.getConfig());
        regionManager.loadAllFromConfig();
        arenaManager.loadArenas();
        Bukkit.getLogger().info("[ArenaManager] Arenas loaded successfully.");
        dailyArenaRestorer = new DailyArenaRestorer(this, arenaManager);
        new CombatTagDisplayTask(combatManager).runTaskTimer(this, 0L, 1L);
        getCommand("arenamap").setExecutor(new ArenaCommand(arenaManager, configManager));
        getCommand("arenabypass").setExecutor(new ArenaBypassCommand(arenaManager));
        getCommand("commandbypass").setExecutor(new BypassCommandsCommand(combatManager));
        getCommand("arenaconfig").setExecutor(new ArenaConfigReloadCommand(configManager));
        getCommand("setlobby").setExecutor(new SetLobbyCommand(configManager));
        getCommand("build").setExecutor(new BuildModeCommand(configManager));
        getCommand("buildffa").setExecutor(new BuildFFACommand(configManager));
        getCommand("bridgefight").setExecutor(new BridgeFightCommand(configManager));
        getCommand("loadworld").setExecutor(new LoadWorldCommand(configManager));
        getCommand("spawn").setExecutor(new LobbyCommand(configManager));
        getCommand("setpos1").setExecutor(new SetPlatformPosCommand(this, platformManager));
        getCommand("setpos2").setExecutor(new SetPlatformPosCommand(this, platformManager));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("guistats").setExecutor(new GUIStatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("guileaderboard").setExecutor(new GUILeaderboardCommand(this));
        RegionCommand rc = new me.molfordan.arenaAndFFAManager.commands.RegionCommand(regionManager);
        getCommand("rc").setExecutor(rc);
        getCommand("rc").setTabCompleter(rc);
        getCommand("hotbarmanager").setExecutor(
                new HotbarManagerCommand(hotbarSessionManager)
        );
        new me.molfordan.arenaAndFFAManager.task.RegionTriggerTask(regionManager).runTaskTimer(this, 0L, 1L);
        int maxPlatforms = 10; // change this to any number
        for (int i = 1; i <= maxPlatforms; i++) {
            // plat commands
            CommandRegister.registerCommand(new me.molfordan.arenaAndFFAManager.commands.PlatformCommand("plat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new me.molfordan.arenaAndFFAManager.commands.PlatformCommand("bigplat" + i, this, kitManager, platformManager));
            // spawn setter commands
            CommandRegister.registerCommand(new me.molfordan.arenaAndFFAManager.commands.PlatformCommand("setplat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new me.molfordan.arenaAndFFAManager.commands.PlatformCommand("setbigplat" + i, this, kitManager, platformManager));
        }
        getCommand("bridgeban").setExecutor(new BridgeBanCommand(bridgeFightBanManager));
        getCommand("bridgeunban").setExecutor(new BridgeUnbanCommand(bridgeFightBanManager));
        getCommand("playerhistory").setExecutor(new PlayerHistoryCommand(this));
        getCommand("freeze").setExecutor(new FrozenCommand(this));
        getCommand("unfreeze").setExecutor(new UnfrozenCommand(this));

        // schedule expiry cleanup every minute
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (UUID uuid : new HashSet<>(bridgeFightBanManager.getBans().keySet())) {
                bridgeFightBanManager.isPlayerBanned(uuid); // triggers auto-unban if expired
            }
        }, 20L * 60, 20L * 60);
        getServer().getPluginManager().registerEvents(new BlockEventListener(arenaManager, this, ladderRestorer, persistentRestoreManager), this);
        getServer().getPluginManager().registerEvents(new DeathEventListener(arenaManager, this, combatManager, deathMessageManager), this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerKillEventListener(combatManager, arenaManager, deathMessageManager, this), this);
        getServer().getPluginManager().registerEvents(new CombatLogListener(this,combatManager, arenaManager, deathMessageManager), this);
        getServer().getPluginManager().registerEvents(new TeleportSnowballListener(combatManager), this);
        getServer().getPluginManager().registerEvents(new ItemDropBlockerListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(configManager, this, kitManager), this);
        getServer().getPluginManager().registerEvents(new GlobalListener(statsManager), this);
        getServer().getPluginManager().registerEvents(new BuildFFAListener(configManager, kitManager), this);
        getServer().getPluginManager().registerEvents(new BridgeFightListener(platformManager, configManager), this);
        getServer().getPluginManager().registerEvents(new HotbarListener(this, hotbarSessionManager, kitManager), this);
        getConfig().addDefault("debug", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(new ItemReceiveListener(buildFFAWorld, hotbarSorter), this);
        getServer().getPluginManager().registerEvents(new LeaderboardGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EnderPearlListener(), this);
        getServer().getPluginManager().registerEvents(this.frozenManager, this);

        //new RegionTriggerTask(regionManager).runTaskTimer(this, 0L, 10L);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "nebula:main");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getServer().getMessenger().registerIncomingPluginChannel(this, "nebula:main", new PluginMessageListener() {
            @Override
            public void onPluginMessageReceived(String channel, Player player, byte[] message) {
                // not needed, but safe
            }
        });

        if (databaseManager.isConnected()) {
            getLogger().info("Database connected successfully!");
        } else {
            getLogger().warning("No database connection established!");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new me.molfordan.arenaAndFFAManager.placeholder.ArenaPlaceholderExpansion(this).register();
            getLogger().info("ArenaPlaceholderExpansion registered with PlaceholderAPI.");
            new me.molfordan.arenaAndFFAManager.placeholder.LeaderboardPlaceholderExpansion(this).register();
            getLogger().info("LeaderboardPlaceholderExpansion registered with PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found - arena placeholders not registered.");
        }

        getLogger().info("ArenaAndFFAManager has been enabled.");


    }
    @Override
    public void onDisable() {
        getLogger().info("ArenaAndFFAManager has been disabled.");

        if (arenaManager != null) {
            arenaManager.saveAllArenas();
        }

        if (persistentRestoreManager != null) {
            persistentRestoreManager.saveAll();
        }

        if (statsManager != null) {
            statsManager.resetAllPlayerStreaks();
            statsManager.saveAllAsync();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }



    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BridgeFightConfig getBridgeFightConfig(){
        return bridgeFightConfig;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public PersistentRestoreManager getPersistentRestoreManager() {
        return persistentRestoreManager;
    }

    public FileConfiguration getConfiguration() {
        return configuration;
    }

    public static ArenaAndFFAManager getPlugin(){
        return plugin;
    }

    public void debug(String message) {

        if (getConfigManager().getConfig().getBoolean("debug", true)) {
            Bukkit.getLogger().info("[DEBUG] " + message);
        }
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug", true);
    }

    public HotbarDataManager getHotbarDataManager(){
        return hotbarDataManager;
    }
    public KitManager getKitManager() {
        return kitManager;
    }
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    public StatsManager getStatsManager() { return  statsManager; }
    public BridgeFightBanManager getBridgeFightBanManager() { return bridgeFightBanManager; }
    public GUILeaderboard getGuiLeaderboard() {
        return guiLeaderboard;
    }
    public GUILeaderboardMain getGuiLeaderboardMain() {
        return guiLeaderboardMain;
    }
    public SpawnItem getSpawnItem() {
        return spawnItem;
    }

    public FrozenManager getFrozenManager(){
        return frozenManager;
    }

    public LeaderboardPlaceholderExpansion getLeaderboardPlaceholderExpansion() {
        return placeholderLeaderboardExpansion;
    }
    public List<LeaderboardPlaceholderExpansion.LBEntry> getLeaderboard(String key) {
        return placeholderLeaderboardExpansion.getCache().getOrDefault(key, java.util.Collections.emptyList());
    }
    public DeathMessageManager getDeathMessageManager() {
        return deathMessageManager;
    }

}