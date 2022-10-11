package net.streamline.platform;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.profile.BungeeProfiler;
import net.streamline.platform.savables.UserManager;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public abstract class BasePlugin extends Plugin implements IStreamline {
    public static class Runner implements Runnable {
        public Runner() {
            MessageUtils.logInfo("Task Runner registered!");
        }

        @Override
        public void run() {
            SLAPI.getMainScheduler().tick();
        }
    }

    @Getter
    private final PlatformType platformType = PlatformType.BUNGEE;
    @Getter
    private final ServerType serverType = ServerType.PROXY;

    @Getter
    private String name;
    @Getter
    private String version;
    @Getter
    private static BasePlugin instance;
    @Getter
    private SLAPI<BasePlugin, UserManager, Messenger> slapi;

    @Getter
    private UserManager userManager;
    @Getter
    private Messenger messenger;

    @Getter @Setter
    private StreamlineResourcePack resourcePack;

    @Override
    public void onLoad() {
        instance = this;
        name = "StreamlineAPI";
        version = "${{project.version}}";

        this.load();
    }

    @Override
    public void onEnable() {
        userManager = new UserManager();
        messenger = new Messenger();
        slapi = new SLAPI<>(this, getUserManager(), getMessenger(), getDataFolder());
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());
        getSlapi().setProfiler(new BungeeProfiler());

        registerListener(new PlatformListener());
        getProxy().getScheduler().schedule(this, new Runner(), 0, 50, TimeUnit.MILLISECONDS);

        getProxy().registerChannel(SLAPI.getApiChannel());

        this.enable();
    }

    @Override
    public void onDisable() {
        for (StreamlineUser user : UserUtils.getLoadedUsersSet()) {
            user.saveAll();
        }

        this.disable();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    public static void registerListener(Listener listener) {
        getInstance().getProxy().getPluginManager().registerListener(getInstance(), listener);
    }

    @Override
    public @NotNull ConcurrentSkipListSet<StreamlinePlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<StreamlinePlayer> players = new ConcurrentSkipListSet<>();

        for (ProxiedPlayer player : onlinePlayers()) {
            players.add(getUserManager().getOrGetPlayer(player));
        }

        return players;
    }

    @Override
    public ProperCommand createCommand(StreamlineCommand command) {
        return new ProperCommand(command);
    }

    public int getMaxPlayers() {
        return getInstance().getProxy().getConfig().getPlayerLimit();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getName());
        });

//        r.add(UserUtils.getConsole().latestName);

        return r;
    }

    @Override
    public long getConnectionThrottle() {
        return getInstance().getProxy().getConfig().getThrottle();
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

    public static Optional<ProxiedPlayer> getPlayerByName(String name) {
        return Optional.ofNullable(getInstance().getProxy().getPlayer(name));
    }

    public static @Nullable ProxiedPlayer getPlayerExact(@NotNull String name) {
        if (getPlayerByName(name).isEmpty()) return null;
        return getPlayerByName(name).get();
    }

    public static ProxiedPlayer getPlayer(CommandSender sender) {
        return getInstance().getProxy().getPlayer(sender.getName());
    }

    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getConfig().isOnlineMode();
    }

    @Override
    public void shutdown() {
        getInstance().getProxy().stop();
    }

    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int people = 0;

        for (ProxiedPlayer player : onlinePlayers()) {
            if (! player.hasPermission(permission)) continue;
            getMessenger().sendMessage(player, message);
            people ++;
        }

        return people;
    }

    @Override
    public boolean hasPermission(StreamlineUser user, String permission) {
        ProxiedPlayer player = getPlayer(user.getUuid());
        if (player == null) return false;
        return player.hasPermission(permission);
    }

    @Override
    public void chatAs(StreamlineUser as, String message) {
        if (as instanceof StreamlineConsole) {
            runAsStrictly(as, message);
        }
        if (as instanceof StreamlinePlayer) {
            if (MessageUtils.isCommand(message)) runAsStrictly(as, message.substring("/".length()));
            ProxiedPlayer player = getPlayer(as.getUuid());
            if (player == null) return;
            player.chat(message);
        }
    }

    @Override
    public void runAsStrictly(StreamlineUser as, String command) {
        if (as instanceof StreamlineConsole) {
            getInstance().getProxy().getPluginManager().dispatchCommand(getInstance().getProxy().getConsole(), command);
        }
        if (as instanceof StreamlinePlayer) {
            if (MessageUtils.isCommand(command)) runAsStrictly(as, command.substring("/".length()));
            ProxiedPlayer player = getPlayer(as.getUuid());
            if (player == null) return;
            getInstance().getProxy().getPluginManager().dispatchCommand(player, command);
        }
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
        if (! (event.getEvent() instanceof Event e)) return;
        getInstance().getProxy().getPluginManager().callEvent(e);
    }

    @Override
    public void fireEvent(StreamlineEvent event) {
        fireEvent(event, true);
    }

    @Override
    public void fireEvent(StreamlineEvent event, boolean async) {
        fireEvent(new ProperEvent(event));
    }

    @Override
    public void handleMisSync(StreamlineEvent event, boolean async) {
        fireEvent(new ProperEvent(event));
    }

    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        return new ConcurrentSkipListSet<>(getInstance().getProxy().getServers().keySet());
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, StreamlineUser player) {
        ProxiedPlayer p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, String uuid) {
        ProxiedPlayer p = getPlayer(uuid);
        sendResourcePack(resourcePack, p);
    }

    public void sendResourcePack(StreamlineResourcePack resourcePack, ProxiedPlayer player) {
        // nothing right now
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }
}
