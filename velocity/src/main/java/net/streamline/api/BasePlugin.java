package net.streamline.api;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.command.ProperCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavableConsole;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import net.streamline.base.configs.MainConfigHandler;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.base.listeners.BaseListener;
import net.streamline.base.timers.UserSaveTimer;
import net.streamline.utils.MessagingUtils;
import net.streamline.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class BasePlugin {
    public static class Runner implements Runnable {
        public Runner() {
            MessagingUtils.logInfo("Task Runner registered!");
        }

        @Override
        public void run() {
            getMainScheduler().tick();
        }
    }

    @Getter
    static TreeMap<String, ModuleCommand> loadedModuleCommands = new TreeMap<>();
    @Getter
    static TreeMap<String, StreamlineCommand> loadedStreamlineCommands = new TreeMap<>();
    @Getter
    static ConcurrentHashMap<String, ProperCommand> properlyRegisteredCommands = new ConcurrentHashMap<>();

    static String name;
    static String version;
    static BasePlugin instance;
    static RATAPI ratapi;
    static File userFolder;
    static File moduleFolder;
    static File updateFolder;
    static File mainCommandsFolder;
    static String commandsFolderChild = "commands" + File.separator;
    static MainConfigHandler mainConfigHandler;
    static MainMessagesHandler mainMessagesHandler;
    static LuckPerms luckPerms;
    static ModuleTaskManager moduleScheduler;
    @Getter
    static TaskManager mainScheduler;
    @Getter
    static GeyserHolder geyserHolder;

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

        mainConfigHandler = new MainConfigHandler();
        mainMessagesHandler = new MainMessagesHandler();

        ratapi = new RATAPI();

        moduleScheduler = new ModuleTaskManager();
        mainScheduler = new TaskManager();

        luckPerms = LuckPermsProvider.get();

        userFolder = new File(this.getDataFolder(), "users" + File.separator);
        moduleFolder = new File(this.getDataFolder(), "modules" + File.separator);
        mainCommandsFolder = new File(this.getDataFolder(), commandsFolderChild);
        userFolder.mkdirs();
        moduleFolder.mkdirs();
        mainCommandsFolder.mkdirs();

        geyserHolder = new GeyserHolder();

        registerListener(new BaseListener());
        getInstance().getProxy().getScheduler().buildTask(this, new Runner())
                .delay(0, TimeUnit.MILLISECONDS)
                .repeat(50, TimeUnit.MILLISECONDS)
                .schedule()
        ;

        UserManager.loadUser(new SavableConsole());

        this.enable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            user.saveAll();
        }

        this.disable();
    }

    public void onLoad() {
        this.load();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    abstract public void reload();

    public static void fireEvent(Object event) {
        getInstance().getProxy().getEventManager().fire(event);
    }

    public static void registerListener(Object listener) {
        getInstance().getProxy().getEventManager().register(getInstance(), listener);
    }

    public static void reloadData() {
        mainConfigHandler.reloadResource();
        mainMessagesHandler.reloadResource();
        for (SavableUser user : UserManager.getLoadedUsers()) {
            user.saveAll();
            user.reload();
        }
    }

    public static @NotNull String getName() {
        return name;
    }

    public static @NotNull String getVersion() {
        return version;
    }

    public static @NotNull Collection<SavablePlayer> getOnlinePlayers() {
        List<SavablePlayer> players = new ArrayList<>();

        for (Player player : onlinePlayers()) {
            players.add(UserManager.getOrGetPlayer(player));
        }

        return players;
    }

    public static int getMaxPlayers() {
        return getInstance().getProxy().getConfiguration().getShowMaxPlayers();
    }

    public static @NotNull String getResourcePack() {
        return "";
    }

    public static @NotNull String getResourcePackHash() {
        return "";
    }

    public static @NotNull String getResourcePackPrompt() {
        return "";
    }

    public static boolean isResourcePackRequired() {
        return false;
    }

    public static boolean hasWhitelist() {
        return false;
    }

    public static void setWhitelist(boolean value) {

    }

    public static boolean isWhitelistEnforced() {
        return false;
    }

    public static void setWhitelistEnforced(boolean value) {

    }

    public static @NotNull Set<SavablePlayer> getWhitelistedPlayers() {
        return null;
    }

    public static void reloadWhitelist() {

    }

    public static int broadcastMessage(@NotNull String message) {
        return 0;
    }

    public static void flushCommands() {
        getProperlyRegisteredCommands().forEach((command, properCommand) -> getInstance().getProxy().getCommandManager().unregister(getCommandMeta(properCommand.getParent())));
    }

    public static CommandMeta getCommandMeta(StreamlineCommand command) {
        return getInstance().getProxy().getCommandManager().metaBuilder(command.getBase())
                .aliases(command.getAliases())
                .plugin(getInstance())
                .build();
    }

    public static void registerStreamlineCommand(StreamlineCommand command) {
        if (properlyRegisteredCommands.containsKey(command.getIdentifier())) {
            MessagingUtils.logWarning("Command with identifier '" + command.getIdentifier() + "' is already registered!");
            return;
        }
        ProperCommand properCommand = new ProperCommand(command);
        getInstance().getProxy().getCommandManager().register(getCommandMeta(command), properCommand);
        properlyRegisteredCommands.put(command.getIdentifier(), properCommand);
        loadedStreamlineCommands.put(command.getBase(), command);
    }

    public static void unregisterStreamlineCommand(StreamlineCommand command) {
        ProperCommand c = properlyRegisteredCommands.get(command.getIdentifier());
        if (c == null) return;
        getInstance().getProxy().getCommandManager().unregister(getCommandMeta(c.getParent()));
        properlyRegisteredCommands.remove(command.getIdentifier());
        loadedStreamlineCommands.remove(command.getBase());
    }

    public static void registerModuleCommand(ModuleCommand command) {
        if (properlyRegisteredCommands.containsKey(command.getIdentifier())) {
            MessagingUtils.logWarning(command.getOwningModule(), "Command with identifier '" + command.getIdentifier() + "' is already registered!");
            return;
        }
        ProperCommand properCommand = new ProperCommand(command);
        getInstance().getProxy().getCommandManager().register(getCommandMeta(command), properCommand);
        properlyRegisteredCommands.put(command.getIdentifier(), properCommand);
        loadedModuleCommands.put(command.getBase(), command);
    }

    public static void unregisterModuleCommand(ModuleCommand command) {
        ProperCommand c = properlyRegisteredCommands.get(command.getIdentifier());
        if (c == null) return;
        getInstance().getProxy().getCommandManager().unregister(getCommandMeta(c.getParent()));
        properlyRegisteredCommands.remove(command.getIdentifier());
        loadedModuleCommands.remove(command.getBase());
    }

    public static BasePlugin getInstance() {
        return instance;
    }

    public static RATAPI getRATAPI() {
        return ratapi;
    }

    public static MainConfigHandler getMainConfig() {
        return mainConfigHandler;
    }

    public static MainMessagesHandler getMainMessages() {
        return mainMessagesHandler;
    }


    public static ModuleTaskManager getModuleScheduler() {
        return moduleScheduler;
    }


    public static List<String> getOnlinePlayerNames() {
        List<String> r = new ArrayList<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getName());
        });

