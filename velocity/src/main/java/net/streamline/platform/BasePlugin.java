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
import net.streamline.base.runnables.PlayerTeleporter;
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

public abstract class BasePlugin implements ISingularityExtension {
    @Getter
    private final PlatformType platformType = PlatformType.VELOCITY;
    @Getter
    private final ServerType serverType = ServerType.PROXY;

    @Getter
    private String name;
    @Getter
    private String version;
    @Getter
    private static BasePlugin instance;
    @Getter
    private SLAPI<CommandSource, Player, BasePlugin, UserManager, Messenger> slapi;

    @Getter
    private UserManager userManager;
    @Getter
    private Messenger messenger;
    @Getter
    private ConsoleHolder consoleHolder;
    @Getter
    private PlayerInterface playerInterface;

    @Getter @Setter
    private CosmicResourcePack resourcePack;

    @Getter
    private final ProxyServer proxy;
    @Getter
    private final Logger logger;
    @Getter
    private final Path dataDirectory;
    @Getter
    private final File dataFolder;
    @Getter
    private final Metrics.Factory metricsFactory;

    @Getter @Setter
    private static PlayerChecker playerChecker;

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
        PlayerTeleporter.init();

        this.enable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        PlayerTeleporter.stopInstance();

        UserUtils.syncAllUsers();
        UuidManager.getUuids().forEach(UuidInfo::save);

        getProxy().getChannelRegistrar().unregister(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.disable();
        fireStopEvent();

        TaskManager.stop();
    }

    public void fireStopEvent() {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

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

    public static List<Player> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getAllPlayers());
    }

    public static List<Player> playersOnServer(String serverName) {
        Optional<RegisteredServer> serverOpt = getInstance().getProxy().getServer(serverName);
        return serverOpt.map(registeredServer -> new ArrayList<>(registeredServer.getPlayersConnected())).orElseGet(ArrayList::new);
    }

    public static Player getPlayer(String uuid) {
        for (Player player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    public static Optional<Player> getPlayerByName(String name) {
        return getInstance().getProxy().getPlayer(name);
    }

    public static @Nullable Player getPlayerExact(@NotNull String name) {
        if (getPlayerByName(name).isEmpty()) return null;
        return getPlayerByName(name).get();
    }

    public static @NotNull List<Player> matchPlayer(@NotNull String name) {
        Player player = getPlayerExact(name);
        if (player == null) return new ArrayList<>();
        return List.of(player);
    }

    public static @Nullable Player getPlayer(@NotNull UUID id) {
        return getPlayer(id.toString());
    }

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
