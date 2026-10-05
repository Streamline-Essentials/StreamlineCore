package net.streamline.platform;

import gg.drak.thebase.events.BaseEventHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.handlers.BackendHandler;
import net.streamline.platform.handlers.GameplayHandler;
import singularity.Singularity;
import net.streamline.platform.modules.RelocatingModuleTransformer;
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

/**
 * Loader-agnostic core of the mod-loader platforms (Fabric, Forge, NeoForge).
 *
 * <p>Everything here is written against Mojang-mapped Minecraft classes only, so the same
 * source compiles for every loader. Each loader supplies a thin entrypoint subclass that
 * reports its {@link PlatformType} and forwards its lifecycle/player events to
 * {@link net.streamline.platform.listeners.ModEvents}.</p>
 */
public abstract class BasePlugin implements ISingularityExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger("StreamlineCore");

    @Getter
    private static BasePlugin instance;

    /**
     * The running server, set by the loader when the server starts and cleared once it has
     * stopped. Mod loaders construct mods before any server exists, so this is the single
     * place shared code looks the server up.
     */
    @Getter
    @Setter
    private static MinecraftServer server;

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
    private SLAPI<Object, ServerPlayer, BasePlugin, UserManager, Messenger> slapi;

    @Getter
    private UserManager userManager;

    @Getter
    private Messenger messenger;

    @Getter
    private ConsoleHolder consoleHolder;

    @Getter
    private PlayerInterface playerInterface;

    /**
     * Called once from the loader entrypoint, before any server exists.
     */
    protected void initialize() {
        instance = this;
        RelocatingModuleTransformer.install();
        setupProperties();
        load();
    }

    /**
     * Brings Streamline up against a server that is starting. Modules load here, so their
     * commands register straight into the server's live dispatcher.
     */
    public void onServerEnable(MinecraftServer server) {
        setServer(server);

        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();

        slapi = new SLAPI<>(
                getFolderName(), this, getUserManager(), getMessenger(),
                getConsoleHolder(), getPlayerInterface(), BaseModule::new);
        SLAPI.setBackendHandler(new BackendHandler());
        Singularity.setGameplayHandler(createGameplayHandler());

        TaskManager.init();

        enable();

        // External modules load only once the platform reports itself enabled.
        setPlatformAsEnabled();
    }

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

    /** The loader's gameplay handler, which knows how that loader refreshes player names. */
    protected abstract GameplayHandler createGameplayHandler();

    public abstract void load();
    public abstract void enable();
    public abstract void disable();
    public abstract void reload();

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();
        for (ServerPlayer player : onlinePlayers()) {
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
        MinecraftServer server = getServer();
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
        MinecraftServer server = getServer();
        return server != null && ! server.usesAuthentication();
    }

    @Override
    public long getConnectionThrottle() {
        return -1;
    }

    @Override
    public boolean getOnlineMode() {
        MinecraftServer server = getServer();
        return server != null && server.usesAuthentication();
    }

    @Override
    public void shutdown() {
        MinecraftServer server = getServer();
        if (server != null) server.halt(false);
    }

    /**
     * Mod loaders have no permission API of their own, so the permission argument is not
     * consulted and every online player receives the message.
     */
    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int count = 0;
        String coded = messenger != null ? messenger.codedString(message) : message;
        for (ServerPlayer player : onlinePlayers()) {
            player.sendSystemMessage(Component.literal(coded));
            count++;
        }
        return count;
    }

    /**
     * Mod loaders expose no registry of Bukkit-style plugins to look a name up in.
     */
    @Override
    public boolean serverHasPlugin(String plugin) {
        return false;
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (event.getEvent() instanceof CosmicEvent) {
            BaseEventHandler.fireEvent((CosmicEvent) event.getEvent());
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
        MessageUtils.logWarning("Resource pack sending is not implemented on " + getPlatformType() + ".");
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
    public Logger getSLFLogger() {
        return LOGGER;
    }

    public Logger getSlf4jLogger() {
        return LOGGER;
    }

    public static List<ServerPlayer> onlinePlayers() {
        MinecraftServer server = getServer();
        if (server == null) return new ArrayList<>();
        return new ArrayList<>(server.getPlayerList().getPlayers());
    }

    public static ServerPlayer getPlayer(String uuid) {
        MinecraftServer server = getServer();
        if (server == null) return null;
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