//        r.add(UserManager.getConsole().latestName);

        return r;
    }

    public static @NotNull List<ServerInfo> getServers() {
        List<ServerInfo> r = new ArrayList<>();
        getInstance().getProxy().getAllServers().forEach(a -> r.add(a.getServerInfo()));
        return r;
    }

    public static boolean unloadServer(@NotNull String name, boolean save) {
        return false;
    }

    public static boolean unloadServer(@NotNull RegisteredServer server, boolean save) {
        return false;
    }

    public static @Nullable ServerInfo getServerInfo(@NotNull String name) {
        Optional<RegisteredServer> thing = getInstance().getProxy().getServer(name);
        if (thing.isEmpty()) return null;
        return thing.get().getServerInfo();
    }

    public static @Nullable RegisteredServer getServer(@NotNull String name) {
        Optional<RegisteredServer> thing = getInstance().getProxy().getServer(name);
        if (thing.isEmpty()) return null;
        return thing.get();
    }

    public static @Nullable RegisteredServer getServer(@NotNull UUID uid) {
        return null;
    }

    public static List<String> getServerNames() {
        List<String> r = new ArrayList<>();

        getServers().forEach(a -> {
            r.add(a.getName());
        });

        return r;
    }

    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public static File getUserFolder() {
        return userFolder;
    }

    public static File getModuleFolder() {
        return moduleFolder;
    }

    public static File getMainCommandsFolder() {
        return mainCommandsFolder;
    }

    public static String getCommandsFolderChild() {
        return commandsFolderChild;
    }

    public static @NotNull String getUpdateFolder() {
        return updateFolder.getPath();
    }

    public static @NotNull File getUpdateFolderFile() {
        return updateFolder;
    }

