package net.streamline.platform;

import gg.drak.thebase.events.BaseEventHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.handlers.BackendHandler;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;
import singularity.events.server.ServerStopEvent;
import singularity.interfaces.IProperEvent;
import singularity.interfaces.ISingularityExtension;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.TaskManager;
import singularity.utils.MessageUtils;
import singularity.utils.StorageUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class BasePlugin implements ISingularityExtension {

    @Getter
    private final PlatformType platformType = PlatformType.FORGE;

    @Getter
    private final ServerType serverType = ServerType.BACKEND;

    @Getter
    @Setter
    private CosmicResourcePack resourcePack;

    @Getter
    private String version;

    @Getter
    private String folderName;

    @Getter
    private static BasePlugin instance;

    @Getter
    private SLAPI<Object, ServerPlayer, BasePlugin, UserManager, Messenger> slapi;

    @Getter
    private UserManager userManager;

    @Getter
    private Messenger messenger;

    @Getter
    private ConsoleHolder consoleHolder;

    @Getter
    private PlayerInterface playerInterface;

    private static final Logger LOGGER = LoggerFactory.getLogger("StreamlineCore");

    /**
     * Called from the concrete {@code @Mod} constructor to initialize the plugin.
     */
    protected void initialize() {
        instance = this;
        setupProperties();
        load();

        NeoForge.EVENT_BUS.register(new PlatformListener());
    }

    /**
     * Called when the server is starting (from ServerStartingEvent in PlatformListener).
     */
    public void onServerEnable() {
        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();

        slapi = new SLAPI<>(
                getFolderName(), this, getUserManager(), getMessenger(),
                getConsoleHolder(), getPlayerInterface(), BaseModule::new);
        SLAPI.setBackendHandler(new BackendHandler());

        TaskManager.init();

        enable();
    }

    /**
     * Called when the server is stopping (from ServerStoppingEvent in PlatformListener).
     */
    public void onServerDisable() {
        disable();

        try {
            ServerStopEvent e = new ServerStopEvent().fire();
            if (!e.isCancelled() && e.isSendable()) {
                SLAPI.sendConsoleMessage(e.getMessage());
            }
        } catch (Exception ignored) {}

        TaskManager.stop();
    }

    public void setupProperties() {
        ConcurrentSkipListMap<String, String> properties = StorageUtils.readProperties();
        if (properties.isEmpty()) return;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().equals("name")) this.folderName = entry.getValue();
            if (entry.getKey().equals("version")) this.version = entry.getValue();
        }
    }

    public abstract void load();
    public abstract void enable();
    public abstract void disable();
    public abstract void reload();

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return players;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (UserUtils.isLoaded(player.getStringUUID())) {
                userManager.getOrCreatePlayer(player).ifPresent(players::add);
            }
        }
        return players;
    }

    @Override
    public ProperCommand createCommand(CosmicCommand command) {
        return new ProperCommand(command);
    }

    @Override
    public int getMaxPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 20;
        return server.getMaxPlayers();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getOnlinePlayers().forEach(p -> r.add(p.getCurrentName()));
        return r;
    }

    @Override
    public boolean isOfflineMode() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && !server.usesAuthentication();
    }

    @Override
    public long getConnectionThrottle() {
        return -1;
    }

    @Override
    public boolean getOnlineMode() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.usesAuthentication();
    }

    @Override
    public void shutdown() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.halt(false);
    }

    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int count = 0;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return count;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(messenger != null
                    ? messenger.codedString(message) : message));
            count++;
        }
        return count;
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        // NeoForge doesn't have a universal plugin registry; return false as a safe fallback
        return false;
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (event.getEvent() instanceof CosmicEvent ce) {
            BaseEventHandler.fireEvent(ce);
        }
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
        return new ConcurrentSkipListSet<>();
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        sendResourcePack(resourcePack, player.getUuid());
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, String uuid) {
        MessageUtils.logWarning("Resource pack sending not fully implemented for NeoForge yet.");
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public String getName() {
        return folderName != null ? folderName : "StreamlineCore";
    }

    @Override
    public java.util.logging.Logger getLoggerLogger() {
        return null;
    }

    @Override
    public org.slf4j.Logger getSLFLogger() {
        return LOGGER;
    }

    public org.slf4j.Logger getSlf4jLogger() {
        return LOGGER;
    }

    public static List<ServerPlayer> onlinePlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return new ArrayList<>();
        return new ArrayList<>(server.getPlayerList().getPlayers());
    }

    public static ServerPlayer getPlayer(String uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
