package net.streamline.api.base;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.base.command.ModuleCommand;
import net.streamline.api.base.configs.self.MainConfigHandler;
import net.streamline.api.base.configs.self.MainMessagesHandler;
import net.streamline.api.base.placeholder.RATAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public abstract class BasePlugin extends Plugin {
    TreeMap<String, ModuleCommand> loadedCommands = new TreeMap<>();

    static String name;
    static String version;
    static BasePlugin instance;
    static RATAPI ratapi;
    static File userFolder;
    static File moduleFolder;
    static MainConfigHandler mainConfigHandler;
    static MainMessagesHandler mainMessagesHandler;
    static LuckPerms luckPerms;

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

    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public static File getUserFolder() {
        return userFolder;
    }

    public static File getModuleFolder() {
        return moduleFolder;
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


}