//    public static long getConnectionThrottle() {
//        return getInstance().getProxy().getConfiguration().getco();
//    }

    public static @Nullable SavablePlayer getSavedPlayer(@NotNull String name) {
        return UserManager.getOrGetPlayer(getUUIDFromName(name));
    }

    public static @Nullable SavablePlayer getSavedPlayerByUUID(@NotNull String uuid) {
        return UserManager.getOrGetPlayer(uuid);
    }

    public static List<Player> onlinePlayers() {
        return new ArrayList<>(getInstance().getProxy().getAllPlayers());
    }

    public static List<Player> playersOnServer(String serverName) {
        Optional<RegisteredServer> thing = getInstance().getProxy().getServer(serverName);
        if (thing.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(thing.get().getPlayersConnected());
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

    public static String getUUIDFromName(String name) {
        if (getPlayerByName(name).isPresent()) return getPlayerByName(name).get().getUniqueId().toString();
        try {
            return UUIDUtils.getCachedUUID(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getNameFromUUID(String uuid) {
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
        Optional<Player> r = getInstance().getProxy().getPlayer(UserManager.getUsername(sender));
        if (r.isEmpty()) return null;
        return r.get();
    }

    public static ModuleCommand getModuleCommand(@NotNull String name) {
        return loadedModuleCommands.get(name);
    }

    public static void savePlayers() {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            user.saveAll();
        }
    }

//    public static boolean dispatchCommand(@NotNull ICommandSource sender, @NotNull String commandLine) throws CommandException {
//        return getInstance().getProxy().getPluginManager().dispatchCommand(getInstance().getPlayer(sender.getUUID()), commandLine);
//    }

    public static Map<String, String[]> getCommandAliases() {
        Map<String, String[]> r = new HashMap<>();

        for (ModuleCommand command : loadedModuleCommands.values()) {
            r.put(command.getBase(), command.getAliases());
        }

        return r;
    }

    public static boolean getOnlineMode() {
        return getInstance().getProxy().getConfiguration().isOnlineMode();
    }

    public static void shutdown() {
        getInstance().getProxy().shutdown();
    }

    public static int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (Player player : onlinePlayers()) {
            if (! player.hasPermission(permission)) continue;
            MessagingUtils.sendMessage(player, message);
            people ++;
        }

        return people;
    }

    public static @NotNull SavablePlayer getOfflinePlayer(@NotNull String name) {
        return UserManager.getOrGetPlayer(getUUIDFromName(name));
    }

    public static @NotNull SavablePlayer getOfflinePlayer(@NotNull UUID id) {
        return UserManager.getOrGetPlayer(id.toString());
    }

    public static @NotNull SavablePlayer createPlayerProfile(@Nullable UUID uniqueId, @Nullable String name) {
        if (uniqueId != null) {
            return createPlayerProfile(uniqueId);
        } else {
            return createPlayerProfile(name);
        }
    }

    public static @NotNull SavablePlayer createPlayerProfile(@NotNull UUID uniqueId) {
        return (SavablePlayer) UserManager.loadUser(new SavablePlayer(uniqueId.toString()));
    }

    public static @NotNull SavablePlayer createPlayerProfile(@NotNull String name) {
        return (SavablePlayer) UserManager.loadUser(new SavablePlayer(getUUIDFromName(name)));
    }

    public static @NotNull Set<String> getIPBans() {
        return null;
    }

    public static void banIP(@NotNull String address) {

    }

    public static void unbanIP(@NotNull String address) {

    }

    public static @NotNull Set<SavablePlayer> getBannedPlayers() {
        return null;
    }


//    public static @NotNull IConsoleCommandSource getConsoleSender() {
//        return (IConsoleCommandSource) getInstance().getProxy().getConsole();
//    }

//    public static @NotNull BanList getBanList(BanList.@NotNull Type type) {
//        return null;
//    }

    public static @NotNull Set<SavablePlayer> getOperators() {
        return null;
    }

    public static @NotNull SavablePlayer[] getOfflinePlayers() {
        return new SavablePlayer[0];
    }

    public static boolean isPrimaryThread() {
        return false;
    }

    public static @NotNull String getMotd() {
        return "";
    }

    public static @Nullable String getShutdownMessage() {
        return null;
    }

    public InputStream getResourceAsStream(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    public static boolean hasPermission(SavableUser user, String permission) {
        Player player = getPlayer(user.uuid);
        if (player == null) return false;
        return player.hasPermission(permission);
    }

    public static void chatAs(SavableUser as, String message) {
        if (as instanceof SavableConsole) {
            runAs(as, message);
        }
        if (as instanceof SavablePlayer) {
            if (MessagingUtils.isCommand(message)) runAs(as, message.substring("/".length()));
            Player player = getPlayer(as.uuid);
            if (player == null) return;
            player.spoofChatInput(message);
        }
    }

    public static void runAs(SavableUser as, String command) {
        if (as instanceof SavableConsole) {
            getInstance().getProxy().getCommandManager().executeAsync(getInstance().getProxy().getConsoleCommandSource(), command);
        }
        if (as instanceof SavablePlayer) {
            if (MessagingUtils.isCommand(command)) runAs(as, command.substring("/".length()));
            Player player = getPlayer(as.uuid);
            if (player == null) return;
            getInstance().getProxy().getCommandManager().executeAsync(player, command);
        }
    }
}
