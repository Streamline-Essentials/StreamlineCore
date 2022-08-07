package net.streamline.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.streamline.api.SLAPI;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainConfigHandler;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.interfaces.IProperEvent;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import net.streamline.api.utils.UUIDUtils;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.config.SavedProfileConfig;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.profile.SpigotProfiler;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.users.SavableConsole;
import net.streamline.platform.users.SavablePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class BasePlugin implements IStreamline {
    public static class Runner implements Runnable {
        public Runner() {
            getInstance().getMessenger().logInfo("Task Runner registered!");
        }

        @Override
        public void run() {
            getInstance().getMainScheduler().tick();
        }
    }

    @Getter
    private TreeMap<String, ModuleCommand> loadedModuleCommands = new TreeMap<>();
    @Getter
    private TreeMap<String, StreamlineCommand> loadedStreamlineCommands = new TreeMap<>();
    @Getter
    private ConcurrentHashMap<String, ProperCommand> properlyRegisteredCommands = new ConcurrentHashMap<>();

    @Getter
    private String name;
    @Getter
    private String version;
    @Getter
    private static BasePlugin instance;
    private RATAPI ratapi;
    @Getter
    private SLAPI<BasePlugin, UserManager, Messenger> slapi;
    @Getter
    private File userFolder;
    @Getter
    private File moduleFolder;
    @Getter
    private File updateFolder;
    @Getter
    private File mainCommandsFolder;
    @Getter
    private String commandsFolderChild = "commands" + File.separator;
    @Getter
    private MainConfigHandler mainConfig;
    @Getter
    private MainMessagesHandler mainMessages;
    @Getter
    private LuckPerms luckPerms;
    @Getter
    private ModuleTaskManager moduleScheduler;
    @Getter
    private TaskManager mainScheduler;
    @Getter
    private GeyserHolder geyserHolder;

    @Getter
    private UserManager userManager;
    @Getter
    private Messenger messenger;

    @Setter
    private SpigotProfiler profiler;
    @Setter @Getter
    private SavedProfileConfig profileConfig;

    @Getter @Setter
    private StreamlineResourcePack resourcePack;

    @Getter
    private final ProxyServer proxy;
    @Getter
    private final Logger logger;
    @Getter
    private final File dataFolder;

    public BasePlugin(ProxyServer s, Logger l, Path dd) {
        this.proxy = s;
        this.logger = l;
        this.dataFolder = dd.toFile();
        onLoad();
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        name = "StreamlineAPI";
        version = "${project.version}";
        instance = this;
        userManager = new UserManager();
        messenger = new Messenger();
        slapi = new SLAPI<>(this, userManager, messenger, getDataFolder());
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());

        mainConfig = new MainConfigHandler();
        mainMessages = new MainMessagesHandler();

        ratapi = new RATAPI();

        moduleScheduler = new ModuleTaskManager();
        mainScheduler = new TaskManager();

        luckPerms = LuckPermsProvider.get();

        profiler = new SpigotProfiler();
        profileConfig = new SavedProfileConfig();

        userFolder = new File(this.getDataFolder(), "users" + File.separator);
        moduleFolder = new File(this.getDataFolder(), "modules" + File.separator);
        mainCommandsFolder = new File(this.getDataFolder(), commandsFolderChild);
        userFolder.mkdirs();
        moduleFolder.mkdirs();
        mainCommandsFolder.mkdirs();

        geyserHolder = new GeyserHolder();

        registerListener(new PlatformListener());
        getProxy().getScheduler().buildTask(this, new Runner()).repeat(50, TimeUnit.MILLISECONDS).schedule();

        getInstance().userManager.loadUser(new SavableConsole());

        getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.enable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        for (StreamlineUser user : getInstance().userManager.getLoadedUsers()) {
            user.saveAll();
        }

        getProxy().getChannelRegistrar().unregister(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.disable();
    }

    public void onLoad() {
        this.load();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    abstract public void reload();

    public static void registerListener(Object listener) {
        getInstance().getProxy().getEventManager().register(getInstance(), listener);
    }

    public static void reloadData() {
        getInstance().getMainConfig().reloadResource();
        getInstance().getMainMessages().reloadResource();
        for (StreamlineUser user : getInstance().userManager.getLoadedUsers()) {
            user.saveAll();
            user.reload();
        }
    }

    @Override
    public @NotNull Collection<StreamlinePlayer> getOnlinePlayers() {
        List<StreamlinePlayer> players = new ArrayList<>();

        for (Player player : onlinePlayers()) {
            players.add(getInstance().userManager.getOrGetPlayer(player));
        }

        return players;
    }

    public int getMaxPlayers() {
        return getInstance().getProxy().getConfiguration().getShowMaxPlayers();
    }

    public boolean hasWhitelist() {
        return false;
    }

    public void setWhitelist(boolean value) {

    }

    public boolean isWhitelistEnforced() {
        return false;
    }

    public void setWhitelistEnforced(boolean value) {

    }

    public @NotNull Set<StreamlinePlayer> getWhitelistedPlayers() {
        return new HashSet<>();
    }

    public void reloadWhitelist() {

    }

    public int broadcastMessage(@NotNull String message) {
        return 0;
    }

    public void flushCommands() {
        getProperlyRegisteredCommands().forEach((command, properCommand) -> properCommand.unregister());
    }

    public static void registerProperCommand(ProperCommand command) {
        command.register();
    }

    public void registerStreamlineCommand(StreamlineCommand command) {
        if (properlyRegisteredCommands.containsKey(command.getIdentifier())) {
            getInstance().messenger.logWarning("Command with identifier '" + command.getIdentifier() + "' is already registered!");
            return;
        }
        ProperCommand properCommand = new ProperCommand(command);
        properCommand.register();
        properlyRegisteredCommands.put(command.getIdentifier(), properCommand);
        loadedStreamlineCommands.put(command.getBase(), command);
    }

    public void unregisterStreamlineCommand(StreamlineCommand command) {
        ProperCommand c = properlyRegisteredCommands.get(command.getIdentifier());
        if (c == null) return;
        c.unregister();
        properlyRegisteredCommands.remove(command.getIdentifier());
        loadedStreamlineCommands.remove(command.getBase());
    }

    public void registerModuleCommand(ModuleCommand command) {
        if (properlyRegisteredCommands.containsKey(command.getIdentifier())) {
            getInstance().messenger.logWarning(command.getOwningModule(), "Command with identifier '" + command.getIdentifier() + "' is already registered!");
            return;
        }
        ProperCommand properCommand = new ProperCommand(command);
        properCommand.register();
        properlyRegisteredCommands.put(command.getIdentifier(), properCommand);
        loadedModuleCommands.put(command.getBase(), command);
    }

    public void unregisterModuleCommand(ModuleCommand command) {
        ProperCommand c = properlyRegisteredCommands.get(command.getIdentifier());
        if (c == null) return;
        c.unregister();
        properlyRegisteredCommands.remove(command.getIdentifier());
        loadedModuleCommands.remove(command.getBase());
    }

    @Override
    public RATAPI getRATAPI() {
        return ratapi;
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        List<String> r = new ArrayList<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getName());
        });

