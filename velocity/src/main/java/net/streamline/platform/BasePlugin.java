package net.streamline.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import gg.drak.thebase.events.BaseEventHandler;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.base.StreamlineVelocity;
import net.streamline.base.runnables.PlayerChecker;
import net.streamline.metrics.Metrics;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import singularity.data.uuid.UuidInfo;
import singularity.data.uuid.UuidManager;
import singularity.events.CosmicEvent;
import singularity.events.server.ServerStopEvent;
import singularity.interfaces.IProperEvent;
import singularity.interfaces.ISingularityExtension;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.TaskManager;
import singularity.utils.MessageUtils;
import singularity.utils.StorageUtils;
import singularity.utils.UserUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Abstract Velocity platform implementation of {@link ISingularityExtension}.
 *
 * <p>Manages the full lifecycle of the StreamlineCore Velocity plugin: dependency injection,
 * proxy event subscriptions, {@link SLAPI} initialisation, player tracking, resource-pack
 * delivery, command registration, and cross-platform event firing.
 *
 * <p>Subclasses must implement {@link #enable()}, {@link #disable()}, and {@link #load()}.
 */
public abstract class BasePlugin implements ISingularityExtension {
    /**
     * Always {@link PlatformType#VELOCITY}; identifies this implementation as a Velocity proxy.
     */
    @Getter
    private final PlatformType platformType = PlatformType.VELOCITY;

    /**
     * Always {@link ServerType#PROXY}; indicates this is a proxy-side installation.
     */
    @Getter
    private final ServerType serverType = ServerType.PROXY;

    /**
     * The plugin name as read from {@code streamline.properties} at startup.
     */
    @Getter
    private String name;

    /**
     * The plugin version as read from {@code streamline.properties} at startup.
     */
    @Getter
    private String version;

    /**
     * The singleton {@code BasePlugin} instance set during {@link #onLoad()}.
     */
    @Getter
    private static BasePlugin instance;

    /**
     * The {@link SLAPI} instance that wires together the UserManager, Messenger,
     * ConsoleHolder, and PlayerInterface for this platform.
     */
    @Getter
    private SLAPI<CommandSource, Player, BasePlugin, UserManager, Messenger> slapi;

    /**
     * The Velocity-specific {@link UserManager} for player/sender resolution and management.
     */
    @Getter
    private UserManager userManager;

    /**
     * The Velocity-specific {@link Messenger} for cross-platform text dispatch.
     */
    @Getter
    private Messenger messenger;

    /**
     * The {@link ConsoleHolder} wrapping the Velocity console command source.
     */
    @Getter
    private ConsoleHolder consoleHolder;

    /**
     * The {@link PlayerInterface} providing player lookup and action wrappers.
     */
    @Getter
    private PlayerInterface playerInterface;

    /**
     * The active resource pack to be sent to players, or {@code null} if none is configured.
     */
    @Getter @Setter
    private CosmicResourcePack resourcePack;

    /**
     * The Velocity {@link ProxyServer} instance injected at construction.
     */
    @Getter
    private final ProxyServer proxy;

    /**
     * The SLF4J logger provided by Velocity.
     */
    @Getter
    private final Logger logger;

    /**
     * The plugin's data directory as a {@link Path} (mirrors {@link #dataFolder}).
     */
    @Getter
    private final Path dataDirectory;

    /**
     * The plugin's data directory as a {@link File}.
     */
    @Getter
    private final File dataFolder;

    /**
     * The bStats {@link Metrics.Factory} used to create and register metrics charts.
     */
    @Getter
    private final Metrics.Factory metricsFactory;

    /**
     * The periodic task that ensures all online players have an initialised {@link CosmicPlayer}.
     */
    @Getter @Setter
    private static PlayerChecker playerChecker;

    /**
     * Constructs the plugin, resolves and renames any legacy data folders, and triggers
     * {@link #onLoad()}.
     *
     * @param server         the Velocity {@link ProxyServer} instance
     * @param logger         the SLF4J logger provided by Velocity
     * @param dataFolder     the plugin data directory
     * @param metricsFactory the bStats factory used to create metric instances
     */
    public BasePlugin(ProxyServer server, Logger logger, File dataFolder, Metrics.Factory metricsFactory) {
        this.proxy = server;
        this.logger = logger;
        this.dataDirectory = dataFolder.toPath();
        this.dataFolder = dataFolder;
        this.metricsFactory = metricsFactory;

        Path parentPath = this.dataDirectory.getParent();
        if (parentPath != null) {
            File parentFile = new File(parentPath.toString());
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
                    file.renameTo(new File(parentPath.toString(), StreamlineVelocity.getStreamlineName()));
                });
            }
        }

        onLoad();
    }

    /**
     * Called by the constructor immediately after field initialisation. Registers the singleton
     * instance, reads properties, renames any legacy data folders, and delegates to {@link #load()}.
     */
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
     * Reads {@code name} and {@code version} from the bundled {@code streamline.properties}
     * resource and populates the corresponding fields.
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
     * Handles the Velocity {@link ProxyInitializeEvent}: initialises all platform components
     * ({@link UserManager}, {@link Messenger}, {@link ConsoleHolder}, {@link PlayerInterface},
     * {@link SLAPI}), registers the plugin-messaging channel, starts the {@link TaskManager},
     * and starts the {@link PlayerChecker} task before calling {@link #enable()}.
     *
     * @param event the proxy initialisation event
     */
    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();
        slapi = new SLAPI<>(getName(), this, getUserManager(), getMessenger(), getConsoleHolder(), getPlayerInterface(), BaseModule::new);
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());

        registerListener(new PlatformListener());

        TaskManager.init();

