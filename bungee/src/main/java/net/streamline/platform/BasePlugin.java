package net.streamline.platform;

import gg.drak.thebase.events.BaseEventHandler;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.base.runnables.PlayerChecker;
import singularity.Singularity;
import singularity.messages.builders.ResourcePackMessageBuilder;
import singularity.objects.CosmicResourcePack;
import singularity.utils.StorageUtils;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import singularity.data.uuid.UuidInfo;
import singularity.data.uuid.UuidManager;
import singularity.events.CosmicEvent;
import singularity.events.server.ServerStartEvent;
import singularity.events.server.ServerStopEvent;
import singularity.interfaces.IProperEvent;
import singularity.interfaces.ISingularityExtension;
import singularity.scheduler.TaskManager;
import singularity.utils.UserUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Abstract base class for the BungeeCord platform plugin.
 *
 * <p>Implements {@link ISingularityExtension} to integrate with the Streamline
 * abstraction layer. Manages the lifecycle of {@link SLAPI}, {@link UserManager},
 * {@link Messenger}, {@link ConsoleHolder}, and {@link PlayerInterface}.
 * Subclasses must implement {@link #enable()}, {@link #disable()}, and
 * {@link #load()} to provide platform-specific startup behaviour.
 */
public abstract class BasePlugin extends Plugin implements ISingularityExtension {

    /** The platform type constant for BungeeCord. */
    @Getter
    private final PlatformType platformType = PlatformType.BUNGEE;

    /** The server type constant identifying this as a proxy server. */
    @Getter
    private final ServerType serverType = ServerType.PROXY;

    /** The plugin name resolved from {@code streamline.properties}. */
    @Getter
    private String name;

    /** The plugin version resolved from {@code streamline.properties}. */
    @Getter
    private String version;

    /** The singleton instance of the currently running {@code BasePlugin}. */
    @Getter
    private static BasePlugin instance;

    /** The Streamline API instance bound to this platform plugin. */
    @Getter
    private SLAPI<CommandSender, ProxiedPlayer, BasePlugin, UserManager, Messenger> slapi;

    /** The user-manager responsible for player and sender lifecycle. */
    @Getter
    private UserManager userManager;

    /** The platform messenger used to send formatted messages. */
    @Getter
    private Messenger messenger;

    /** The holder providing access to the BungeeCord console sender. */
    @Getter
    private ConsoleHolder consoleHolder;

    /** The interface for obtaining platform {@link ProxiedPlayer} instances. */
    @Getter
    private PlayerInterface playerInterface;

    /** Periodic task that checks online player state. */
    @Getter @Setter
    private static PlayerChecker playerChecker;

    /** The resource pack currently assigned to this proxy instance. */
    @Getter @Setter
    private CosmicResourcePack resourcePack;

    /**
     * {@inheritDoc}
     *
     * <p>Stores the singleton instance, reads {@code streamline.properties},
     * migrates any legacy plugin data-folder names to the canonical name, then
     * delegates to {@link #load()}.
     */
    @Override
    public void onLoad() {
        instance = this;

        setupProperties();

        String parentPath = getDataFolder().getParent();
        if (parentPath != null) {
            File parentFile = new File(parentPath);
            File[] files = parentFile.listFiles((f) -> {
                if (! f.isDirectory()) return false;
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
                    file.renameTo(new File(parentPath, this.name));
                });
            }
        }

        this.load();
    }

    /**
     * Reads {@code streamline.properties} and populates {@link #name} and
     * {@link #version} from the key-value pairs found there.
     */
    public void setupProperties() {
        ConcurrentSkipListMap<String, String> properties = StorageUtils.readProperties();
        if (properties.isEmpty()) return;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals("name")) {
                this.name = value;
            }
            if (key.equals("version")) {
                this.version = value;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises core services ({@link UserManager}, {@link Messenger},
     * {@link ConsoleHolder}, {@link PlayerInterface}), creates the {@link SLAPI}
     * instance, registers the platform listener and plugin-messaging channel,
     * starts the {@link singularity.scheduler.TaskManager}, and then calls
     * {@link #enable()} followed by firing a {@link singularity.events.server.ServerStartEvent}.
     */
    @Override
    public void onEnable() {
        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();
        slapi = new SLAPI<>(getName(), this, getUserManager(), getMessenger(), getConsoleHolder(), getPlayerInterface(), BaseModule::new);
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());

        registerListener(new PlatformListener());

        TaskManager.init();

        getProxy().registerChannel(SLAPI.getApiChannel());

        playerChecker = new PlayerChecker();

        this.enable();
        fireStartEvent();
    }

    /**
     * Fires a {@link singularity.events.server.ServerStartEvent} and, if the event
     * is not cancelled and is sendable, prints its message to the console.
     */
    public void fireStartEvent() {
        ServerStartEvent e = new ServerStartEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cancels teleport-ticket tasks, synchronises all users, saves UUID data,
     * unregisters the plugin-messaging channel, calls {@link #disable()}, fires a
     * {@link singularity.events.server.ServerStopEvent}, and stops the
     * {@link singularity.scheduler.TaskManager}.
     */
    @Override
    public void onDisable() {
        Singularity.getTpTicketFlusher().cancel();
        Singularity.getTpTicketPuller().cancel();

        UserUtils.syncAllUsers();
        UuidManager.getUuids().forEach(UuidInfo::save);

        getProxy().unregisterChannel(SLAPI.getApiChannel());

        this.disable();
        fireStopEvent();

        TaskManager.stop();
    }

    /**
     * Fires a {@link singularity.events.server.ServerStopEvent} and, if the event
     * is not cancelled and is sendable, prints its message to the console.
     */
    public void fireStopEvent() {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * Called after core services are initialised; subclasses perform their
     * platform-specific enable logic here.
     */
    abstract public void enable();

    /**
     * Called before the plugin fully shuts down; subclasses perform their
     * platform-specific cleanup here.
     */
    abstract public void disable();

    /**
     * Called during the BungeeCord {@code onLoad} phase; subclasses perform
     * early initialisation here before services are available.
     */
    abstract public void load();

    /**
     * Registers a BungeeCord {@link Listener} against the proxy plugin manager.
     *
     * @param listener the listener to register
     */
    public static void registerListener(Listener listener) {
        getInstance().getProxy().getPluginManager().registerListener(getInstance(), listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();

        for (ProxiedPlayer player : onlinePlayers()) {
            CosmicPlayer cosmicPlayer = getUserManager().getOrCreatePlayer(player).orElse(null);
            if (cosmicPlayer == null) continue;
            players.add(cosmicPlayer);
        }

        return players;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProperCommand createCommand(CosmicCommand command) {
        return new ProperCommand(command);
    }

    /**
     * Returns the maximum number of players allowed on this BungeeCord proxy,
     * as configured in {@code config.yml}.
     *
     * @return the configured player limit
     */
    public int getMaxPlayers() {
        return getInstance().getProxy().getConfig().getPlayerLimit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getCurrentName());
        });

//        r.add(UserUtils.getConsole().latestName);

        return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOfflineMode() {
        return ! getInstance().getProxy().getConfig().isOnlineMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getConnectionThrottle() {
        return getInstance().getProxy().getConfig().getThrottle();
    }

    /**
     * Returns a snapshot list of all currently online {@link ProxiedPlayer}s.
     *
     * @return a mutable list of online players
     */
    public static List<ProxiedPlayer> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getPlayers());
    }

    /**
     * Returns a snapshot list of all {@link ProxiedPlayer}s connected to the
     * named backend server.
     *
     * @param serverName the name of the backend server
     * @return a mutable list of players on that server
     */
    public static List<ProxiedPlayer> playersOnServer(String serverName) {
        return new ArrayList<>(getInstance().getProxy().getServerInfo(serverName).getPlayers());
    }

    /**
     * Looks up an online {@link ProxiedPlayer} by their UUID string.
     *
     * @param uuid the player's UUID as a string
     * @return the matching player, or {@code null} if not found
     */
    public static ProxiedPlayer getPlayer(String uuid) {
        for (ProxiedPlayer player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    /**
     * Looks up an online {@link ProxiedPlayer} by their username.
     *
     * @param name the player's username (case-insensitive on BungeeCord)
     * @return an {@link Optional} containing the player, or empty if not found
     */
    public static Optional<ProxiedPlayer> getPlayerByName(String name) {
        return Optional.ofNullable(getInstance().getProxy().getPlayer(name));
    }

    /**
     * Looks up an online {@link ProxiedPlayer} by exact username.
     *
     * @param name the player's exact username; must not be {@code null}
     * @return the player, or {@code null} if not found
     */
    public static @Nullable ProxiedPlayer getPlayerExact(@NotNull String name) {
        if (getPlayerByName(name).isEmpty()) return null;
        return getPlayerByName(name).get();
    }

    /**
     * Returns the {@link ProxiedPlayer} corresponding to the given
     * {@link CommandSender}, looked up by sender name.
     *
     * @param sender the command sender whose player instance is required
     * @return the matching {@link ProxiedPlayer}, or {@code null} if the sender is not a player
     */
    public static ProxiedPlayer getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getConfig().isOnlineMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        getInstance().getProxy().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (ProxiedPlayer player : onlinePlayers()) {
            if (! player.hasPermission(permission)) continue;
            getMessenger().sendMessage(player, message);
            people ++;
        }

        return people;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (! (event.getEvent() instanceof Event)) return;
        Event e = (Event) event.getEvent();
        getInstance().getProxy().getPluginManager().callEvent(e);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always fires the event asynchronously on BungeeCord.
     */
    @Override
    public void fireEvent(CosmicEvent event) {
        fireEvent(event, true);
    }

    /**
     * {@inheritDoc}
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
     */
    @Override
    public void handleMisSync(CosmicEvent event, boolean async) {
        BaseEventHandler.fireEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        return new ConcurrentSkipListSet<>(getInstance().getProxy().getServers().keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        ProxiedPlayer p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, String uuid) {
        ProxiedPlayer p = getPlayer(uuid);
        sendResourcePack(resourcePack, p);
    }

    /**
     * Sends the specified resource pack to a {@link ProxiedPlayer} via the
     * {@link singularity.messages.builders.ResourcePackMessageBuilder} pipeline.
     *
     * @param resourcePack the resource pack to send
     * @param player       the target player; the method returns immediately if {@code null}
     */
    public void sendResourcePack(CosmicResourcePack resourcePack, ProxiedPlayer player) {
        if (player == null) return;
        CosmicPlayer streamPlayer = getUserManager().getOrCreatePlayer(player).orElse(null);
        if (streamPlayer == null) return;

        ResourcePackMessageBuilder.build(streamPlayer, true, streamPlayer, resourcePack).send();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }

    /**
     * Returns a sorted map of UUID strings to {@link ProxiedPlayer} instances
     * for all currently online players.
     *
     * @return a {@link ConcurrentSkipListMap} keyed by UUID string
     */
    public static ConcurrentSkipListMap<String, ProxiedPlayer> getPlayersByUUID() {
        ConcurrentSkipListMap<String, ProxiedPlayer> map = new ConcurrentSkipListMap<>();
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            map.put(player.getUniqueId().toString(), player);
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLoggerLogger() {
        return getProxy().getLogger();
    }

    /**
     * {@inheritDoc}
     *
     * <p>BungeeCord does not expose an SLF4J logger; this implementation
     * always returns {@code null}.
     */
    @Override
    public org.slf4j.Logger getSLFLogger() {
        return null;
    }
}
