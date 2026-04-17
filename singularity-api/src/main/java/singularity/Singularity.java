package singularity;

import ch.qos.logback.classic.LoggerContext;
import gg.drak.thebase.async.AsyncUtils;
import gg.drak.thebase.objects.SingleSet;
import gg.drak.thebase.objects.handling.derived.PluginEventable;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.runners.PlayerSaver;
import singularity.data.update.defaults.DefaultUpdaters;
import singularity.data.uuid.UuidInfo;
import singularity.data.uuid.UuidManager;
import singularity.database.CoreDBOperator;
import singularity.database.servers.SavedServer;
import singularity.interfaces.*;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;
import singularity.interfaces.audiences.real.RealPlayer;
import singularity.logging.CosmicLogHandler;
import singularity.logging.CosmicLogbackAppender;
import singularity.logging.LogCollector;
import singularity.messages.ProxyMessenger;
import singularity.messages.proxied.ProxiedMessageManager;
import singularity.modules.CosmicModule;
import singularity.modules.ModuleManager;
import singularity.modules.ModuleUtils;
import singularity.timers.TPTicketFlusher;
import singularity.scheduler.BaseRunnable;
import singularity.scheduler.ModuleTaskManager;
import singularity.timers.TPTicketPuller;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Core entry point for the Singularity cross-platform framework.
 *
 * <p>This generic class wires together the platform-specific implementations of the
 * command sender ({@code C}), player ({@code P}), platform plugin ({@code S}),
 * user manager ({@code U}), and messenger ({@code M}) into a single cohesive API
 * accessible from any module or utility class via {@link #getInstance()}.
 *
 * <p>Responsibilities include:
 * <ul>
 *   <li>Bootstrapping the database, module system, and scheduler.</li>
 *   <li>Configuring the logging pipeline (java.util.logging / SLF4J/Logback).</li>
 *   <li>Providing static accessor helpers that delegate to the live instance.</li>
 * </ul>
 *
 * @param <C> the platform's base command-sender type
 * @param <P> the platform's player type, which extends {@code C}
 * @param <S> the platform plugin implementation that extends {@link ISingularityExtension}
 * @param <U> the user manager implementation
 * @param <M> the messenger implementation
 */
public class Singularity<C, P extends C, S extends ISingularityExtension, U extends IUserManager<C, P>, M extends IMessenger> extends PluginEventable {

    /**
     * A repeating scheduler task that drains the {@link #getCachedCommands()} queue
     * each tick, applies placeholder replacements, and dispatches each command according
     * to the prefix-based {@link RunType} rules.
     */
    public static class CommandRunner extends BaseRunnable {

        /**
         * Creates a {@code CommandRunner} that ticks every game tick (period = 1)
         * with no initial delay.
         */
        public CommandRunner() {
            super(0, 1);
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            List<Integer> toRemove = new ArrayList<>();

            for (int i : getCachedCommands().keySet()) {
                toRemove.add(i);

                SingleSet<String, CosmicSender> set = getCachedCommands().get(i);
                String command = set.getKey();
                CosmicSender user = set.getValue();
                command = ModuleUtils.replacePlaceholders(user, command);

                RunType runType = RunType.CONSOLE_COMMAND;
                if (command.startsWith("@ ")) {
                    runType = RunType.NORMAL_COMMAND;
                    command = command.substring(2);
                } else if (command.startsWith("? ")) {
                    runType = RunType.CONSOLE_COMMAND;
                    command = command.substring(3);
                } else if (command.startsWith("! ")) {
                    runType = RunType.OPERATOR_COMMAND;
                    command = command.substring(2);
                } else if (command.startsWith("# ")) {
                    runType = RunType.NORMAL_CHAT;
                    command = command.substring(2);
                }

                switch (runType) {
                    case CONSOLE_COMMAND:
                        getConsole().runCommand(command);
                        break;
                    case NORMAL_COMMAND:
                        if (user.isConsole()) getConsole().runCommand(command);
                        else getPlayerFromUuid(user.getUuid()).runCommand(command);
                        break;
                    case OPERATOR_COMMAND:
                        MessageUtils.logWarning("Operator command running is not yet supported.");
//                        OperatorUser operatorUser = new OperatorUser(user);
//                        ModuleUtils.runAs(operatorUser, command);
                        break;
                    case NORMAL_CHAT:
                        if (user.isConsole()) return;
                        getPlayerFromUuid(user.getUuid()).chatAs(command);
                        break;
                }
            }

            for (int i : toRemove) {
                removeCachedCommand(i);
            }
        }
    }

    /**
     * Describes how a cached command string should be dispatched.
     *
     * <p>The prefix character that precedes the command text determines the run type:
     * <ul>
     *   <li>{@code @ } — run as the player ({@link #NORMAL_COMMAND})</li>
     *   <li>{@code ? } — run as console ({@link #CONSOLE_COMMAND})</li>
     *   <li>{@code ! } — run with operator privileges ({@link #OPERATOR_COMMAND})</li>
     *   <li>{@code # } — send as chat ({@link #NORMAL_CHAT})</li>
     *   <li>no prefix  — defaults to {@link #CONSOLE_COMMAND}</li>
     * </ul>
     */
    public enum RunType {
        /** The command is dispatched as if the associated player typed it. */
        NORMAL_COMMAND,
        /** The text is sent as a chat message by the associated player. */
        NORMAL_CHAT,

        /**
         * The command is run with operator-level permissions.
         * <em>Not yet fully implemented.</em>
         */
        OPERATOR_COMMAND,

        /** The command is dispatched through the server console. */
        CONSOLE_COMMAND,
        ;
    }

    /** The singleton {@link CommandRunner} task that processes the cached-command queue. */
    @Getter @Setter
    private static CommandRunner commandRunner;

    /**
     * Ordered map of pending commands awaiting dispatch.  Keys are sequential integer
     * IDs; values pair the raw command string with the {@link CosmicSender} on whose
     * behalf the command will be run.
     */
    @Getter @Setter
    private static LinkedHashMap<Integer, SingleSet<String, CosmicSender>> cachedCommands = new LinkedHashMap<>();

    /**
     * Enqueues a command to be dispatched on the next tick of {@link CommandRunner}.
     *
     * @param command the raw command string, optionally prefixed with a {@link RunType} marker
     * @param user    the sender context used for placeholder replacement and command dispatch
     */
    public static void addCachedCommand(String command, CosmicSender user) {
        int lastId = getCachedCommands().keySet().stream().max(Integer::compareTo).orElse(0);
        getCachedCommands().put(lastId + 1, new SingleSet<>(command, user));
    }

    /**
     * Removes the cached command entry with the given ID from the queue.
     *
     * @param id the sequential ID of the command to remove
     */
    public static void removeCachedCommand(int id) {
        cachedCommands.remove(id);
    }

    /** The directory from which PF4J module JARs are loaded. */
    @Getter
    private static File moduleFolder;

    /** The directory used to persist module-specific resource files. */
    @Getter
    private static File moduleSaveFolder;

    /** The directory that stores command configuration files. */
    @Getter
    private static File mainCommandsFolder;

    /** The relative path component appended to the data folder to derive {@link #mainCommandsFolder}. */
    @Getter
    private static final String commandsFolderChild = "commands" + File.separator;

    /** The active singleton instance of Singularity for the running platform. */
    @Getter
    private static Singularity<?, ?, ?, ?, ?> instance;

    /** The platform-specific plugin/extension that backs this Singularity instance. */
    @Getter
    private final S platform;

    /** The user-manager implementation responsible for player lifecycle management. */
    @Getter
    private final U userManager;

    /** The messenger implementation used to send formatted messages to senders. */
    @Getter
    private final M messenger;

    /** Wraps the platform console so it can be used as a generic {@link CosmicSender}. */
    @Getter
    private final IConsoleHolder<C> consoleHolder;

    /** Provides access to online players in a platform-agnostic manner. */
    @Getter
    private final IPlayerInterface<P> playerInterface;

    /** The proxy-channel messenger used to communicate between backend and proxy servers. */
    @Getter @Setter
    private ProxyMessenger proxyMessenger;

    /**
     * The optional base {@link CosmicModule} that provides built-in module functionality.
     * May be {@code null} if no base module supplier was provided at construction time.
     */
    @Getter @Setter
    private static CosmicModule baseModule;

    /** The per-module task scheduler that tracks all {@link singularity.scheduler.ModuleRunnable} instances. */
    @Getter
    private static ModuleTaskManager moduleScheduler;

    /** Handler used to communicate with backend (non-proxy) servers, if applicable. */
    @Getter @Setter
    private static IBackendHandler backendHandler;

    /** {@code true} if this server is running behind a proxy (i.e., is a backend server). */
    @Getter @Setter
    private static boolean proxiedServer;

    /** {@code true} if this server instance is a proxy (Velocity or BungeeCord). */
    @Getter @Setter
    private static boolean proxy;

    /** The timer that periodically flushes pending {@link singularity.data.teleportation.TPTicket}s. */
    @Getter @Setter
    private static TPTicketFlusher tpTicketFlusher;

    /** The timer that pulls new {@link singularity.data.teleportation.TPTicket}s from the database. */
    @Getter @Setter
    private static TPTicketPuller tpTicketPuller;

    /**
     * {@code true} once the Singularity constructor has finished all synchronous
     * setup steps and the framework is operational.
     */
    @Getter @Setter
    private static boolean ready = false;

    /**
     * Returns the primary database operator used by Singularity.
     *
     * @return the main {@link CoreDBOperator}
     */
    public static CoreDBOperator getMainDatabase() {
        return GivenConfigs.getMainDatabase();
    }

    /**
     * Replaces the main database operator.
     *
     * @param mainDatabase the new {@link CoreDBOperator} to use
     */
    public static void setMainDatabase(CoreDBOperator mainDatabase) {
        GivenConfigs.setMainDatabase(mainDatabase);
    }

    /**
     * The plugin-messaging channel name used for cross-server API communication.
     */
    @Getter @Setter
    private static String apiChannel;

    /** The recurring task that persists online players to the database. */
    @Getter @Setter
    private static PlayerSaver playerSaver;

    /**
     * Atomically tracks whether the database has been initialised and is ready
     * to accept queries.
     */
    @Getter @Setter
    private static AtomicBoolean databaseReady;

    /**
     * Atomically tracks whether the platform plugin has finished its enable phase
     * and is accepting players.
     */
    @Getter @Setter
    private static AtomicBoolean platformEnabled;

    /**
     * Full constructor that bootstraps the entire Singularity framework.
     *
     * <p>The constructor is synchronous for field initialisation but offloads
     * database setup and module loading to async threads so that the platform
     * plugin is not blocked during startup.
     *
     * @param identifier      the unique plugin identifier used by the event system
     * @param platform        the platform-specific plugin implementation
     * @param userManager     the user/player manager for this platform
     * @param messenger       the messenger used to send formatted text to senders
     * @param consoleHolder   wraps the platform console sender
     * @param playerInterface provides access to online players
     * @param baseModuleGetter optional supplier for a built-in base module; may be {@code null}
     * @param apiChannel      the plugin-messaging channel name for cross-server communication
     */
    public Singularity(String identifier, S platform, U userManager, M messenger, IConsoleHolder<C> consoleHolder, IPlayerInterface<P> playerInterface, Supplier<CosmicModule> baseModuleGetter, String apiChannel) {
        super(identifier);
        instance = this;
        databaseReady = new AtomicBoolean(false);
        platformEnabled = new AtomicBoolean(false);

        // Field Stuff
        this.platform = platform;
        this.userManager = userManager;
        this.messenger = messenger;
        this.consoleHolder = consoleHolder;
        this.playerInterface = playerInterface;

        // Console Stuff
        setupLogger();

        // Set up the api channel.
        setApiChannel(apiChannel);

//        setProxiedServer(platform.getServerType().equals(IStreamline.ServerType.BACKEND));
        setProxy(platform.getServerType().equals(ISingularityExtension.ServerType.PROXY));

        moduleFolder = new File(getDataFolder(), "modules" + File.separator);
        moduleSaveFolder = new File(getDataFolder(), "module-resources" + File.separator);
        mainCommandsFolder = new File(getDataFolder(), getCommandsFolderChild());
        moduleFolder.mkdirs();
        moduleSaveFolder.mkdirs();
        mainCommandsFolder.mkdirs();

        // Must go here.
        GivenConfigs.init();

        // Must go here.
        AsyncUtils.executeAsync(this::initDatabase);

        AsyncUtils.executeAsync(() -> initModules(baseModuleGetter));

        ProxiedMessageManager.init();

        setCommandRunner(new CommandRunner());

        MessageUtils.init();

        DefaultUpdaters.init();

        playerSaver = new PlayerSaver();

        LogCollector.init();

        tpTicketFlusher = new TPTicketFlusher();
        tpTicketPuller = new TPTicketPuller();

        setReady(true);
    }

    /**
     * Convenience constructor that delegates to
     * {@link #Singularity(String, ISingularityExtension, IUserManager, IMessenger,
     * IConsoleHolder, IPlayerInterface, Supplier, String)} with no base-module supplier.
     *
     * @param identifier      the unique plugin identifier
     * @param platform        the platform-specific plugin implementation
     * @param userManager     the user/player manager for this platform
     * @param messenger       the messenger used to send formatted text to senders
     * @param consoleHolder   wraps the platform console sender
     * @param playerInterface provides access to online players
     * @param apiChannel      the plugin-messaging channel name
     */
    public Singularity(String identifier, S platform, U userManager, M messenger, IConsoleHolder<C> consoleHolder, IPlayerInterface<P> playerInterface, String apiChannel) {
        this(identifier, platform, userManager, messenger, consoleHolder, playerInterface, null, apiChannel);
    }

    /**
     * Returns {@code true} if the platform has completed its enable phase.
     *
     * @return {@code true} when the platform is enabled and ready to serve players
     */
    public static boolean isPlatformEnabled() {
        return platformEnabled != null && platformEnabled.get();
    }

    /**
     * Updates the platform-enabled flag.  Initialises the underlying
     * {@link AtomicBoolean} if it has not yet been set.
     *
     * @param platformEnabled {@code true} to mark the platform as enabled
     */
    public static void platformEnabled(boolean platformEnabled) {
        if (Singularity.platformEnabled == null) Singularity.platformEnabled = new AtomicBoolean(false);
        Singularity.platformEnabled.set(platformEnabled);
    }

    /**
     * Performs asynchronous database initialisation: waits for the database to become
     * available, ensures usability, pre-loads all UUID-to-name mappings, and loads
     * the console user.  Sets {@link #databaseReady} to {@code true} when complete.
     */
    public void initDatabase() {
        GivenConfigs.awaitDatabaseReady();

        try {
            getMainDatabase().ensureUsable();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ConcurrentSkipListSet<UuidInfo> uuidInfos = getMainDatabase().pullAllUuidInfo().join();
            UuidManager.registerAll(uuidInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        UserUtils.loadConsole(); // Load the console user // done with database.

        databaseReady.set(true);
    }

    /**
     * Waits for the database to be ready, initialises the {@link ModuleTaskManager},
     * optionally loads the base module, migrates non-JAR files from the module folder,
     * and then registers and starts all external PF4J modules.
     *
     * @param baseModuleGetter an optional supplier for the built-in base module;
     *                         may be {@code null} if no base module is required
     */
    public void initModules(Supplier<CosmicModule> baseModuleGetter) {
        awaitDatabaseReady(); // Ensure database is ready before loading modules.

        moduleScheduler = new ModuleTaskManager(); // Init scheduler first.

        if (baseModuleGetter != null) setBaseModule(baseModuleGetter.get());

        getFiles(getModuleFolder(), file -> {
            if (file.isDirectory()) return true;
            return ! file.getName().endsWith(".jar");
        }).forEach((s, file) -> {
            try {
                Files.move(file.toPath(), Path.of(file.toPath().toString()
                        .replace(getModuleFolder().toPath().toString(), getModuleSaveFolder().toPath().toString())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//        baseModule = new BaseModule();
//        ModuleManager.registerModule(getBaseModule());


        try {
            awaitPlatformEnabled();

            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
//            setServerPusher(new ServerPusher());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Blocks the calling thread until {@link #isPlatformEnabled()} returns {@code true}.
     * Uses {@link Thread#onSpinWait()} to hint to the JVM that this is a hot spin-loop.
     */
    public void awaitPlatformEnabled() {
        if (isPlatformEnabled()) return;

        while (! isPlatformEnabled()) {
            Thread.onSpinWait();
        }
    }

    /**
     * Installs the Singularity logging pipeline.
     *
     * <p>Depending on what logging APIs the platform exposes, this method will:
     * <ul>
     *   <li>Add a {@link CosmicLogHandler} to the platform's {@link java.util.logging.Logger}.</li>
     *   <li>Replace all existing Logback appenders with a {@link CosmicLogbackAppender}
     *       when an SLF4J/Logback implementation is present.</li>
     * </ul>
     */
    public void setupLogger() {
        try {
            if (getPlatform().hasLoggerLogger()) {
                java.util.logging.Logger rootLogger = getPlatform().getLoggerLogger();

                // Add the custom handler
                CosmicLogHandler handler = new CosmicLogHandler();
                rootLogger.addHandler(handler);
            }
            if (getPlatform().hasSLFLogger()) {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

                // Remove existing appenders
                rootLogger.detachAndStopAllAppenders();

                // Add custom appender
                CosmicLogbackAppender appender = new CosmicLogbackAppender();
                appender.setContext(loggerContext);
                appender.setName("CosmicLogbackAppender");
                appender.start();
                rootLogger.addAppender(appender);
            }
        } catch (Exception e) {
            // nothing
        }
    }

    /**
     * Checks if the database is ready to be used.
     * @return true if the database is ready, false otherwise.
     */
    public static boolean isDatabaseReady() {
        return databaseReady.get();
    }

    /**
     * Blocks the thread calling this method until the database is ready.
     */
    public static void awaitDatabaseReady() {
        if (isDatabaseReady()) return;

        // Spin wait until the database is ready.
        while (! isDatabaseReady()) {
            Thread.onSpinWait();
        }
    }

    /**
     * Returns the UUID string that uniquely identifies this server instance, as stored
     * in the server configuration.
     *
     * @return the server UUID string
     */
    public static String getServerUuid() {
        return GivenConfigs.getServer().getUuid();
    }

    /**
     * Returns the human-readable name of this server instance as stored in the
     * server configuration.
     *
     * @return the server name
     */
    public static String getServerName() {
        return GivenConfigs.getServer().getName();
    }

    /**
     * Returns the {@link SavedServer} object representing this server as persisted
     * in the database configuration.
     *
     * @return the current server's {@link SavedServer} record
     */
    public static SavedServer getServer() {
        return GivenConfigs.getServerConfig().getServer();
    }

    /**
     * Lists files in the given folder that match the supplied predicate.
     *
     * @param folder        the directory to scan; returns an empty map if not a directory
     * @param filePredicate a predicate that returns {@code true} for files to include
     * @return a sorted map from file name to {@link File} for all matched entries
     */
    public ConcurrentSkipListMap<String, File> getFiles(File folder, Predicate<File> filePredicate) {
        ConcurrentSkipListMap<String, File> r = new ConcurrentSkipListMap<>();
        if (! folder.isDirectory()) return r;
        File[] files = folder.listFiles();
        if (files == null) return r;

        for (File file : files) {
            if (filePredicate.test(file)) r.put(file.getName(), file);
        }

        return r;
    }

    /**
     * Opens a resource from this class's class loader.
     *
     * @param filename the resource path relative to the class loader root
     * @return an {@link InputStream} for the resource, or {@code null} if not found
     */
    public InputStream getResourceAsStream(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    /**
     * Opens a resource from the platform plugin's main class loader.
     * This is typically needed to access resources bundled in the platform JAR rather
     * than the API JAR.
     *
     * @param filename the resource path relative to the platform plugin's class loader root
     * @return an {@link InputStream} for the resource, or {@code null} if not found
     */
    public InputStream getResourceAsStreamMain(String filename) {
        return getPlatform().getMainClassLoader().getResourceAsStream(filename);
    }

    /**
     * Returns the primary data folder for the Singularity plugin as reported by
     * the platform plugin implementation.
     *
     * @return the root data {@link File} directory
     */
    public static File getMainFolder() {
        return getInstance().getDataFolder();
    }

    /**
     * Returns the platform console wrapped as a {@link RealSender}.
     *
     * @param <C> the platform's command-sender type
     * @return the console {@link RealSender}
     */
    public static <C> RealSender<C> getConsole() {
        return (RealSender<C>) getInstance().getConsoleHolder().getRealConsole();
    }

    /**
     * Looks up an online player by their {@link UUID}.
     *
     * @param <P>  the platform's player type
     * @param uuid the player's unique identifier
     * @return the {@link RealPlayer} for the given UUID, or {@code null} if not online
     */
    public static <P> RealPlayer<P> getPlayer(UUID uuid) {
        return (RealPlayer<P>) getInstance().getPlayerInterface().getPlayer(uuid);
    }

    /**
     * Looks up an online player by their display/login name.
     *
     * @param <P>  the platform's player type
     * @param name the player's in-game name
     * @return the {@link RealPlayer} with the given name, or {@code null} if not online
     */
    public static <P> RealPlayer<P> getPlayer(String name) {
        return (RealPlayer<P>) getInstance().getPlayerInterface().getPlayer(name);
    }

    /**
     * Looks up an online player by their UUID represented as a {@link String}.
     *
     * @param <P>  the platform's player type
     * @param uuid the player's UUID string (parseable by {@link UUID#fromString(String)})
     * @return the {@link RealPlayer} for the given UUID string, or {@code null} if not online
     */
    public static <P> RealPlayer<P> getPlayerFromUuid(String uuid) {
        return (RealPlayer<P>) getInstance().getPlayerInterface().getPlayer(UUID.fromString(uuid));
    }

    /**
     * Sends a raw (possibly colour-coded) message to the server console.
     *
     * @param message the message to send; supports Streamline colour codes
     */
    public static void sendConsoleMessage(String message) {
        getInstance().getConsoleHolder().sendConsoleMessage(message);
    }

    /**
     * Checks whether the server is running in offline (cracked) mode, where player
     * UUIDs are not validated against Mojang's session servers.
     *
     * @return {@code true} if the server is in offline mode
     */
    public static boolean isOfflineMode() {
        return getInstance().getPlatform().isOfflineMode();
    }
}