//        r.add(getInstance().userManager.getConsole().latestName);

        return r;
    }

    public @NotNull String getUpdateFolderPath() {
        return updateFolder.getPath();
    }

    public long getConnectionThrottle() {
        return getInstance().getProxy().getConfiguration().getCompressionThreshold();
    }

    public @Nullable StreamlinePlayer getSavedPlayer(@NotNull String name) {
        return getInstance().userManager.getOrGetPlayer(getInstance().getUUIDFromName(name));
    }

    public @Nullable StreamlinePlayer getSavedPlayerByUUID(@NotNull String uuid) {
        return getInstance().userManager.getOrGetPlayer(uuid);
    }

    public static List<Player> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getAllPlayers());
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
        return getInstance().getProxy().getPlayer(name);
    }

    @Override
    public String getUUIDFromName(String name) {
        if (getPlayerByName(name).isPresent()) return getPlayerByName(name).get().getUniqueId().toString();
        try {
            return UUIDUtils.getCachedUUID(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getNameFromUUID(String uuid) {
        return UUIDUtils.getCachedName(uuid);
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
        Optional<Player> player = getInstance().getProxy().getPlayer(UserManager.getInstance().getUsername(sender));
        if (player.isEmpty()) return null;
        return player.get();
    }

    public ModuleCommand getModuleCommand(@NotNull String name) {
        return loadedModuleCommands.get(name);
    }

    public void savePlayers() {
        for (StreamlineUser user : getInstance().userManager.getLoadedUsers()) {
            user.saveAll();
        }
    }

    public Map<String, String[]> getCommandAliases() {
        Map<String, String[]> r = new HashMap<>();

        for (ModuleCommand command : loadedModuleCommands.values()) {
            r.put(command.getBase(), command.getAliases());
        }

        return r;
    }

    public boolean getOnlineMode() {
        return getInstance().getProxy().getConfiguration().isOnlineMode();
    }

    public void shutdown() {
        getInstance().getProxy().shutdown();
    }

    public int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (Player player : onlinePlayers()) {
            if (! player.hasPermission(permission)) continue;
            getInstance().getMessenger().sendMessage(player, message);
            people ++;
        }

        return people;
    }

    public @NotNull StreamlinePlayer getOfflinePlayer(@NotNull String name) {
        return (StreamlinePlayer) getInstance().getUserManager().getOrGetOrGetUser(getInstance().getUUIDFromName(name));
    }

    public @NotNull StreamlinePlayer getOfflinePlayer(@NotNull UUID id) {
        return (StreamlinePlayer) getInstance().userManager.getOrGetOrGetUser(id.toString());
    }

    public @NotNull StreamlinePlayer createPlayerProfile(@Nullable UUID uniqueId, @Nullable String name) {
        if (uniqueId != null) {
            return createPlayerProfile(uniqueId);
        } else {
            return createPlayerProfile(name);
        }
    }

    public @NotNull StreamlinePlayer createPlayerProfile(@NotNull UUID uniqueId) {
        return (StreamlinePlayer) getInstance().userManager.loadUser(new SavablePlayer(uniqueId.toString()));
    }

    public @NotNull StreamlinePlayer createPlayerProfile(@NotNull String name) {
        return (StreamlinePlayer) getInstance().userManager.loadUser(new SavablePlayer(getInstance().getUUIDFromName(name)));
    }

    public @NotNull Set<String> getIPBans() {
        return null;
    }

    public void banIP(@NotNull String address) {

    }

    public void unbanIP(@NotNull String address) {

    }

    public @NotNull Set<StreamlinePlayer> getBannedPlayers() {
        return null;
    }

    public @NotNull Set<StreamlinePlayer> getOperators() {
        return null;
    }

    public @NotNull StreamlinePlayer[] getOfflinePlayers() {
        return new StreamlinePlayer[0];
    }

    public boolean isPrimaryThread() {
        return false;
    }

    public @NotNull String getMotd() {
        return "";
    }

    public @Nullable String getShutdownMessage() {
        return null;
    }

    @Override
    public boolean hasPermission(StreamlineUser user, String permission) {
        Player player = getPlayer(user.getUUID());
        if (player == null) return false;
        return player.hasPermission(permission);
    }

    @Override
    public void chatAs(StreamlineUser as, String message) {
        if (as instanceof StreamlineConsole) {
            runAsStrictly(as, message);
        }
        if (as instanceof StreamlinePlayer) {
            if (getInstance().messenger.isCommand(message)) runAsStrictly(as, message.substring("/".length()));
            Player player = getPlayer(as.getUUID());
            if (player == null) return;
            player.spoofChatInput(message);
        }
    }

    @Override
    public void runAsStrictly(StreamlineUser as, String command) {
        if (as instanceof StreamlineConsole) {
            getInstance().getProxy().getCommandManager().executeAsync(getInstance().getProxy().getConsoleCommandSource(), command);
        }
        if (as instanceof StreamlinePlayer) {
            if (getInstance().messenger.isCommand(command)) runAsStrictly(as, command.substring("/".length()));
            Player player = getPlayer(as.getUUID());
            if (player == null) return;
            getInstance().getProxy().getCommandManager().executeAsync(player, command);
        }
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin) != null;
    }

    public StreamlineServerInfo getStreamlineServer(String server) {
        return getProfileConfig().getServerInfo(server);
    }

    public void setStreamlineServer(StreamlineServerInfo serverInfo) {
        getInstance().getProfileConfig().updateServerInfo(serverInfo);
    }

    @Override
    public StreamlineProfiler getProfiler() {
        return profiler;
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    @Override
    public void ensureApiChannel(String apiChannel) {
        // do nothing.
    }

    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (! (event.getEvent() instanceof CompletableFuture<?> e)) return;
        getInstance().getProxy().getEventManager().fire(e);
    }

    @Override
    public void fireEvent(StreamlineEvent event) {
        fireEvent(new ProperEvent(event));
    }

    @Override
    public List<String> getServerNames() {
        return null;
    }
}
