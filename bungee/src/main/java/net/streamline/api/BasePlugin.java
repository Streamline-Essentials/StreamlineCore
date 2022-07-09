package net.streamline.api;

import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.command.*;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import net.streamline.base.configs.MainConfigHandler;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavableConsole;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.listeners.BaseListener;
import net.streamline.base.timers.UserSaveTimer;
import net.streamline.utils.MessagingUtils;
import net.streamline.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class BasePlugin extends Plugin {
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

    @Override
    public void onEnable() {
        name = "StreamlineAPI";
        version = "${project.version}";
        instance = this;

        ratapi = new RATAPI();
        mainConfigHandler = new MainConfigHandler();
        mainMessagesHandler = new MainMessagesHandler();

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
        getInstance().getProxy().getScheduler().schedule(this, new Runner(), 0, 50, TimeUnit.MILLISECONDS);

        UserManager.loadUser(new SavableConsole());

        try {
            ModuleManager.unJarAll();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new UserSaveTimer();

        this.enable();
    }

    @Override
    public void onDisable() {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            user.saveAll();
        }

        this.disable();
    }

    @Override
    public void onLoad() {
        this.load();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    abstract public void reload();

    public static void fireEvent(Event event) {
        getInstance().getProxy().getPluginManager().callEvent(event);
    }

    public static void registerListener(Listener listener) {
        getInstance().getProxy().getPluginManager().registerListener(getInstance(), listener);
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

        for (ProxiedPlayer player : onlinePlayers()) {
            players.add(UserManager.getOrGetPlayer(player));
        }

        return players;
    }

    public static int getMaxPlayers() {
        return getInstance().getProxy().getConfig().getPlayerLimit();
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
        getInstance().getProxy().getPluginManager().unregisterCommands(getInstance());
    }

    public static void registerProperCommand(Command command) {
        getInstance().getProxy().getPluginManager().registerCommand(getInstance(), command);
    }

    public static void registerStreamlineCommand(StreamlineCommand command) {
        getInstance().getProxy().getPluginManager().registerCommand(getInstance(), command);
        loadedStreamlineCommands.put(command.getName(), command);
    }

    public static void unregisterStreamlineCommand(StreamlineCommand command) {
        getInstance().getProxy().getPluginManager().unregisterCommand(command);
        loadedStreamlineCommands.remove(command.getName());
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

        return r;
    }

    public static @NotNull List<ServerInfo> getServers() {
        return new ArrayList<>(getInstance().getProxy().getServers().values());
    }

    public static boolean unloadServer(@NotNull String name, boolean save) {
        return false;
    }

    public static boolean unloadServer(@NotNull Server server, boolean save) {
        return false;
    }

    public static @Nullable ServerInfo getServer(@NotNull String name) {
        return getInstance().getProxy().getServerInfo(name);
    }

    public static @Nullable Server getServer(@NotNull UUID uid) {
        return null;
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

    public static long getConnectionThrottle() {
        return getInstance().getProxy().getConfig().getThrottle();
    }

    public static @Nullable SavablePlayer getSavedPlayer(@NotNull String name) {
        return UserManager.getOrGetPlayer(getUUIDFromName(name));
    }

    public static @Nullable SavablePlayer getSavedPlayerByUUID(@NotNull String uuid) {
        return UserManager.getOrGetPlayer(uuid);
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

    public static String getUUIDFromName(String name) {
        return UUIDUtils.getCachedUUID(name);
    }

    public static String getNameFromUUID(String uuid) {
        return UUIDUtils.getCachedName(uuid);
    }

    public static @Nullable ProxiedPlayer getPlayerExact(@NotNull String name) {
        return getPlayer(getUUIDFromName(name));
    }

    public static @NotNull List<ProxiedPlayer> matchPlayer(@NotNull String name) {
        ProxiedPlayer player = getPlayerExact(name);
        if (player == null) return new ArrayList<>();
        return List.of(player);
    }

    public static @Nullable ProxiedPlayer getPlayer(@NotNull UUID id) {
        return getPlayer(id.toString());
    }

    public static ProxiedPlayer getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
    }

    public static ModuleCommand getModuleCommand(@NotNull String name) {
        return loadedModuleCommands.get(name);
    }

    public static void savePlayers() {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            user.saveAll();
        }
    }

    public static void registerModuleCommand(ModuleCommand command) {
        getInstance().getProxy().getPluginManager().registerCommand(getInstance(), command);
        loadedModuleCommands.put(command.getName(), command);
    }

    public static void unregisterModuleCommand(ModuleCommand command) {
        getInstance().getProxy().getPluginManager().unregisterCommand(command);
        loadedModuleCommands.remove(command.getName());
    }

//    public static boolean dispatchCommand(@NotNull ICommandSender sender, @NotNull String commandLine) throws CommandException {
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
        return getInstance().getProxy().getConfig().isOnlineMode();
    }

    public static void shutdown() {
        getInstance().getProxy().stop();
    }

    public static int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (ProxiedPlayer player : onlinePlayers()) {
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


//    public static @NotNull IConsoleCommandSender getConsoleSender() {
//        return (IConsoleCommandSender) getInstance().getProxy().getConsole();
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
}