//        UserUtils.loadSender(new StreamSender());

        getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        playerChecker = new PlayerChecker();

        this.enable();
    }

    /**
     * Handles the Velocity {@link ProxyShutdownEvent}: cancels TP ticket tasks, syncs all users,
     * saves UUID information, unregisters the plugin-messaging channel, calls {@link #disable()},
     * fires the {@link singularity.events.server.ServerStopEvent}, and stops the task manager.
     *
     * @param event the proxy shutdown event
     */
    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        Singularity.getTpTicketFlusher().cancel();
        Singularity.getTpTicketPuller().cancel();

        UserUtils.syncAllUsers();
        UuidManager.getUuids().forEach(UuidInfo::save);

        getProxy().getChannelRegistrar().unregister(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.disable();
        fireStopEvent();

        TaskManager.stop();
    }

    /**
     * Fires a {@link singularity.events.server.ServerStopEvent} and, if the event is not
     * cancelled and is marked sendable, broadcasts its message to the console.
     */
    public void fireStopEvent() {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * Called after all platform components are initialised. Subclasses should perform
     * plugin-specific startup logic here.
     */
    abstract public void enable();

    /**
     * Called when the proxy shuts down. Subclasses should perform plugin-specific
     * cleanup and teardown here.
     */
    abstract public void disable();

    /**
     * Called before {@link #enable()} during the load phase. Subclasses may perform
     * pre-initialisation work here.
     */
    abstract public void load();

    /**
     * Registers a Velocity event listener with the proxy event manager using this plugin
     * as the owner.
     *
     * @param listener the listener object to register
     */
    public static void registerListener(Object listener) {
        getInstance().getProxy().getEventManager().register(getInstance(), listener);
    }

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();

        for (Player player : onlinePlayers()) {
            CosmicPlayer cosmicPlayer = getUserManager().getOrCreatePlayer(player).orElse(null);
            if (cosmicPlayer == null) continue;
            players.add(cosmicPlayer);
        }

        return players;
    }

    @Override
    public ProperCommand createCommand(CosmicCommand command) {
        return new ProperCommand(command);
    }

    @Override
    public int getMaxPlayers() {
        return getInstance().getProxy().getConfiguration().getShowMaxPlayers();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getCurrentName());
        });

