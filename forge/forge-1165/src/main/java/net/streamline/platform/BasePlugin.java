package net.streamline.platform;

import gg.drak.thebase.events.BaseEventHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.events.ProperEvent;
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
    private SLAPI<Object, ServerPlayerEntity, BasePlugin, UserManager, Messenger> slapi;

    @Getter
    private UserManager userManager;

    @Getter
    private Messenger messenger;

    @Getter
    private ConsoleHolder consoleHolder;

    @Getter
    private PlayerInterface playerInterface;

    private static final Logger LOGGER = LoggerFactory.getLogger("StreamlineCore");

    protected void init() {
        instance = this;
        setupProperties();
        load();
    }

    public void onServerEnable() {
        userManager = new UserManager();
        messenger = new Messenger();
        consoleHolder = new ConsoleHolder();
        playerInterface = new PlayerInterface();

        slapi = new SLAPI<Object, ServerPlayerEntity, BasePlugin, UserManager, Messenger>(
                getFolderName(), this, getUserManager(), getMessenger(),
                getConsoleHolder(), getPlayerInterface(), BaseModule::new);
        SLAPI.setBackendHandler(new BackendHandler());

        TaskManager.init();

        enable();
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

    public abstract void load();
    public abstract void enable();
    public abstract void disable();
    public abstract void reload();

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();
        if (ServerLifecycleHooks.getCurrentServer() == null) return players;
        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
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
        if (ServerLifecycleHooks.getCurrentServer() == null) return 20;
        return ServerLifecycleHooks.getCurrentServer().getMaxPlayers();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getOnlinePlayers().forEach(p -> r.add(p.getCurrentName()));
        return r;
    }

    @Override
    public boolean isOfflineMode() {
        return ServerLifecycleHooks.getCurrentServer() != null
                && !ServerLifecycleHooks.getCurrentServer().usesAuthentication();
    }

    @Override
    public long getConnectionThrottle() {
        return -1;
    }

    @Override
    public boolean getOnlineMode() {
        return ServerLifecycleHooks.getCurrentServer() != null
                && ServerLifecycleHooks.getCurrentServer().usesAuthentication();
    }

    @Override
    public void shutdown() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ServerLifecycleHooks.getCurrentServer().halt(false);
        }
    }

    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int count = 0;
        if (ServerLifecycleHooks.getCurrentServer() == null) return count;
        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            player.sendMessage(new StringTextComponent(messenger != null
                    ? messenger.codedString(message) : message), player.getUUID());
            count++;
        }
        return count;
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        return net.minecraftforge.fml.ModList.get().isLoaded(plugin.toLowerCase());
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
        MessageUtils.logWarning("Resource pack sending not fully implemented for Forge 1.16.5 yet.");
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

    public static List<ServerPlayerEntity> onlinePlayers() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return new ArrayList<>();
        return new ArrayList<>(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers());
    }

    public static ServerPlayerEntity getPlayer(String uuid) {
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        try {
            return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
