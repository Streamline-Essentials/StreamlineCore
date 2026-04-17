package net.streamline.platform;

import gg.drak.thebase.events.BaseEventHandler;
import host.plas.bou.BetterPlugin;
import host.plas.bou.libs.universalScheduler.UniversalScheduler;
import host.plas.bou.libs.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.apib.SLAPIB;
import net.streamline.base.runnables.PlayerChecker;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.handlers.BackendHandler;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.uuid.UuidInfo;
import singularity.data.uuid.UuidManager;
import singularity.events.CosmicEvent;
import singularity.events.server.ServerStopEvent;
import singularity.interfaces.IProperEvent;
import singularity.interfaces.ISingularityExtension;
import singularity.logging.CosmicLogHandler;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.TaskManager;
import singularity.utils.MessageUtils;
import singularity.utils.StorageUtils;
import singularity.utils.UserUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Abstract base plugin for the StreamlineCore Spigot platform implementation.
 *
 * <p>Extends BukkitOfUtils' {@code BetterPlugin} and implements
 * {@link ISingularityExtension} to integrate the cross-platform Singularity
 * abstraction layer with the Spigot/Bukkit server API.
 *
 * <p>Subclasses must implement the four lifecycle hooks:
 * {@link #load()}, {@link #enable()}, {@link #disable()}, and {@link #reload()}.
 */
public abstract class BasePlugin extends BetterPlugin implements ISingularityExtension {
    /**
     * The platform type constant for this implementation.
     * Always {@link singularity.interfaces.ISingularityExtension.PlatformType#SPIGOT}.
     */
    @Getter
    private final PlatformType platformType = PlatformType.SPIGOT;

    /**
     * The server type constant for this implementation.
     * Always {@link singularity.interfaces.ISingularityExtension.ServerType#BACKEND}.
     */
    @Getter
    private final ServerType serverType = ServerType.BACKEND;

    /**
     * The resource pack currently configured to be sent to players, if any.
     */
    @Getter @Setter
    private CosmicResourcePack resourcePack;

    /** The plugin version string read from {@code streamline.properties}. */
    @Getter
    private String version;

    /** The plugin data folder name read from {@code streamline.properties}. */
    @Getter
    private String folderName;

    /** The singleton instance of this plugin, set during {@link #onBaseConstruct()}. */
    @Getter
    private static BasePlugin instance;

    /**
     * The primary Streamline API instance parameterised for the Spigot platform,
     * created during {@link #onBaseEnabled()}.
     */
    @Getter
    private SLAPI<CommandSender, Player, BasePlugin, UserManager, Messenger> slapi;

    /** The Bukkit-specific Streamline API extension. */
    @Getter
    private SLAPIB slapiB;

    /**
     * Returns the Bukkit {@link Server} instance.
     * Named {@code getProxy()} to match the cross-platform convention used on
     * proxy platforms.
     *
     * @return the Bukkit server
     */
    public Server getProxy() {
        return getServer();
    }

    /** The platform-specific user manager. */
    @Getter
    private UserManager userManager;

    /** The platform-specific messenger used to send formatted messages. */
    @Getter
    private Messenger messenger;

    /** Holder that represents the server console as a {@link singularity.data.console.CosmicSender}. */
    @Getter
    private ConsoleHolder consoleHolder;

    /** Interface that wraps Bukkit player operations for the cross-platform API. */
    @Getter
    private PlayerInterface playerInterface;

    /**
     * The periodic task that ensures all online players have their
     * {@link singularity.data.players.CosmicPlayer} loaded.
     */
    @Getter @Setter
    private static PlayerChecker playerChecker;

    /**
     * The plugin-messaging listener registered on the incoming BungeeCord
     * channel for receiving proxy messages.
     */
    @Getter @Setter
    private static PlatformListener.ProxyMessagingListener proxyMessagingListener;

    /**
     * {@inheritDoc}
     *
     * <p>Sets the singleton instance, reads {@code streamline.properties}, migrates
     * legacy plugin data directories to the canonical folder name, and calls
     * {@link #load()}.
     */
    @Override
    public void onBaseConstruct() {
        instance = this;

        setupProperties();

        String parentPath = getDataFolder().getParent();
        if (parentPath != null) {
            File parentFile = new File(parentPath);
            File[] files = parentFile.listFiles((f) -> {
                if (!f.isDirectory()) return false;
                if (f.getName().equals("StreamlineAPI")) return true;
                if (f.getName().equals("StreamlineCore-Spigot")) return true;
                if (f.getName().equals("StreamlineCore-Bungee")) return true;
                if (f.getName().equals("StreamlineCore-Velocity")) return true;
                if (f.getName().equals("StreamlineCore-Fabric")) return true;
                if (f.getName().equals("StreamlineCore-Forge")) return true;
                if (f.getName().equals("streamlinecore")) return true;
                return false;
            });

            if (files != null) {
                Arrays.stream(files).forEach(file -> {
                    file.renameTo(new File(parentPath, this.folderName));
                });
            }
        }

        this.load();
    }

    /**
     * Reads {@code streamline.properties} and populates {@link #folderName} and
     * {@link #version} from the {@code name} and {@code version} keys respectively.
     * Does nothing if the properties file is empty or missing.
     */
    public void setupProperties() {
        ConcurrentSkipListMap<String, String> properties = StorageUtils.readProperties();
        if (properties.isEmpty()) return;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals("name")) {
                this.folderName = value;
            }
            if (key.equals("version")) {
                this.version = value;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wires up the Streamline platform: initialises {@link UserManager},
     * {@link Messenger}, {@link ConsoleHolder}, {@link PlayerInterface}, and
     * {@link SLAPI}; registers the proxy plugin-messaging channels; starts
     * {@link singularity.scheduler.TaskManager}; and delegates to {@link #enable()}.
     */
    @Override
    public void onBaseEnabled() {
        setupCommandMap();

        getLogger().addHandler(new CosmicLogHandler());

        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();
        slapi = new SLAPI<>(getFolderName(), this, getUserManager(), getMessenger(), getConsoleHolder(), getPlayerInterface(), BaseModule::new);
        SLAPI.setBackendHandler(new BackendHandler());
        slapiB = new SLAPIB(getSlapi(), this);

        getSlapi().setProxyMessenger(new ProxyPluginMessenger());

        TaskManager.init();

        proxyMessagingListener = new PlatformListener.ProxyMessagingListener();

        getProxy().getMessenger().registerOutgoingPluginChannel(this, SLAPI.getApiChannel());
        getProxy().getMessenger().registerIncomingPluginChannel(this, SLAPI.getApiChannel(), proxyMessagingListener);

        playerChecker = new PlayerChecker();

        this.enable();
        registerListener(new PlatformListener());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cancels scheduled tickets, syncs all user data to storage,
     * saves UUID cache entries, unregisters plugin-messaging channels,
     * delegates to {@link #disable()}, fires a {@link singularity.events.server.ServerStopEvent},
     * and stops {@link singularity.scheduler.TaskManager}.
     */
    @Override
    public void onBaseDisable() {
        Singularity.getTpTicketFlusher().cancel();
        Singularity.getTpTicketPuller().cancel();

        UserUtils.syncAllUsers();
        UuidManager.getUuids().forEach(UuidInfo::save);

        getProxy().getMessenger().unregisterOutgoingPluginChannel(this, SLAPI.getApiChannel());
        getProxy().getMessenger().unregisterIncomingPluginChannel(this, SLAPI.getApiChannel());
//        getProxy().getMessenger().unregisterIncomingPluginChannel(this, SLAPI.getApiChannel(), proxyMessagingListener);

        this.disable();
        fireStopEvent();

        TaskManager.stop();
    }

    /**
     * Fires a {@link singularity.events.server.ServerStopEvent} and, if the event
     * is not cancelled and is sendable, broadcasts its message to the console.
     */
    public void fireStopEvent() {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (!e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * Called after the Streamline platform is fully initialised (during
     * {@link #onBaseEnabled()}).  Subclasses should register game-specific
     * features, commands, and listeners here.
     */
    abstract public void enable();

    /**
     * Called before the plugin is unloaded (during {@link #onBaseDisable()}).
     * Subclasses should release any resources they acquired in {@link #enable()}.
     */
    abstract public void disable();

    /**
     * Called during early construction (during {@link #onBaseConstruct()})
     * before any Streamline services are initialised. Subclasses may perform
     * pre-init configuration here.
     */
    abstract public void load();

    /**
     * Called when the plugin is instructed to reload its configuration.
     * Subclasses should re-read any config files and apply changes.
     */
    abstract public void reload();

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();

        for (Player player : onlinePlayers()) {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) {
                CosmicPlayer cosmicPlayer = getUserManager().getOrCreatePlayer(player).orElse(null);
                if (cosmicPlayer == null) continue;
                players.add(cosmicPlayer);
            }
        }

        return players;
    }

    /** {@inheritDoc} */
    @Override
    public ProperCommand createCommand(CosmicCommand command) {
        return new ProperCommand(command);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxPlayers() {
        return getInstance().getProxy().getMaxPlayers();
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getCurrentName());
        });

        //        r.add(getUserManager().getConsole().latestName);

        return r;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOfflineMode() {
        return ! Bukkit.getOnlineMode();
    }

    /** {@inheritDoc} */
    @Override
    public long getConnectionThrottle() {
        return getInstance().getProxy().getConnectionThrottle();
    }

    /**
     * Returns a snapshot list of all currently online Bukkit {@link Player}s.
     *
     * @return a mutable {@link List} of online players
     */
    public static List<Player> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getOnlinePlayers());
    }

    /**
     * Returns the list of players on the named sub-server.
     *
     * <p>Currently returns an empty list; sub-server enumeration is not
     * implemented on the Spigot backend.
     *
     * @param serverName the name of the sub-server (unused)
     * @return an empty {@link List}
     */
    public static List<Player> playersOnServer(String serverName) {
        return new ArrayList<>(/*getInstance().getProxy().gets(serverName).getPlayers()*/);
    }

    /**
     * Retrieves an online {@link Player} by their UUID string.
     *
     * @param uuid the player's UUID as a string
     * @return the online {@link Player}, or {@code null} if not found
     */
    public static Player getPlayer(String uuid) {
        for (Player player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    /**
     * Looks up an online {@link Player} by their exact username.
     *
     * @param name the player's username
     * @return an {@link Optional} containing the player, or empty if not found
     */
    public static Optional<Player> getPlayerByName(String name) {
        return Optional.ofNullable(getInstance().getProxy().getPlayer(name));
    }

    /**
     * Retrieves an online {@link Player} by their exact username, or {@code null}.
     *
     * @param name the player's username
     * @return the online {@link Player}, or {@code null} if not found
     */
    public static @Nullable Player getPlayerExact(@NotNull String name) {
        if (getPlayerByName(name).isEmpty()) return null;
        return getPlayerByName(name).get();
    }

    /**
     * Returns a list containing the player whose username exactly matches
     * {@code name}, or an empty list if no such player is online.
     *
     * @param name the player's username to match exactly
     * @return a list of zero or one matching {@link Player}
     */
    public static @NotNull List<Player> matchPlayer(@NotNull String name) {
        Player player = getPlayerExact(name);
        if (player == null) return new ArrayList<>();
        return List.of(player);
    }

    /**
     * Retrieves an online {@link Player} by their {@link UUID}.
     *
     * @param id the player's {@link UUID}
     * @return the online {@link Player}, or {@code null} if not found
     */
    public static @Nullable Player getPlayer(@NotNull UUID id) {
        return getPlayer(id.toString());
    }

    /**
     * Retrieves an online {@link Player} whose name matches that of the given
     * {@link CommandSender}.
     *
     * @param sender the command sender whose name is used for the lookup
     * @return the matching online {@link Player}, or {@code null} if not found
     */
    public static Player getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getOnlineMode();
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        getInstance().getProxy().shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates all online players and sends {@code message} to each player
     * who has the given {@code permission}.
     *
     * @param message    the message to broadcast
     * @param permission the permission required to receive the message
     * @return the number of players who received the message
     */
    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (Player player : onlinePlayers()) {
            if (!player.hasPermission(permission)) continue;
            getMessenger().sendMessage(player, message);
            people++;
        }

        return people;
    }

    /** {@inheritDoc} */
    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin) != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unwraps the platform event from {@code event} and passes it to the
     * Bukkit plugin manager if it is a Bukkit {@link Event}.
     */
    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (!(event.getEvent() instanceof Event)) return;
        Event e = (Event) event.getEvent();
        getInstance().getProxy().getPluginManager().callEvent(e);
    }

    /** {@inheritDoc} */
    @Override
    public void fireEvent(CosmicEvent event) {
        fireEvent(event, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fires the event via {@link gg.drak.thebase.events.BaseEventHandler}. If an
     * exception is thrown (e.g. due to a thread mis-sync), falls back to
     * {@link #handleMisSync(CosmicEvent, boolean)}.
     */
    @Override
    public void fireEvent(CosmicEvent event, boolean async) {
        try {
            BaseEventHandler.fireEvent(event);
        } catch (Exception e) {
            handleMisSync(event, async);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Re-fires the event synchronously via
     * {@link gg.drak.thebase.events.BaseEventHandler} as a fallback when the
     * primary fire in {@link #fireEvent(CosmicEvent, boolean)} threw an exception.
     */
    @Override
    public void handleMisSync(CosmicEvent event, boolean async) {
        BaseEventHandler.fireEvent(event);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Currently returns an empty set; sub-server enumeration is not
     * available from the Spigot backend without a proxy.
     */
    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        //        for (String server : getInstance().getProxy().getServerNames()) {
        //            r.add(server);
        //        }

        return r;
    }

    /** {@inheritDoc} */
    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        Player p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    /** {@inheritDoc} */
    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, String uuid) {
        Player p = getPlayer(uuid);

        //        getMessenger().logInfo("Attempting to send a resource pack to a uuid of '" + whitelistedUuid + "'...");

        sendResourcePack(resourcePack, p);
    }

    /**
     * Sends the given {@link CosmicResourcePack} to a specific Bukkit
     * {@link Player}.
     *
     * <p>Uses the hash-based overload of {@link Player#setResourcePack} when
     * a non-empty hash is present, falling back to the URL-only variant.
     * Logs a warning and returns early if {@code player} is {@code null}.
     *
     * @param resourcePack the resource pack to send
     * @param player       the target player; may be {@code null}
     */
    public void sendResourcePack(CosmicResourcePack resourcePack, Player player) {
        if (player == null) {
            MessageUtils.logWarning("Tried to send a player a resource pack, but could not find their player!");
            return;
        }

        //        getMessenger().logInfo("Sending resource pack to '" + player.getName() + "'.");

        try {
            if (resourcePack.getHash().length > 0) {
//                    if (! resourcePack.getPrompt().isEmpty()) {
//                        player.setResourcePack(resourcePack.getUrl(), resourcePack.getHash(), resourcePack.getPrompt(), resourcePack.isForce());
//                        return;
//                    }
//                    player.setResourcePack(resourcePack.getUrl(), resourcePack.getHash(), resourcePack.isForce());
                player.setResourcePack(resourcePack.getUrl(), resourcePack.getHash());
                return;
            }
            player.setResourcePack(resourcePack.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }

    /**
     * Flag indicating that one or more commands were registered or unregistered
     * and the server command map needs to be synced with the client.
     */
    @Getter
    @Setter
    private static boolean commandsNeedToBeSynced = false;

    /**
     * The Bukkit {@link CommandMap} obtained via reflection, used to
     * programmatically register and unregister commands at runtime.
     */
    @Getter
    @Setter
    private static CommandMap commandMap;

    private static void setupCommandMap() {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register command(s) into the server command map.
     *
     * @param commands The command(s) to register
     */
    public static void registerCommands(ProperCommand... commands) {
        // Get the commandMap
        try {
            // Register all the commands into the map
            for (final ProperCommand command : commands) {
                commandMap.register(command.getParent().getBase(), command.getLabel(), command);

                try {
                    commandMap.register("streamlinecore", command);
                } catch (Throwable e) {
                    MessageUtils.logDebugWithInfo("Failed to register command: " + command.getLabel(), e);
                }
            }

            CompletableFuture.runAsync(BasePlugin::syncCommands);
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Unregister command(s) from the server command map.
     *
     * @param commands The command(s) to unregister
     */
    public static void unregisterCommands(String... commands) {
        // Get the commandMap
        try {
            // Register all the commands into the map
            for (final String command : commands) {
                Command com = commandMap.getCommand(command);
                if (com == null) {
                    MessageUtils.logDebug("Tried to unregister a command that does not exist: " + command);
                    continue;
                }

                try {
                    com.unregister(commandMap);
                } catch (Throwable e) {
                    MessageUtils.logDebugWithInfo("Failed to unregister command: " + command, e);
                }

                try {
                    // Unregister the command
                    final Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                    knownCommandsField.setAccessible(true);
                    @SuppressWarnings("unchecked") final Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

                    // Remove the command and its aliases
                    knownCommands.remove(com.getName());
                    for (String alias : com.getAliases()) {
                        knownCommands.remove(alias);
                    }
                } catch (Throwable e) {
                    MessageUtils.logDebugWithInfo("Failed to unregister command: " + command, e);
                }
            }

            CompletableFuture.runAsync(BasePlugin::syncCommands);
        } catch (final Exception e) {
            MessageUtils.logWarningWithInfo("Failed to unregister commands: ", e);
        }
    }

    /**
     * Synchronises the server's command map with connected clients using
     * CraftServer's {@code syncCommands()} method via reflection.
     *
     * <p>This is required on modern versions to push newly registered commands
     * to client tab-completion. Gracefully handles servers that do not expose
     * the method (e.g. older versions).
     */
    public static void syncCommands() {
        try {
            // Get the CraftServer class
            Class<?> craftServerClass = Bukkit.getServer().getClass();

            // Attempt to find the syncCommands method
            try {
                Method syncCommandsMethod = craftServerClass.getDeclaredMethod("syncCommands");
                syncCommandsMethod.setAccessible(true);

                // Invoke the syncCommands method
                syncCommandsMethod.invoke(Bukkit.getServer());
            } catch (NoSuchMethodException e) {
                MessageUtils.logDebugWithInfo("syncCommands method not found: ", e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                MessageUtils.logDebugWithInfo("Failed to invoke syncCommands method: ", e);
            }
        } catch (Exception e) {
            MessageUtils.logDebugWithInfo("An unknown error occurred while syncing commands: ", e);
        }
    }

    /**
     * Builds a map of UUID strings to online Bukkit {@link Player} instances for
     * all currently online players.
     *
     * @return a {@link ConcurrentSkipListMap} keyed by player UUID string
     */
    public static ConcurrentSkipListMap<String, Player> getPlayersByUUID() {
        ConcurrentSkipListMap<String, Player> map = new ConcurrentSkipListMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            map.put(player.getUniqueId().toString(), player);
        }
        return map;
    }

    /**
     * Retrieves a registered Bukkit {@link Command} from the server command map
     * by name.
     *
     * @param name the command name or alias to look up
     * @return the {@link Command}, or {@code null} if not registered
     */
    public static Command getBukkitCommand(String name) {
        return commandMap.getCommand(name);
    }

    /** {@inheritDoc} */
    @Override
    public Logger getLoggerLogger() {
        return Bukkit.getLogger();
    }

    /** {@inheritDoc} */
    @Override
    public org.slf4j.Logger getSLFLogger() {
        return null;
    }
}