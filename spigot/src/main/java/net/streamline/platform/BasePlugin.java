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
import net.streamline.base.runnables.PlayerTeleporter;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.handlers.BackendHandler;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.savables.ConsoleHolder;
import net.streamline.platform.savables.PlayerInterface;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
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

public abstract class BasePlugin extends BetterPlugin implements ISingularityExtension {
    @Getter
    private final PlatformType platformType = PlatformType.SPIGOT;
    @Getter
    private final ServerType serverType = ServerType.BACKEND;

    @Getter @Setter
    private CosmicResourcePack resourcePack;

    @Getter
    private String version;
    @Getter
    private String folderName;
    @Getter
    private static BasePlugin instance;
    @Getter
    private SLAPI<CommandSender, Player, BasePlugin, UserManager, Messenger> slapi;
    @Getter
    private SLAPIB slapiB;

    public Server getProxy() {
        return getServer();
    }

    @Getter
    private UserManager userManager;
    @Getter
    private Messenger messenger;
    @Getter
    private ConsoleHolder consoleHolder;
    @Getter
    private PlayerInterface playerInterface;

    @Getter @Setter
    private static PlayerChecker playerChecker;

    @Getter @Setter
    private static TaskScheduler scheduler;

    @Override
    public void onBaseConstruct() {
        instance = this;

        scheduler = UniversalScheduler.getScheduler(this);

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

        getProxy().getMessenger().registerOutgoingPluginChannel(this, SLAPI.getApiChannel());
        getProxy().getMessenger().registerIncomingPluginChannel(this, SLAPI.getApiChannel(), new PlatformListener.ProxyMessagingListener());

        playerChecker = new PlayerChecker();
        PlayerTeleporter.init();

        this.enable();
        registerListener(new PlatformListener());
    }

    @Override
    public void onBaseDisable() {
        PlayerTeleporter.stopInstance();

        UserUtils.syncAllUsers();
        UuidManager.getUuids().forEach(UuidInfo::save);

        this.disable();
        fireStopEvent();

        TaskManager.stop();
    }

    public void fireStopEvent() {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (!e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    abstract public void reload();

    @Override
    public @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<CosmicPlayer> players = new ConcurrentSkipListSet<>();

        for (Player player : onlinePlayers()) {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) {
                players.add(getUserManager().getOrCreatePlayer(player));
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
        return getInstance().getProxy().getMaxPlayers();
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
        return getInstance().getProxy().getConnectionThrottle();
    }

    public static List<Player> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getOnlinePlayers());
    }

    public static List<Player> playersOnServer(String serverName) {
        return new ArrayList<>(/*getInstance().getProxy().gets(serverName).getPlayers()*/);
    }

    public static Player getPlayer(String uuid) {
        for (Player player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    public static Optional<Player> getPlayerByName(String name) {
        return Optional.ofNullable(getInstance().getProxy().getPlayer(name));
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

    public static Player getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
    }

    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getOnlineMode();
    }

    @Override
    public void shutdown() {
        getInstance().getProxy().shutdown();
    }

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
        if (!(event.getEvent() instanceof Event)) return;
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
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        //        for (String server : getInstance().getProxy().getServerNames()) {
        //            r.add(server);
        //        }

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

        //        getMessenger().logInfo("Attempting to send a resource pack to a uuid of '" + whitelistedUuid + "'...");

        sendResourcePack(resourcePack, p);
    }

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

    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }

    @Getter
    @Setter
    private static boolean commandsNeedToBeSynced = false;

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
                commandMap.register(command.getLabel(), command.getParent().getBase(), command);

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
                    @SuppressWarnings("unchecked") final Map<String, Command> knownCommands = (java.util.Map<String, Command>) knownCommandsField.get(commandMap);

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

    public static ConcurrentSkipListMap<String, Player> getPlayersByUUID() {
        ConcurrentSkipListMap<String, Player> map = new ConcurrentSkipListMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            map.put(player.getUniqueId().toString(), player);
        }
        return map;
    }

    public static Command getBukkitCommand(String name) {
        return commandMap.getCommand(name);
    }
}