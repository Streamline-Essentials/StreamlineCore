package net.streamline.api.base;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.base.command.CommandException;
import net.streamline.api.base.command.CommandExecutor;
import net.streamline.api.base.command.ModuleCommand;
import net.streamline.api.base.configs.self.MainConfigHandler;
import net.streamline.api.base.configs.self.MainMessagesHandler;
import net.streamline.api.base.modules.ServicesManager;
import net.streamline.api.base.modules.SimpleModuleManager;
import net.streamline.api.base.placeholder.RATAPI;
import net.streamline.api.base.scheduler.StreamlineScheduler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public abstract class BasePlugin extends Plugin implements IPlugin {
    TreeMap<String, ModuleCommand> loadedCommands = new TreeMap<>();

    static String name;
    static String version;
    static BasePlugin instance;
    static RATAPI ratapi;
    static File userFolder;
    static File moduleFolder;
    static File updateFolder;
    static MainConfigHandler mainConfigHandler;
    static MainMessagesHandler mainMessagesHandler;
    static SimpleModuleManager moduleManager;
    static LuckPerms luckPerms;
    static UnsafeValues unsafeValues;
    static Warning.WarningState warningState;
    static StreamlineScheduler scheduler;
    static ServicesManager servicesManager;

    @Override
    public void onEnable() {
        name = "StreamlineAPI";
        version = "${project.version}";
        instance = this;

        ratapi = new RATAPI();
        mainConfigHandler = new MainConfigHandler();
        mainMessagesHandler = new MainMessagesHandler();

        luckPerms = LuckPermsProvider.get();

        userFolder = new File(this.getDataFolder(), "users" + File.separator);
        moduleFolder = new File(this.getDataFolder(), "modules" + File.separator);

        this.enable();
    }

    @Override
    public void onDisable() {
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

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
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

    public SimpleModuleManager getModuleManager() {
        return moduleManager;
    }

    public StreamlineScheduler getScheduler() {
        return scheduler;
    }

    public ServicesManager getServicesManager() {
        return servicesManager;
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

    public static File getUpdateFolder() {
        return updateFolder;
    }

    public static UnsafeValues getUnsafe() {
        return unsafeValues;
    }

    public Warning.WarningState getWarningState() {
        return warningState;
    }

    public List<ProxiedPlayer> onlinePlayers() {
        return new ArrayList<>(getProxy().getPlayers());
    }

    public List<ProxiedPlayer> playersOnServer(String serverName) {
        return new ArrayList<>(getProxy().getServerInfo(serverName).getPlayers());
    }

    public ProxiedPlayer getPlayer(String uuid) {
        for (ProxiedPlayer player : onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return player;
        }

        return null;
    }

    public ProxiedPlayer getPlayer(CommandSender sender) {
        return getProxy().getPlayer(sender.getName());
    }

    public ModuleCommand getModuleCommand(String name) {
        return loadedCommands.get(name);
    }

    public void registerModuleCommand(ModuleCommand command) {
        this.loadedCommands.put(command.getName(), command);
    }

    public static boolean dispatchCommand(@NotNull CommandExecutor sender, @NotNull String commandLine) throws CommandException {
        return getInstance().getProxy().getPluginManager().dispatchCommand(getInstance().getPlayer(sender.getUUID()), commandLine);
    }

    public Map<String, String[]> getCommandAliases() {
        Map<String, String[]> r = new HashMap<>();

        for (ModuleCommand command : loadedCommands.values()) {
            r.put(command.getLabel(), command.getAliases().toArray(new String[0]));
        }

        return r;
    }
}
