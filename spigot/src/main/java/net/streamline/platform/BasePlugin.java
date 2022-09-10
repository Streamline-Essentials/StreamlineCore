package net.streamline.platform;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.streamline.api.SLAPI;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainConfigHandler;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperCommand;
import net.streamline.api.interfaces.IProperEvent;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.punishments.PunishmentConfig;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.platform.config.SavedProfileConfig;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.profile.SpigotProfiler;
import net.streamline.platform.savables.UserManager;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.users.SavableConsole;
import net.streamline.platform.users.SavablePlayer;
import net.streamline.api.utils.UUIDUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BasePlugin extends JavaPlugin implements IStreamline {
    public static class Runner implements Runnable {
        public Runner() {
            getInstance().getMessenger().logInfo("Task Runner registered!");
        }

        @Override
        public void run() {
            SLAPI.getInstance().getMainScheduler().tick();
        }
    }

    @Getter
    private final PlatformType platformType = PlatformType.SPIGOT;
    @Getter
    private final ServerType serverType = ServerType.BACKEND;

    @Getter
    private TreeMap<String, ModuleCommand> loadedModuleCommands = new TreeMap<>();
    @Getter
    private TreeMap<String, StreamlineCommand> loadedStreamlineCommands = new TreeMap<>();
    @Getter
    private ConcurrentHashMap<String, IProperCommand> properlyRegisteredCommands = new ConcurrentHashMap<>();

    @Getter @Setter
    private StreamlineResourcePack resourcePack;

    @Getter
    private final String version = "${project.version}";
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
    private LuckPerms luckPerms;
    @Getter
    private GeyserHolder geyserHolder;

    public Server getProxy() {
        return getServer();
    }

    @Getter
    private UserManager userManager;
    @Getter
    private Messenger messenger;

    @Setter
    private SpigotProfiler profiler;
    @Setter @Getter
    private SavedProfileConfig profileConfig;

    @Override
    public void onLoad() {
        instance = this;
        userFolder = new File(this.getDataFolder(), "users" + File.separator);
        moduleFolder = new File(this.getDataFolder(), "modules" + File.separator);
        mainCommandsFolder = new File(this.getDataFolder(), commandsFolderChild);
        userFolder.mkdirs();
        moduleFolder.mkdirs();
        mainCommandsFolder.mkdirs();

        this.load();
    }

    @Override
    public void onEnable() {
        userManager = new UserManager();
        messenger = new Messenger();
        slapi = new SLAPI<>(this, userManager, messenger, getDataFolder());
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());

        ratapi = new RATAPI();

        luckPerms = LuckPermsProvider.get();

        profiler = new SpigotProfiler();
        profileConfig = new SavedProfileConfig();

        geyserHolder = new GeyserHolder();

        registerListener(new PlatformListener());
        getProxy().getScheduler().scheduleSyncRepeatingTask(this, new Runner(), 0, 50);

        getInstance().userManager.loadUser(new SavableConsole());

        getProxy().getMessenger().registerOutgoingPluginChannel(this, SLAPI.getApiChannel());
        getProxy().getMessenger().registerIncomingPluginChannel(this, SLAPI.getApiChannel(), new PlatformListener.ProxyMessagingListener());

        this.enable();
    }

    @Override
    public void onDisable() {
        for (StreamlineUser user : getInstance().userManager.getLoadedUsers()) {
            user.saveAll();
        }

        this.disable();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    abstract public void reload();

    public static void registerListener(Listener listener) {
        getInstance().getProxy().getPluginManager().registerEvents(listener, getInstance());
    }

    public static void reloadData() {
        GivenConfigs.getMainConfig().reloadResource();
        GivenConfigs.getMainMessages().reloadResource();
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
        return getInstance().getProxy().getMaxPlayers();
    }

    public boolean hasWhitelist() {
        return getInstance().getProxy().hasWhitelist();
    }

    public void setWhitelist(boolean value) {
        getInstance().getProxy().setWhitelist(value);
    }

    public boolean isWhitelistEnforced() {
        return getInstance().getProxy().isWhitelistEnforced();
    }

    public void setWhitelistEnforced(boolean value) {
        getInstance().getProxy().setWhitelistEnforced(value);
    }

    public WhitelistConfig getWhitelist() {
        return null;
    }

    public PunishmentConfig getPunishmentConfig() {
        return null;
    }

    public @NotNull Set<StreamlinePlayer> getWhitelistedPlayers() {
        Set<StreamlinePlayer> r = new HashSet<>();
        for (OfflinePlayer player : getInstance().getProxy().getWhitelistedPlayers()) {
            StreamlinePlayer p = getInstance().userManager.getOrGetPlayer(player.getUniqueId().toString());
            if (p == null) continue;
            r.add(p);
        }

        return r;
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
        ProperCommand c = (ProperCommand) properlyRegisteredCommands.get(command.getIdentifier());
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
        ProperCommand c = (ProperCommand) properlyRegisteredCommands.get(command.getIdentifier());
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
        return getInstance().getProxy().getConnectionThrottle();
    }

    public @Nullable StreamlinePlayer getSavedPlayer(@NotNull String name) {
        return getInstance().userManager.getOrGetPlayer(getInstance().getUUIDFromName(name));
    }

    public @Nullable StreamlinePlayer getSavedPlayerByUUID(@NotNull String uuid) {
        return getInstance().userManager.getOrGetPlayer(uuid);
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

    public static Player getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
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
        return getInstance().getProxy().getOnlineMode();
    }

    public void shutdown() {
        getInstance().getProxy().shutdown();
    }

    public int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (Player player : onlinePlayers()) {
            if (! player.hasPermission(permission)) continue;
            getInstance().messenger.sendMessage(player, message);
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
            player.chat(message);
        }
    }

    @Override
    public void runAsStrictly(StreamlineUser as, String command) {
        if (as instanceof StreamlineConsole) {
            getInstance().getProxy().dispatchCommand(getInstance().getProxy().getConsoleSender(), command);
        }
        if (as instanceof StreamlinePlayer) {
            if (getInstance().messenger.isCommand(command)) runAsStrictly(as, command.substring("/".length()));
            Player player = getPlayer(as.getUUID());
            if (player == null) return;
            getInstance().getProxy().dispatchCommand(player, command);
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
        if (! (event.getEvent() instanceof Event e)) return;
        getInstance().getProxy().getPluginManager().callEvent(e);
    }

    @Override
    public void fireEvent(StreamlineEvent event) {
        fireEvent(event, true);
    }

    @Override
    public void fireEvent(StreamlineEvent event, boolean async) {
        try {
            fireEvent(new ProperEvent(event, async));
        } catch (Exception e) {
            handleMisSync(event, async);
        }
    }

    @Override
    public void handleMisSync(StreamlineEvent event, boolean async) {
        fireEvent(new ProperEvent(event, ! async));
    }

    @Override
    public List<String> getServerNames() {
        return new ArrayList<>(getProfileConfig().getCachedProfile().getServers().keySet());
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, StreamlineUser player) {
        Player p = getPlayer(player.getUUID());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, String uuid) {
        Player p = getPlayer(uuid);

//        Messenger.getInstance().logInfo("Attempting to send a resource pack to a whitelistedUuid of '" + whitelistedUuid + "'...");

        sendResourcePack(resourcePack, p);
    }

    public void sendResourcePack(StreamlineResourcePack resourcePack, Player player) {
        if (player == null) {
            Messenger.getInstance().logWarning("Tried to send a player a resource pack, but could not find their player!");
            return;
        }

//        Messenger.getInstance().logInfo("Sending resource pack to '" + player.getName() + "'.");

        try {
            if (resourcePack.getHash().length > 0) {
                if (! resourcePack.getPrompt().equals("")) {
                    player.setResourcePack(resourcePack.getUrl(), resourcePack.getHash(), resourcePack.getPrompt(), resourcePack.isForce());
                    return;
                }
                player.setResourcePack(resourcePack.getUrl(), resourcePack.getHash(), resourcePack.isForce());
                return;
            }
            player.setResourcePack(resourcePack.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}