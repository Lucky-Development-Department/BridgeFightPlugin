package me.molfordan.bridgefightplugin;

import me.molfordan.bridgefightplugin.bedfight.*;
import me.molfordan.bridgefightplugin.commands.admin.*;
import me.molfordan.bridgefightplugin.commands.arena.ArenaBypassCommand;
import me.molfordan.bridgefightplugin.commands.arena.ArenaCommand;
import me.molfordan.bridgefightplugin.commands.bridgefight.BridgeBanCommand;
import me.molfordan.bridgefightplugin.commands.bridgefight.PlatformCommand;
import me.molfordan.bridgefightplugin.commands.common.*;
import me.molfordan.bridgefightplugin.commands.common.gui.GUICustomKit;
import me.molfordan.bridgefightplugin.commands.utils.*;
import me.molfordan.bridgefightplugin.commands.common.gui.GUILeaderboardCommand;
import me.molfordan.bridgefightplugin.commands.common.gui.GUIStatsCommand;
import me.molfordan.bridgefightplugin.commands.world.BridgeFightCommand;
import me.molfordan.bridgefightplugin.commands.bridgefight.BridgeFightKitCommand;
import me.molfordan.bridgefightplugin.commands.bridgefight.BridgeUnbanCommand;
import me.molfordan.bridgefightplugin.commands.world.BuildFFACommand;
import me.molfordan.bridgefightplugin.commands.world.LobbyCommand;
import me.molfordan.bridgefightplugin.commands.world.PrivateWorldCommand;
import me.molfordan.bridgefightplugin.config.BridgeFightConfig;
import me.molfordan.bridgefightplugin.config.RegionsConfig;
import me.molfordan.bridgefightplugin.cosmetics.CosmeticsManager;
import me.molfordan.bridgefightplugin.database.DatabaseManager;
import me.molfordan.bridgefightplugin.gui.GUILeaderboardBridgeFight;
import me.molfordan.bridgefightplugin.gui.GUILeaderboardBuildFFA;
import me.molfordan.bridgefightplugin.gui.GUILeaderboardMain;
import me.molfordan.bridgefightplugin.gui.StatsGUI;
import me.molfordan.bridgefightplugin.hotbarmanager.BedFightHotbarListener;
import me.molfordan.bridgefightplugin.hotbarmanager.HotbarListener;
import me.molfordan.bridgefightplugin.hotbarmanager.HotbarSorter;
import me.molfordan.bridgefightplugin.kits.KitManager;
import me.molfordan.bridgefightplugin.kits.bridgefightkit.BridgeFightKitManager;
import me.molfordan.bridgefightplugin.kits.bridgefightkit.customkit.gui.CustomKitBaseGUI;
import me.molfordan.bridgefightplugin.kits.bridgefightkit.gui.BridgeFightKitGUI;
import me.molfordan.bridgefightplugin.kits.bridgefightkit.gui.EditKitListener;
import me.molfordan.bridgefightplugin.kits.bridgefightkit.gui.SelectKitListener;
import me.molfordan.bridgefightplugin.listener.PlatformWandListener;
import me.molfordan.bridgefightplugin.listener.*;
import me.molfordan.bridgefightplugin.manager.*;
import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.object.SerializableBlockState;
import me.molfordan.bridgefightplugin.placeholder.LeaderboardPlaceholderExpansion;
import me.molfordan.bridgefightplugin.queue.*;
import me.molfordan.bridgefightplugin.queue.QueueCommand;
import me.molfordan.bridgefightplugin.region.CommandRegionManager;
import me.molfordan.bridgefightplugin.region.RegionFlagListener;
import me.molfordan.bridgefightplugin.restore.DailyArenaRestorer;
import me.molfordan.bridgefightplugin.restore.PersistentRestoreManager;
import me.molfordan.bridgefightplugin.spawnitem.SpawnItem;
import me.molfordan.bridgefightplugin.task.CombatTagDisplayTask;
import me.molfordan.bridgefightplugin.utils.CommandRegister;
import me.molfordan.bridgefightplugin.utils.WorldGuardUtils;
import me.neznamy.tab.api.TabAPI;
//import me.molfordan.arenaAndFFAManager.utils.FlightManager;
import org.bukkit.Bukkit;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class BridgeFightPlugin extends JavaPlugin {
    public static BridgeFightPlugin plugin;

    private ArenaManager arenaManager;
    private ConfigManager configManager;
    private HotbarSorter hotbarSorter;

    private BridgeFightBanManager bridgeFightBanManager;
    private BridgeFightKitGUI bridgeFightKitGUI;
    private TeleportPendingManager teleportPendingManager;
    private EggBridgeManager eggBridgeManager;
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
    private BalanceManager balanceManager;
    private me.molfordan.bridgefightplugin.config.KnockbackConfig knockbackConfig;

    public me.molfordan.bridgefightplugin.config.KnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }
    private BedFightManager bedFightManager;
    private BedFightArenaManager bedFightArenaManager;
    private BedFightScoreboard bedFightScoreboard;
    private BedFightKitManager bedFightKitManager;
    private BedFightHotbarDataManager bedFightHotbarDataManager;
    private BedFightHotbarSessionManager bedFightHotbarSessionManager;
    private BedFightListener bedFightListener;
    private QueueGUI queueGUI;
    private MatchmakingService matchmakingService;
    private PartyManager partyManager;
    private DuelManager duelManager;
    private StatsGUI statsGUI;
    private BedFightHologramManager bedFightHologramManager;
    private PatchNotesManager patchNotesManager;
    private DuelScoreManager duelScoreManager;
    private FireballTracker fireballTracker;
    private TNTTracker tntTracker;
    private EnderPearlListener enderPearlListener;
    private me.molfordan.bridgefightplugin.cosmetics.CosmeticsManager cosmeticsManager;
    private me.molfordan.bridgefightplugin.cosmetics.gui.CosmeticsGUI cosmeticsGUI;
    private me.molfordan.bridgefightplugin.cosmetics.listener.CosmeticsListener cosmeticsListener;

    private me.molfordan.bridgefightplugin.cosmetics.crate.CrateManager crateManager;
    private me.molfordan.bridgefightplugin.kits.bridgefightkit.SwordChoiceManager swordChoiceManager;
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
        this.knockbackConfig = new me.molfordan.bridgefightplugin.config.KnockbackConfig(this);
        this.databaseManager = new DatabaseManager(this);
        this.statsManager = new StatsManager(this);
        this.balanceManager = new BalanceManager(this);
        this.bridgeFightConfig = new BridgeFightConfig(this);
        bridgeFightConfig.load();
        this.platformManager = new PlatformManager(this);
        ConfigurationSerialization.registerClass(Arena.class);
        ConfigurationSerialization.registerClass(SerializableBlockState.class);
        LadderRestorer ladderRestorer = new LadderRestorer();
        this.regionsConfig = new me.molfordan.bridgefightplugin.config.RegionsConfig(this);
        this.regionManager = new me.molfordan.bridgefightplugin.region.CommandRegionManager(this, regionsConfig);
        this.combatManager = new CombatManager(this);
        this.arenaManager = new ArenaManager(getDataFolder(), this, ladderRestorer);
        this.persistentRestoreManager = new PersistentRestoreManager(this);
        this.hotbarDataManager = new HotbarDataManager(this);
        this.kitManager = new KitManager(hotbarDataManager);
        this.hotbarSessionManager = new HotbarSessionManager(this, hotbarDataManager, kitManager);
        this.fireballTracker = new FireballTracker(this);
        this.tntTracker = new TNTTracker(this);
        this.cosmeticsManager = new me.molfordan.bridgefightplugin.cosmetics.CosmeticsManager(this);
        this.cosmeticsGUI = new me.molfordan.bridgefightplugin.cosmetics.gui.CosmeticsGUI(this, cosmeticsManager);
        this.crateManager = new me.molfordan.bridgefightplugin.cosmetics.crate.CrateManager(this);
        this.swordChoiceManager = new me.molfordan.bridgefightplugin.kits.bridgefightkit.SwordChoiceManager(this);
        this.deathMessageManager = new DeathMessageManager(this,combatManager, arenaManager, hotbarDataManager, statsManager, fireballTracker);
        this.deathMessageManager.setCosmeticsManager(cosmeticsManager);
        this.hotbarSorter = new HotbarSorter(hotbarDataManager);
        this.bridgeFightBanManager = new BridgeFightBanManager(getDataFolder());
        this.placeholderLeaderboardExpansion = new LeaderboardPlaceholderExpansion(this);
        this.guiLeaderboardBridgeFight = new GUILeaderboardBridgeFight(this);
        this.guiLeaderboardBuildFFA = new GUILeaderboardBuildFFA(this);
        this.guiLeaderboardMain = new GUILeaderboardMain(this);
        this.spawnItem = new SpawnItem(this);
        this.frozenManager = new FrozenManager(this);
        this.reportManager = new ReportManager(this, getDataFolder());
        this.bedFightArenaManager = new BedFightArenaManager(this);
        this.bedFightArenaManager.loadArenas();
        this.bedFightManager = new BedFightManager(this);
        this.bedFightHologramManager = new BedFightHologramManager(this);
        this.bedFightScoreboard = new BedFightScoreboard(this, bedFightManager);
        this.bedFightKitManager = new BedFightKitManager(this);
        this.bedFightHotbarDataManager = new BedFightHotbarDataManager(this);
        this.bedFightHotbarSessionManager = new BedFightHotbarSessionManager(this, bedFightHotbarDataManager, kitManager);
        this.queueGUI = new QueueGUI(this);
        this.statsGUI = new StatsGUI(this);
        this.matchmakingService = new MatchmakingService(this);
        this.partyManager = new PartyManager();
        this.duelManager = new DuelManager(this);
        this.duelScoreManager = new DuelScoreManager(this);
        this.teleportPendingManager = new TeleportPendingManager();
        this.eggBridgeManager = new EggBridgeManager();
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
        debug("[ArenaManager] Arenas loaded successfully.");
        this.dailyArenaRestorer = new DailyArenaRestorer(this, arenaManager);
        this.backupManager = new BackupManager(this);
        this.autoRestartManager = new AutoRestartManager(this, configManager);
        this.patchNotesManager = new PatchNotesManager(this);
        new CombatTagDisplayTask(combatManager).runTaskTimer(this, 0L, 1L);
        getCommand("arenamap").setExecutor(new ArenaCommand(arenaManager, configManager, this));
        getCommand("arenabypass").setExecutor(new ArenaBypassCommand(arenaManager));
        getCommand("commandbypass").setExecutor(new BypassCommandsCommand(combatManager));
        getCommand("arenaconfig").setExecutor(new ConfigReloadCommand(configManager));
        getCommand("configreload").setExecutor(new ConfigReloadCommand(configManager));
        getCommand("patchnotes").setExecutor(new PatchNotesCommand(this));
        me.molfordan.bridgefightplugin.cosmetics.CosmeticsCommand cosmeticsCmd = new me.molfordan.bridgefightplugin.cosmetics.CosmeticsCommand(cosmeticsGUI, cosmeticsManager);
        getCommand("cosmetics").setExecutor(cosmeticsCmd);
        getCommand("cosmetics").setTabCompleter(cosmeticsCmd);
        getCommand("setupcrate").setExecutor(new me.molfordan.bridgefightplugin.cosmetics.crate.SetupCrateCommand());
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("setbalance").setExecutor(new SetBalanceCommand(this));
        getCommand("setlobby").setExecutor(new SetLobbyCommand(configManager));
        getCommand("build").setExecutor(new BuildModeCommand(configManager));
        getCommand("buildffa").setExecutor(new BuildFFACommand(configManager));
        getCommand("bedfight").setExecutor(new BedFightCommand(this));
        getCommand("kiteditor").setExecutor(new KitEditorCommand());
        getCommand("forfeit").setExecutor(new ForfeitCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));
        getCommand("spec").setExecutor(new SpecCommand(this));
        getCommand("queue").setExecutor(new me.molfordan.bridgefightplugin.queue.QueueCommand(this));
        StatsCommand statsCommand = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCommand);
        getCommand("stats").setTabCompleter(statsCommand);
        getCommand("bfparty").setExecutor(new PartyCommand(this));
        getCommand("bfparty").setTabCompleter(new PartyTabCompleter());
        getCommand("duel").setExecutor(new DuelCommand(this));
        
        // Create LobbyListener instance to be used by both event registration and BridgeFightCommand
        LobbyListener lobbyListener = new LobbyListener(configManager, this, kitManager);
        
        getCommand("bridgefight").setExecutor(new BridgeFightCommand(configManager, this, lobbyListener));
        getCommand("loadworld").setExecutor(new LoadWorldCommand(configManager, this));
        getCommand("spawn").setExecutor(new LobbyCommand(configManager, teleportPendingManager, this));
        getCommand("setpos1").setExecutor(new SetPlatformPosCommand(this, platformManager));
        getCommand("setpos2").setExecutor(new SetPlatformPosCommand(this, platformManager));
        getCommand("guistats").setExecutor(new GUIStatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("guileaderboard").setExecutor(new GUILeaderboardCommand(this));
        RegionCommand rc = new RegionCommand(regionManager);
        getCommand("rc").setExecutor(rc);
        getCommand("rc").setTabCompleter(rc);
        getCommand("platformwand").setExecutor(new PlatformWandCommand());
        getCommand("platform").setExecutor(new me.molfordan.bridgefightplugin.commands.bridgefight.PlatformInfoCommand(this));
        
        getCommand("hotbarmanager").setExecutor(
                new HotbarManagerCommand(hotbarSessionManager)
        );
        new me.molfordan.bridgefightplugin.task.RegionTriggerTask(regionManager).runTaskTimer(this, 0L, 1L);
        int maxPlatforms = 10; // change this to any number
        for (int i = 1; i <= maxPlatforms; i++) {
            // plat commands
            CommandRegister.registerCommand(new PlatformCommand("plat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new PlatformCommand("bigplat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new PlatformCommand("boxingplat" + i, this, kitManager, platformManager));
            // spawn setter commands
            CommandRegister.registerCommand(new PlatformCommand("setplat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new PlatformCommand("setbigplat" + i, this, kitManager, platformManager));
            CommandRegister.registerCommand(new PlatformCommand("setboxingplat" + i, this, kitManager, platformManager));
        }
        getCommand("bridgeban").setExecutor(new BridgeBanCommand(bridgeFightBanManager, this));
        getCommand("bridgeunban").setExecutor(new BridgeUnbanCommand(bridgeFightBanManager));
        getCommand("playerhistory").setExecutor(new PlayerHistoryCommand(this));
        getCommand("freeze").setExecutor(new FrozenCommand(this));
        getCommand("unfreeze").setExecutor(new UnfrozenCommand(this));
        getCommand("togglebridgeegg").setExecutor(new ToggleBridgeEggCommand(this));
        getCommand("databasebackup").setExecutor(new me.molfordan.bridgefightplugin.commands.admin.DatabaseBackupCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("reports").setExecutor(new ReportsCommand(this));
        getCommand("statsreset").setExecutor(new StatsResetCommand(this));
        getCommand("statsmigrate").setExecutor(new StatsMigrateCommand(this));
        getCommand("hbmresetall").setExecutor(new HBMResetAllCommand(this));
        getCommand("privateworld").setExecutor(new PrivateWorldCommand(this));
        getCommand("setstats").setExecutor(new SetCommand(this));
        getCommand("playagain").setExecutor(new me.molfordan.bridgefightplugin.commands.common.PlayAgainCommand(this));
        getCommand("bfknockback").setExecutor(new BFKnockbackCommand(this));
        getCommand("queue").setExecutor(new QueueCommand(this));
        getCommand("kit").setExecutor(new BridgeFightKitCommand(this));
        getCommand("debugging").setExecutor(new GUICustomKit(this));
        getCommand("givepots").setExecutor(new GivePotsCommand(this));
        getCommand("rematch").setExecutor(new RematchCommand(this));

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
        getServer().getPluginManager().registerEvents(new ItemDropBlockerListener(arenaManager, this), this);
        getServer().getPluginManager().registerEvents(lobbyListener, this);
        getServer().getPluginManager().registerEvents(new GlobalListener(statsManager, this, arenaManager), this);
        getServer().getPluginManager().registerEvents(new BuildFFAListener(configManager, kitManager), this);
        getServer().getPluginManager().registerEvents(new BridgeFightListener(platformManager, configManager, this), this);
        this.bedFightListener = new BedFightListener(this, bedFightManager);
        getServer().getPluginManager().registerEvents(this.bedFightListener, this);
        getServer().getPluginManager().registerEvents(new me.molfordan.bridgefightplugin.listener.BedFightSetupListener(this, bedFightArenaManager), this);
        getServer().getPluginManager().registerEvents(new BedFightHotbarListener(this, bedFightHotbarSessionManager), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
        getServer().getPluginManager().registerEvents(new PartyItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PartyQueueGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PartyListGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PartyListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayAgainListener(this), this);
        getServer().getPluginManager().registerEvents(new RematchItemListener(this), this);
        getServer().getPluginManager().registerEvents(new QueueListener(this), this);
        getServer().getPluginManager().registerEvents(new me.molfordan.bridgefightplugin.listener.QueueQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new BedfightKnockback(), this);
        getServer().getPluginManager().registerEvents(new HotbarListener(this, hotbarSessionManager, kitManager), this);
        getConfig().addDefault("debug", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(new ItemReceiveListener(buildFFAWorld, hotbarSorter), this);
        getServer().getPluginManager().registerEvents(new LeaderboardGUIListener(this), this);
        this.enderPearlListener = new EnderPearlListener(this);
        getServer().getPluginManager().registerEvents(this.enderPearlListener, this);
        getServer().getPluginManager().registerEvents(new PrivateWorldListener(this), this);
        getServer().getPluginManager().registerEvents(this.frozenManager, this);
        getServer().getPluginManager().registerEvents(new TeleportCancelListener(this.teleportPendingManager), this);
        getServer().getPluginManager().registerEvents(new StatsGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EggBridgeListener(), this);
        getServer().getPluginManager().registerEvents(
                new BridgeFightBanListener(bridgeFightBanManager), this
        );
        getServer().getPluginManager().registerEvents(new RegionListener(regionManager), this);
        getServer().getPluginManager().registerEvents(new PlatformWandListener(platformManager), this);
        getServer().getPluginManager().registerEvents(new RegionFlagListener(regionManager), this);
        getServer().getPluginManager().registerEvents(new InvisPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MasterPickupDebugger(), this);
        getServer().getPluginManager().registerEvents(new SelectKitListener(this), this);
        getServer().getPluginManager().registerEvents(new EditKitListener(this), this);
        getServer().getPluginManager().registerEvents(new FireballListener(this.configManager, fireballTracker), this);
        getServer().getPluginManager().registerEvents(new ExplosiveListener(), this);
        getServer().getPluginManager().registerEvents(new TNTListener(fireballTracker, tntTracker), this);
        getServer().getPluginManager().registerEvents(new PatchNotesListener(this), this);
        this.cosmeticsListener = new me.molfordan.bridgefightplugin.cosmetics.listener.CosmeticsListener(this, cosmeticsManager);
        getServer().getPluginManager().registerEvents(this.cosmeticsListener, this);
        getServer().getPluginManager().registerEvents(new me.molfordan.bridgefightplugin.cosmetics.gui.CosmeticsGUIListener(this, cosmeticsGUI, cosmeticsManager), this);
        getServer().getPluginManager().registerEvents(new me.molfordan.bridgefightplugin.cosmetics.crate.CrateListener(crateManager), this);
        getServer().getPluginManager().registerEvents(new me.molfordan.bridgefightplugin.kits.bridgefightkit.SwordChoiceListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorRemovalListener(this, arenaManager), this);
        getServer().getPluginManager().registerEvents(this.deathMessageManager, this);
        getServer().getPluginManager().registerEvents(new BalanceListener(this), this);

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
            new me.molfordan.bridgefightplugin.placeholder.ArenaPlaceholderExpansion(this, deathMessageManager).register();
            getLogger().info("ArenaPlaceholderExpansion registered with PlaceholderAPI.");
            new me.molfordan.bridgefightplugin.placeholder.LeaderboardPlaceholderExpansion(this).register();
            getLogger().info("LeaderboardPlaceholderExpansion registered with PlaceholderAPI.");
            new me.molfordan.bridgefightplugin.placeholder.WorldCountPlaceholderExpansion(this).register();
            getLogger().info("WorldCountPlaceholderExpansion registered with PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found - arena placeholders not registered.");
        }

        // Start auto-restart system
        autoRestartManager.start();

        getLogger().info("BridgeFightPlugin has been enabled.");

        // Reset all daily streaks on server startup
        if (statsManager != null) {
            statsManager.resetAllDailyStreaks();
        }


    }
    @Override
    public void onDisable() {
        getLogger().info(plugin.getName() + " has been disabled.");

        // Clean up WorldGuard configuration directories
        File wgWorldsDir = new File(getServer().getWorldContainer(), "plugins/WorldGuard/worlds/");
        if (wgWorldsDir.exists() && wgWorldsDir.isDirectory()) {
            File[] files = wgWorldsDir.listFiles((dir, name) -> name.startsWith("bf_"));
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                        getLogger().info("Removed WorldGuard config directory on disable: " + file.getName());
                    }
                }
            }
        }

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

        if (matchmakingService != null) {
            matchmakingService.stop();
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

        if (tntTracker != null) {
            tntTracker.cleanup();
        }

        if (crateManager != null) {
            crateManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    private boolean deleteDirectory(File dir) {
        if (!dir.exists()) return true;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!deleteDirectory(file)) {
                    getLogger().warning("Failed to delete: " + file.getAbsolutePath());
                    return false;
                }
            }
        }
        
        try {
            return java.nio.file.Files.deleteIfExists(dir.toPath());
        } catch (IOException e) {
            getLogger().warning("Failed to delete directory: " + dir.getAbsolutePath() + " - " + e.getMessage());
            return false;
        }
    }



    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
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
    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public PersistentRestoreManager getPersistentRestoreManager() {
        return persistentRestoreManager;
    }

    public FileConfiguration getConfiguration() {
        return configuration;
    }

    public static BridgeFightPlugin getPlugin(){
        return plugin;
    }

    public static BridgeFightPlugin getInstance(){
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

    public BedFightManager getBedFightManager() {
        return bedFightManager;
    }

    public BedFightHologramManager getBedFightHologramManager() {
        return bedFightHologramManager;
    }

    public BedFightScoreboard getBedFightScoreboard() {
        return bedFightScoreboard;
    }

    public BedFightKitManager getBedFightKitManager() {
        return bedFightKitManager;
    }

    public BedFightArenaManager getBedFightArenaManager() {
        return bedFightArenaManager;
    }

    public BedFightListener getBedFightListener() {
        return bedFightListener;
    }

    public BedFightHotbarDataManager getBedFightHotbarDataManager() {
        return bedFightHotbarDataManager;
    }

    public BedFightHotbarSessionManager getBedFightHotbarSessionManager() {
        return bedFightHotbarSessionManager;
    }

    public QueueGUI getQueueGUI() {
        return queueGUI;
    }

    public StatsGUI getStatsGUI() {
        return statsGUI;
    }

    public MatchmakingService getMatchmakingService() {
        return matchmakingService;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public DuelScoreManager getDuelScoreManager() {
        return duelScoreManager;
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

    public TNTTracker getTNTTracker() {
        return tntTracker;
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
    public BalanceManager getBalanceManager() {
        return balanceManager;
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

    public EggBridgeManager getEggBridgeManager() {
        return eggBridgeManager;
    }

    public EnderPearlListener getEnderPearlListener() {
        return enderPearlListener;
    }

    public PatchNotesManager getPatchNotesManager() {
        return patchNotesManager;
    }

    public me.molfordan.bridgefightplugin.cosmetics.listener.CosmeticsListener getCosmeticsListener() {
        return cosmeticsListener;
    }

    public me.molfordan.bridgefightplugin.cosmetics.gui.CosmeticsGUI getCosmeticsGUI() {
        return cosmeticsGUI;
    }

    public me.molfordan.bridgefightplugin.cosmetics.crate.CrateManager getCrateManager() {
        return crateManager;
    }

    public me.molfordan.bridgefightplugin.kits.bridgefightkit.SwordChoiceManager getSwordChoiceManager() {
        return swordChoiceManager;
    }
}
