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
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.metrics.Metrics;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.profile.VelocityProfiler;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import tv.quaint.events.BaseEventHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public abstract class BasePlugin implements IStreamline {
    public static class Runner implements Runnable {
        public Runner() {
            MessageUtils.logInfo("Task Runner registered!");
        }

        @Override
        public void run() {
            SLAPI.getMainScheduler().tick();
        }
    }

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

    @Setter
    private VelocityProfiler profiler;

    @Getter @Setter
    private StreamlineResourcePack resourcePack;

    @Getter
    private final ProxyServer proxy;
    @Getter
    private final Logger logger;
    @Getter
    private final File dataFolder;
    @Getter
    private final Metrics.Factory metricsFactory;

    public BasePlugin(ProxyServer s, Logger l, Path dd, Metrics.Factory mf) {
        this.proxy = s;
        this.logger = l;
        this.dataFolder = dd.toFile();
        this.metricsFactory = mf;

        Path parentPath = dd.getParent();
        if (parentPath != null) {
            File parentFile = new File(parentPath.toUri());
            File[] files = parentFile.listFiles((f) -> {
                if (! f.isDirectory()) return false;
                if (f.getName().equals("streamlineapi")) return true;
                return false;
            });

            if (files != null) {
                Arrays.stream(files).forEach(file -> {
                    file.renameTo(new File(parentPath.toFile(), "streamlinecore"));
                });
            }
        }

        onLoad();
    }

    public void onLoad() {
        instance = this;
        name = "streamlinecore";
        version = "${{project.version}}";

        this.load();
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();
        slapi = new SLAPI<>(getName(), this, getUserManager(), getMessenger(), getConsoleHolder(), getPlayerInterface());
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());
        getSlapi().setProfiler(new VelocityProfiler());

        registerListener(new PlatformListener());
        getProxy().getScheduler().buildTask(this, new Runner()).repeat(50, TimeUnit.MILLISECONDS).schedule();

//        UserUtils.loadSender(new StreamSender());

        getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.enable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        for (StreamSender user : UserUtils.getLoadedSendersSet()) {
            user.save();
        }

        getProxy().getChannelRegistrar().unregister(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.disable();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    public static void registerListener(Object listener) {
        getInstance().getProxy().getEventManager().register(getInstance(), listener);
    }

    @Override
    public @NotNull ConcurrentSkipListSet<StreamPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<StreamPlayer> players = new ConcurrentSkipListSet<>();

        for (Player player : onlinePlayers()) {
            players.add(getUserManager().getOrGetPlayer(player));
        }

        return players;
    }

    @Override
    public ProperCommand createCommand(StreamlineCommand command) {
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
    public void fireEvent(StreamlineEvent event) {
        fireEvent(event, true);
    }

    @Override
    public void fireEvent(StreamlineEvent event, boolean async) {
        try {
            BaseEventHandler.fireEvent(event);
        } catch (Exception e) {
            handleMisSync(event, async);
        }
    }

    @Override
    public void handleMisSync(StreamlineEvent event, boolean async) {
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
    public void sendResourcePack(StreamlineResourcePack resourcePack, StreamPlayer player) {
        Player p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, String uuid) {
        Player p = getPlayer(uuid);
        sendResourcePack(resourcePack, p);
    }

    public void sendResourcePack(StreamlineResourcePack resourcePack, Player player) {
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
}