//        r.add(getUserManager().getConsole().latestName);

        return r;
    }

    @Override
    public long getConnectionThrottle() {
        return getInstance().getProxy().getConfiguration().getCompressionThreshold();
    }

    /**
     * Returns an unmodifiable snapshot of all currently connected players.
     *
     * @return a new {@link List} of all online {@link Player} instances
     */
    public static List<Player> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getAllPlayers());
    }

    /**
     * Returns all players currently connected to the named backend server.
     *
     * @param serverName the name of the registered backend server
     * @return a list of connected players, or an empty list if the server is not found
     */
    public static List<Player> playersOnServer(String serverName) {
        Optional<RegisteredServer> serverOpt = getInstance().getProxy().getServer(serverName);
        return serverOpt.map(registeredServer -> new ArrayList<>(registeredServer.getPlayersConnected())).orElseGet(ArrayList::new);
    }

    /**
     * Finds an online player by their UUID string.
     *
     * @param uuid the UUID string to search for
     * @return the matching {@link Player}, or {@code null} if no online player has that UUID
     */
    public static Player getPlayer(String uuid) {
        for (Player player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    /**
     * Looks up an online player by exact username via the Velocity proxy.
     *
     * @param name the player's username
     * @return an {@link Optional} containing the player, or empty if not found
     */
    public static Optional<Player> getPlayerByName(String name) {
        return getInstance().getProxy().getPlayer(name);
    }

    /**
     * Returns the online player whose username exactly matches the given name, or {@code null}.
     *
     * @param name the exact username to match (not null)
     * @return the matching {@link Player}, or {@code null} if not online
     */
    public static @Nullable Player getPlayerExact(@NotNull String name) {
        if (getPlayerByName(name).isEmpty()) return null;
        return getPlayerByName(name).get();
    }

    /**
     * Returns a list containing the online player whose username exactly matches the given name.
     *
     * @param name the username to match (not null)
     * @return a singleton list with the player, or an empty list if not found
     */
    public static @NotNull List<Player> matchPlayer(@NotNull String name) {
        Player player = getPlayerExact(name);
        if (player == null) return new ArrayList<>();
        return List.of(player);
    }

    /**
     * Finds an online player by their {@link UUID}.
     *
     * @param id the UUID to search for (not null)
     * @return the matching {@link Player}, or {@code null} if not online
     */
    public static @Nullable Player getPlayer(@NotNull UUID id) {
        return getPlayer(id.toString());
    }

    /**
     * Resolves the online player associated with the given {@link CommandSource}.
     *
     * <p>Looks up the player by the username derived from the source via {@link UserManager}.
     *
     * @param sender the command source to resolve
     * @return the associated {@link Player}, or {@code null} if the source is not a player
     */
    public static Player getPlayer(CommandSource sender) {
        Optional<Player> player = getInstance().getProxy().getPlayer(getInstance().getUserManager().getUsername(sender));
        return player.orElse(null);
    }

    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getConfiguration().isOnlineMode();
    }

    @Override
    public void shutdown() {
        getInstance().getProxy().shutdown();
    }

    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (Player player : onlinePlayers()) {
            if (! player.hasPermission(permission)) continue;
            getMessenger().sendMessage(player, message);
            people ++;
        }

        return people;
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin).isPresent();
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (! (event.getEvent() instanceof CompletableFuture<?>)) return;
        CompletableFuture<?> e = (CompletableFuture<?>) event.getEvent();
        getInstance().getProxy().getEventManager().fire(e).join();
    }

    @Override
    public void fireEvent(CosmicEvent event) {
        fireEvent(event, true);
    }

    @Override
    public void fireEvent(CosmicEvent event, boolean async) {
        try {
            BaseEventHandler.fireEvent(event);
        } catch (Exception e) {
            handleMisSync(event, async);
        }
    }

    @Override
    public boolean isOfflineMode() {
        return ! getInstance().getProxy().getConfiguration().isOnlineMode();
    }

    @Override
    public void handleMisSync(CosmicEvent event, boolean async) {
        BaseEventHandler.fireEvent(event);
    }

    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getInstance().getProxy().getAllServers().forEach(a -> {
            r.add(a.getServerInfo().getName());
        });

        return r;
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        Player p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, String uuid) {
        Player p = getPlayer(uuid);
        sendResourcePack(resourcePack, p);
    }

    /**
     * Sends a resource pack offer to the specified Velocity {@link Player}.
     *
     * <p>Builds the Velocity {@link ResourcePackInfo} from the {@link CosmicResourcePack} fields
     * and calls {@link Player#sendResourcePackOffer(ResourcePackInfo)}.
     *
     * @param resourcePack the resource pack descriptor; must not be {@code null}
     * @param player       the target player; if {@code null}, this method is a no-op
     */
    public void sendResourcePack(CosmicResourcePack resourcePack, Player player) {
        if (player == null) return;
        try {
            ResourcePackInfo.Builder infoBuilder = getInstance().getProxy().createResourcePackBuilder(resourcePack.getUrl()).setShouldForce(resourcePack.isForce());
            if (resourcePack.getHash().length > 0) infoBuilder.setHash(resourcePack.getHash());
            if (! resourcePack.getPrompt().isEmpty()) infoBuilder.setPrompt(getMessenger().codedText(resourcePack.getPrompt()));
            player.sendResourcePackOffer(infoBuilder.build());
        } catch (Exception e) {
            MessageUtils.logWarning("Sent '" + player.getUsername() + "' a resourcepack, but it returned null! This is probably due to an incorrect link to the pack.");
        }
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }

    /**
     * Returns a sorted map of all currently connected players keyed by their UUID string.
     *
     * @return a {@link ConcurrentSkipListMap} mapping UUID strings to their {@link Player} instances
     */
    public static ConcurrentSkipListMap<String, Player> getPlayersByUUID() {
        ConcurrentSkipListMap<String, Player> map = new ConcurrentSkipListMap<>();
        for (Player player : getInstance().getProxy().getAllPlayers()) {
            map.put(player.getUniqueId().toString(), player);
        }
        return map;
    }

    @Override
    public java.util.logging.Logger getLoggerLogger() {
        return null;
    }

    @Override
    public org.slf4j.Logger getSLFLogger() {
        return getLogger();
    }
}
