package me.molfordan.arenaAndFFAManager;

import me.molfordan.arenaAndFFAManager.commands.admin.*;
import me.molfordan.arenaAndFFAManager.commands.arena.ArenaBypassCommand;
import me.molfordan.arenaAndFFAManager.commands.arena.ArenaCommand;
import me.molfordan.arenaAndFFAManager.commands.arena.ArenaConfigReloadCommand;
import me.molfordan.arenaAndFFAManager.commands.bridgefight.BridgeBanCommand;
import me.molfordan.arenaAndFFAManager.commands.bridgefight.PlatformCommand;
import me.molfordan.arenaAndFFAManager.commands.common.HotbarManagerCommand;
import me.molfordan.arenaAndFFAManager.commands.common.LeaderboardCommand;
import me.molfordan.arenaAndFFAManager.commands.common.ReportCommand;
import me.molfordan.arenaAndFFAManager.commands.common.StatsCommand;
import me.molfordan.arenaAndFFAManager.commands.common.gui.GUICustomKit;
import me.molfordan.arenaAndFFAManager.commands.utils.*;
import me.molfordan.arenaAndFFAManager.commands.common.gui.GUILeaderboardCommand;
import me.molfordan.arenaAndFFAManager.commands.common.gui.GUIStatsCommand;
import me.molfordan.arenaAndFFAManager.commands.world.BridgeFightCommand;
import me.molfordan.arenaAndFFAManager.commands.bridgefight.BridgeFightKitCommand;
import me.molfordan.arenaAndFFAManager.commands.bridgefight.BridgeUnbanCommand;
import me.molfordan.arenaAndFFAManager.commands.world.BuildFFACommand;
import me.molfordan.arenaAndFFAManager.commands.world.LobbyCommand;
import me.molfordan.arenaAndFFAManager.commands.world.PrivateWorldCommand;
import me.molfordan.arenaAndFFAManager.config.BridgeFightConfig;
import me.molfordan.arenaAndFFAManager.config.RegionsConfig;
import me.molfordan.arenaAndFFAManager.database.DatabaseManager;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboardBridgeFight;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboardBuildFFA;
import me.molfordan.arenaAndFFAManager.gui.GUILeaderboardMain;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarListener;
import me.molfordan.arenaAndFFAManager.hotbarmanager.HotbarSorter;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BridgeFightKitManager;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkit.gui.CustomKitBaseGUI;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui.BridgeFightKitGUI;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui.EditKitListener;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.gui.SelectKitListener;
import me.molfordan.arenaAndFFAManager.listener.*;
import me.molfordan.arenaAndFFAManager.manager.*;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.SerializableBlockState;
import me.molfordan.arenaAndFFAManager.placeholder.LeaderboardPlaceholderExpansion;
import me.molfordan.arenaAndFFAManager.region.CommandRegionManager;
import me.molfordan.arenaAndFFAManager.region.RegionFlagListener;
import me.molfordan.arenaAndFFAManager.restore.DailyArenaRestorer;
import me.molfordan.arenaAndFFAManager.restore.PersistentRestoreManager;
import me.molfordan.arenaAndFFAManager.spawnitem.SpawnItem;
import me.molfordan.arenaAndFFAManager.task.CombatTagDisplayTask;
import me.molfordan.arenaAndFFAManager.utils.CommandRegister;
import me.molfordan.arenaAndFFAManager.utils.WorldGuardUtils;
//import me.molfordan.arenaAndFFAManager.utils.FlightManager;
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
    private BridgeFightKitGUI bridgeFightKitGUI;
    private TeleportPendingManager teleportPendingManager;
    private CombatManager combatManager;
    private DeathMessageManager deathMessageManager;
    private PersistentRestoreManager persistentRestoreManager;
    private HotbarSessionManager hotbarSessionManager;
    private HotbarDataManager hotbarDataManager;
    private GUILeaderboardMain guiLeaderboardMain;
    private KitManager kitManager;
    private CustomKitBaseGUI customKitBaseGUI;
    public FileConfiguration configuration;
    private BridgeFightConfig bridgeFightConfig;
    private PlatformManager platformManager;
    private CommandRegionManager regionManager;
    private RegionsConfig regionsConfig;
    private StatsManager statsManager;
    private DatabaseManager databaseManager;
    private GUILeaderboardBridgeFight guiLeaderboardBridgeFight;
    private GUILeaderboardBuildFFA guiLeaderboardBuildFFA;
    private BridgeFightKitManager bridgeFightKitManager;
    private LeaderboardPlaceholderExpansion placeholderLeaderboardExpansion;
    private InvisPlayerListener invisPlayerListener;
    private DailyArenaRestorer dailyArenaRestorer;
    //private FlightManager flightManager;
    private FrozenManager frozenManager;
    private ReportManager reportManager;
    private SpawnItem spawnItem;
    private BackupManager backupManager; // Add this line
    private AutoRestartManager autoRestartManager;
    private FireballTracker fireballTracker;
    private static final String LOBBY_PATH = "lobby";
    private static final String BUILDFFA_PATH = "buildffa";
    private static final String BRIDGEFIGHT_PATH = "bridgefight";
    private static final String PRIVATEWORLD_PATH = "privateworld";

    @Override
    public void onEnable() {
        plugin = this;

        // Initialize WorldGuard integration
        WorldGuardUtils.initialize(this);
        if (WorldGuardUtils.isWorldGuardAvailable()) {
            getLogger().info("WorldGuard integration enabled!");
        } else {
            getLogger().warning("WorldGuard not found - some features may be limited");
        }


        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
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
        this.fireballTracker = new FireballTracker();
        this.deathMessageManager = new DeathMessageManager(this,combatManager, arenaManager, hotbarDataManager, statsManager, fireballTracker);
        this.hotbarSorter = new HotbarSorter(hotbarDataManager);
        this.bridgeFightBanManager = new BridgeFightBanManager(getDataFolder());
        this.placeholderLeaderboardExpansion = new LeaderboardPlaceholderExpansion(this);
        this.guiLeaderboardBridgeFight = new GUILeaderboardBridgeFight(this);
        this.guiLeaderboardBuildFFA = new GUILeaderboardBuildFFA(this);
        this.guiLeaderboardMain = new GUILeaderboardMain(this);
        this.spawnItem = new SpawnItem(this);
        this.frozenManager = new FrozenManager(this);
        this.reportManager = new ReportManager(this, getDataFolder());
        this.teleportPendingManager = new TeleportPendingManager();
        //this.flightManager = new FlightManager(configManager.getLobbyWorldName());
        this.bridgeFightKitManager = new BridgeFightKitManager(this);
        this.bridgeFightKitGUI = new BridgeFightKitGUI(this);
        this.customKitBaseGUI = new CustomKitBaseGUI(this);
        persistentRestoreManager.loadAll();
        persistentRestoreManager.startAutoSave();
        String lobbyWorld = configManager.getLobbyWorldName();
        String buildFFAWorld = configManager.getBuildFFAWorldName();
        String bridgeFightWorld = configManager.getBridgeFightWorldName();
        String privateWorldWorld = configManager.getPrivateWorldWorldName();
        double bridgeFightVoidLimit = configManager.getBridgeFightVoidLimit();
        configManager.ensureConfigDefaults();
        configManager.loadWorld(lobbyWorld, WorldType.NORMAL);
        configManager.loadWorld(buildFFAWorld, WorldType.NORMAL);
        configManager.loadWorld(bridgeFightWorld, WorldType.NORMAL);
        configManager.loadWorld(privateWorldWorld, WorldType.FLAT);
        platformManager.loadFromConfig(bridgeFightConfig.getConfig());
        regionManager.loadAllFromConfig();
        arenaManager.loadArenas();
        Bukkit.getLogger().info("[ArenaManager] Arenas loaded successfully.");
        dailyArenaRestorer = new DailyArenaRestorer(this, arenaManager);
        this.autoRestartManager = new AutoRestartManager(this, configManager);
        new CombatTagDisplayTask(combatManager).runTaskTimer(this, 0L, 1L);
        getCommand("arenamap").setExecutor(new ArenaCommand(arenaManager, configManager, this));
        getCommand("arenabypass").setExecutor(new ArenaBypassCommand(arenaManager));
        getCommand("commandbypass").setExecutor(new BypassCommandsCommand(combatManager));
        getCommand("arenaconfig").setExecutor(new ArenaConfigReloadCommand(configManager));
        getCommand("setlobby").setExecutor(new SetLobbyCommand(configManager));
        getCommand("build").setExecutor(new BuildModeCommand(configManager));
        getCommand("buildffa").setExecutor(new BuildFFACommand(configManager));
        
        // Create LobbyListener instance to be used by both event registration and BridgeFightCommand
        LobbyListener lobbyListener = new LobbyListener(configManager, this, kitManager);
        
        getCommand("bridgefight").setExecutor(new BridgeFightCommand(configManager, this, lobbyListener));
        getCommand("loadworld").setExecutor(new LoadWorldCommand(configManager, this));
        getCommand("spawn").setExecutor(new LobbyCommand(configManager, teleportPendingManager, this));
        getCommand("setpos1").setExecutor(new SetPlatformPosCommand(this, platformManager));
        getCommand("setpos2").setExecutor(new SetPlatformPosCommand(this, platformManager));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("guistats").setExecutor(new GUIStatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("guileaderboard").setExecutor(new GUILeaderboardCommand(this));
        RegionCommand rc = new RegionCommand(regionManager);
        getCommand("rc").setExecutor(rc);
        getCommand("rc").setTabCompleter(rc);
        
        getCommand("hotbarmanager").setExecutor(
                new HotbarManagerCommand(hotbarSessionManager)
        );
        new me.molfordan.arenaAndFFAManager.task.RegionTriggerTask(regionManager).runTaskTimer(this, 0L, 1L);
        int maxPlatforms = 10; // change this to any number
        for (int i = 1; i <= maxPlatforms; i++) {
            // plat commands
            CommandRegister.registerCommand(new PlatformCommand("plat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new PlatformCommand("bigplat" + i, this, kitManager, platformManager));
            // spawn setter commands
            CommandRegister.registerCommand(new PlatformCommand("setplat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new PlatformCommand("setbigplat" + i, this, kitManager, platformManager));
        }
        getCommand("bridgeban").setExecutor(new BridgeBanCommand(bridgeFightBanManager, this));
        getCommand("bridgeunban").setExecutor(new BridgeUnbanCommand(bridgeFightBanManager));
        getCommand("playerhistory").setExecutor(new PlayerHistoryCommand(this));
        getCommand("freeze").setExecutor(new FrozenCommand(this));
        getCommand("unfreeze").setExecutor(new UnfrozenCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("reports").setExecutor(new ReportsCommand(this));
        getCommand("statsreset").setExecutor(new StatsResetCommand(this));
        getCommand("privateworld").setExecutor(new PrivateWorldCommand(this));
        getCommand("setstats").setExecutor(new SetCommand(this));
        getCommand("kit").setExecutor(new BridgeFightKitCommand(this));
        getCommand("debugging").setExecutor(new GUICustomKit(this));

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
        getServer().getPluginManager().registerEvents(lobbyListener, this);
        getServer().getPluginManager().registerEvents(new GlobalListener(statsManager, this), this);
        getServer().getPluginManager().registerEvents(new BuildFFAListener(configManager, kitManager), this);
        getServer().getPluginManager().registerEvents(new BridgeFightListener(platformManager, configManager, this), this);
        getServer().getPluginManager().registerEvents(new HotbarListener(this, hotbarSessionManager, kitManager), this);
        getConfig().addDefault("debug", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(new ItemReceiveListener(buildFFAWorld, hotbarSorter), this);
        getServer().getPluginManager().registerEvents(new LeaderboardGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EnderPearlListener(this), this);
        getServer().getPluginManager().registerEvents(new PrivateWorldListener(this), this);
        getServer().getPluginManager().registerEvents(this.frozenManager, this);
        getServer().getPluginManager().registerEvents(new TeleportCancelListener(this.teleportPendingManager), this);
        getServer().getPluginManager().registerEvents(new StatsGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EggBridgeListener(), this);
        getServer().getPluginManager().registerEvents(
                new BridgeFightBanListener(bridgeFightBanManager), this
        );
        getServer().getPluginManager().registerEvents(new RegionListener(regionManager), this);
        getServer().getPluginManager().registerEvents(new RegionFlagListener(regionManager), this);
        getServer().getPluginManager().registerEvents(new InvisPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new SelectKitListener(this), this);
        getServer().getPluginManager().registerEvents(new EditKitListener(this), this);
        getServer().getPluginManager().registerEvents(new FireballListener(this.configManager, fireballTracker), this);
        getServer().getPluginManager().registerEvents(new ArmorRemovalListener(this, arenaManager), this);
        getServer().getPluginManager().registerEvents(this.deathMessageManager, this);

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

        // Start auto-restart system
        autoRestartManager.start();

        getLogger().info("ArenaAndFFAManager has been enabled.");

        // Reset all daily streaks on server startup
        if (statsManager != null) {
            statsManager.resetAllDailyStreaks();
        }


    }
    @Override
    public void onDisable() {
        getLogger().info("ArenaAndFFAManager has been disabled.");

        // Reset all daily streaks on server shutdown
        if (statsManager != null) {
            statsManager.resetAllDailyStreaks();
        }

        if (arenaManager != null) {
            arenaManager.saveAllArenas();
        }

        if (persistentRestoreManager != null) {
            persistentRestoreManager.saveAll();
        }

        if (statsManager != null) {
            statsManager.shutdown();
        }

        if (autoRestartManager != null) {
            autoRestartManager.stop();
        }

        if (fireballTracker != null) {
            fireballTracker.cleanup();
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

    public static ArenaAndFFAManager getInstance(){
        return plugin;
    }

    public void debug(String msg) {
        try {
            if (configManager != null
                    && configManager.getConfig() != null
                    && configManager.getConfig().getBoolean("debug")) {

                Bukkit.getLogger().info("[ArenaManager] " + msg);
            }
        } catch (Exception ignored) {
            // Never break startup
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
    public GUILeaderboardBridgeFight getGuiLeaderboardBridgeFight() {
        return guiLeaderboardBridgeFight;
    }
    public GUILeaderboardBuildFFA getGuiLeaderboardBuildFFA() {
        return guiLeaderboardBuildFFA;
    }
    public GUILeaderboardMain getGuiLeaderboardMain() {
        return guiLeaderboardMain;
    }
    public SpawnItem getSpawnItem() {
        return spawnItem;
    }

    public ReportManager getReportManager(){
        return reportManager;
    }
    public BridgeFightKitManager getBridgeFightKitManager(){
        return bridgeFightKitManager;
    }
    public BridgeFightKitGUI getBridgeFightGUI(){
        return bridgeFightKitGUI;
    }

    public AutoRestartManager getAutoRestartManager() {
        return autoRestartManager;
    }

    public FireballTracker getFireballTracker() {
        return fireballTracker;
    }

    public FrozenManager getFrozenManager(){
        return frozenManager;
    }

    public LeaderboardPlaceholderExpansion getLeaderboardPlaceholderExpansion() {
        return placeholderLeaderboardExpansion;
    }
    public List<LeaderboardPlaceholderExpansion.LBEntry> getLeaderboard(String key) {
        return placeholderLeaderboardExpansion.getLeaderboardCache().getOrDefault(key, java.util.Collections.emptyList());
    }
    public DeathMessageManager getDeathMessageManager() {
        return deathMessageManager;
    }

    public PlatformManager getPlatformManager() {
        return platformManager;
    }

    public CommandRegionManager getRegionManager() {
        return regionManager;
    }

    public InvisPlayerListener getInvisPlayerListener() {
        return invisPlayerListener;
    }

    public CustomKitBaseGUI getCustomKitBaseGUI() {
        return customKitBaseGUI;
    }
}