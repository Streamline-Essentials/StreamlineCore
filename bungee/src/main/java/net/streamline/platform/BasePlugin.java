package net.streamline.platform;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.SLAPI;
import singularity.logging.CosmicLogHandler;
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
import tv.quaint.events.BaseEventHandler;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class BasePlugin extends Plugin implements ISingularityExtension {
    @Getter
    private final PlatformType platformType = PlatformType.BUNGEE;
    @Getter
    private final ServerType serverType = ServerType.PROXY;

    @Getter
    private String name;
    @Getter
    private String version;
    @Getter
    private static BasePlugin instance;
    @Getter
    private SLAPI<CommandSender, ProxiedPlayer, BasePlugin, UserManager, Messenger> slapi;

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

    @Override
    public void onEnable() {
        getLogger().addHandler(new CosmicLogHandler());

        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();
        slapi = new SLAPI<>(getName(), this, getUserManager(), getMessenger(), getConsoleHolder(), getPlayerInterface());
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());

        registerListener(new PlatformListener());

        TaskManager.init();

        getProxy().registerChannel(SLAPI.getApiChannel());

        this.enable();
        fireStartEvent();
    }

    public void fireStartEvent() {
        ServerStartEvent e = new ServerStartEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    @Override
    public void onDisable() {
        UserUtils.syncAllUsers();
        UuidManager.getUuids().forEach(UuidInfo::save);

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

    public static void registerListener(Listener listener) {
        getInstance().getProxy().getPluginManager().registerListener(getInstance(), listener);
    }

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();

        for (ProxiedPlayer player : onlinePlayers()) {
            players.add(getUserManager().getOrCreatePlayer(player));
        }

        return players;
    }

    @Override
    public ProperCommand createCommand(CosmicCommand command) {
        return new ProperCommand(command);
    }

    public int getMaxPlayers() {
        return getInstance().getProxy().getConfig().getPlayerLimit();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getCurrentName());
        });

//        r.add(UserUtils.getConsole().latestName);

        return r;
    }

    @Override
    public long getConnectionThrottle() {
        return getInstance().getProxy().getConfig().getThrottle();
    }

    public static List<ProxiedPlayer> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getPlayers());
    }

    public static List<ProxiedPlayer> playersOnServer(String serverName) {
        return new ArrayList<>(getInstance().getProxy().getServerInfo(serverName).getPlayers());
    }

    public static ProxiedPlayer getPlayer(String uuid) {
        for (ProxiedPlayer player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    public static Optional<ProxiedPlayer> getPlayerByName(String name) {
        return Optional.ofNullable(getInstance().getProxy().getPlayer(name));
    }

    public static @Nullable ProxiedPlayer getPlayerExact(@NotNull String name) {
        if (getPlayerByName(name).isEmpty()) return null;
        return getPlayerByName(name).get();
    }

    public static ProxiedPlayer getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
    }

    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getConfig().isOnlineMode();
    }

    @Override
    public void shutdown() {
        getInstance().getProxy().stop();
    }

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

    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin) != null;
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (! (event.getEvent() instanceof Event)) return;
        Event e = (Event) event.getEvent();
        getInstance().getProxy().getPluginManager().callEvent(e);
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
    public void handleMisSync(CosmicEvent event, boolean async) {
        BaseEventHandler.fireEvent(event);
    }

    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        return new ConcurrentSkipListSet<>(getInstance().getProxy().getServers().keySet());
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        ProxiedPlayer p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, String uuid) {
        ProxiedPlayer p = getPlayer(uuid);
        sendResourcePack(resourcePack, p);
    }

    public void sendResourcePack(CosmicResourcePack resourcePack, ProxiedPlayer player) {
        if (player == null) return;
        CosmicPlayer streamPlayer = getUserManager().getOrCreatePlayer(player);
        if (streamPlayer == null) return;

        ResourcePackMessageBuilder.build(streamPlayer, true, streamPlayer, resourcePack).send();
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }
}
