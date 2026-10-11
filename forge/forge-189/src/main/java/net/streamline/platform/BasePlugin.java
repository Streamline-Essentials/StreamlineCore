package net.streamline.platform;

import gg.drak.thebase.events.BaseEventHandler;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.modules.LegacyModuleTransformer;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import net.streamline.platform.savables.UserManager;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import net.streamline.libs.slf4j.Logger;
import net.streamline.libs.slf4j.LoggerFactory;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;
import singularity.events.server.ServerStopEvent;
import singularity.interfaces.IProperEvent;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.SupportedPlatformTag;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.TaskManager;
import singularity.utils.MessageUtils;
import singularity.utils.StorageUtils;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Streamline on a Minecraft 1.8.9 Forge client.
 *
 * <p>A 1.8.9 client runs client modules only: it never attaches to a server, integrated or
 * remote, so it has no players, no server commands and nothing to broadcast to. Everything
 * here that a server would answer reports that there is none.</p>
 */
public abstract class BasePlugin implements ISingularityExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger("StreamlineCore");

    /** The game's own logger, which reaches the launcher console and latest.log. */
    private static final org.apache.logging.log4j.Logger GAME_LOGGER = LogManager.getLogger("StreamlineCore");

    private static BasePlugin instance;

    private String version;
    private String folderName;
    private CosmicResourcePack resourcePack;

    private SLAPI<Object, Object, BasePlugin, UserManager, Messenger> slapi;
    private UserManager userManager;
    private Messenger messenger;
    private ConsoleHolder consoleHolder;
    private PlayerInterface playerInterface;

    /** Set by the first full teardown, so a later one does nothing. */
    private boolean shutDown;

    public static BasePlugin getInstance() {
        return instance;
    }

    /** Called once from the mod's constructor. */
    protected void initialize() {
        instance = this;
        LegacyModuleTransformer.install();
        setupProperties();
    }

    /**
     * Brings Streamline up: SLAPI, the managers and the task manager, after which modules
     * load on their own thread. Called once the game has started; later calls do nothing.
     */
    public synchronized void boot() {
        if (slapi != null || shutDown) return;

        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();

        slapi = new SLAPI<>(
                getFolderName(), this, userManager, messenger,
                consoleHolder, playerInterface, BaseModule::new);

        TaskManager.init();

        // External modules load only once the platform reports itself enabled.
        setPlatformAsEnabled();
    }

    /** Takes Streamline down for good. Runs once; later calls do nothing. */
    public synchronized void shutdownStreamline() {
        if (shutDown || slapi == null) return;
        shutDown = true;

        try {
            ServerStopEvent e = new ServerStopEvent().fire();
            if (! e.isCancelled() && e.isSendable()) SLAPI.sendConsoleMessage(e.getMessage());
        } catch (Exception ignored) {
            // Nothing is left to report it to.
        }
        TaskManager.stop();
    }

    private void setupProperties() {
        ConcurrentSkipListMap<String, String> properties = StorageUtils.readProperties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().equals("name")) this.folderName = entry.getValue();
            if (entry.getKey().equals("version")) this.version = entry.getValue();
        }
    }

    public String getFolderName() {
        return folderName != null ? folderName : "StreamlineCore";
    }

    @Override
    public String getVersion() {
        return version;
    }

    public SLAPI<Object, Object, BasePlugin, UserManager, Messenger> getSlapi() {
        return slapi;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public CosmicResourcePack getResourcePack() {
        return resourcePack;
    }

    public void setResourcePack(CosmicResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    /** Logs to the game's log, which is where a 1.8.9 player looks. */
    public static void log(String message) {
        GAME_LOGGER.info(message);
    }

    @Override
    public Set<SupportedPlatformTag> getSupportedPlatformTags() {
        return EnumSet.of(SupportedPlatformTag.MODDED_CLIENT);
    }

    @Override
    public ServerType getServerType() {
        return ServerType.BACKEND;
    }

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        return new ConcurrentSkipListSet<>();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        return new ConcurrentSkipListSet<>();
    }

    @Override
    public ProperCommand createCommand(CosmicCommand command) {
        return new ProperCommand(command);
    }

    @Override
    public int getMaxPlayers() {
        return 1;
    }

    @Override
    public boolean isOfflineMode() {
        return false;
    }

    @Override
    public long getConnectionThrottle() {
        return -1;
    }

    @Override
    public boolean getOnlineMode() {
        return true;
    }

    /** A client does not shut the game down for a module. */
    @Override
    public void shutdown() {
    }

    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        return 0;
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        return false;
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return false;
    }

    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        return new ConcurrentSkipListSet<>();
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
    public void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        sendResourcePack(resourcePack, player.getUuid());
    }

    @Override
    public void sendResourcePack(CosmicResourcePack resourcePack, String uuid) {
        MessageUtils.logWarning("A game client cannot send resource packs.");
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public String getName() {
        return getFolderName();
    }

    @Override
    public java.util.logging.Logger getLoggerLogger() {
        return null;
    }

    @Override
    public Logger getSLFLogger() {
        return LOGGER;
    }
}
