package net.streamline.platform;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.apib.SLAPIB;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.platform.config.SavedProfileConfig;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.profile.SpigotProfiler;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.listeners.PlatformListener;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class BasePlugin extends JavaPlugin implements IStreamline {
    public class Runner implements Runnable {
        public Runner() {
            getMessenger().logInfo("Task Runner registered!");
        }

        @Override
        public void run() {
            SLAPI.getMainScheduler().tick();
        }
    }

    @Getter
    private final PlatformType platformType = PlatformType.SPIGOT;
    @Getter
    private final ServerType serverType = ServerType.BACKEND;

    @Getter @Setter
    private StreamlineResourcePack resourcePack;

    @Getter
    private final String version = "${{project.version}}";
    @Getter
    private static BasePlugin instance;
    @Getter
    private SLAPI<BasePlugin, UserManager, Messenger> slapi;
    @Getter
    private SLAPIB slapiB;

    public Server getProxy() {
        return getServer();
    }

    @Getter
    private UserManager userManager;
    @Getter
    private Messenger messenger;
    @Setter @Getter
    private SavedProfileConfig profileConfig;

    @Override
    public void onLoad() {
        instance = this;

        this.load();
    }

    @Override
    public void onEnable() {
        userManager = new UserManager();
        messenger = new Messenger();
        slapi = new SLAPI<>(this, getUserManager(), getMessenger(), getDataFolder());
        slapiB = new SLAPIB(getSlapi(), this);

        getSlapi().setProxyMessenger(new ProxyPluginMessenger());
        profileConfig = new SavedProfileConfig();
        getSlapi().setProfiler(new SpigotProfiler());

        registerListener(new PlatformListener());
        getProxy().getScheduler().scheduleSyncRepeatingTask(this, new Runner(), 0, 50);

        getProxy().getMessenger().registerOutgoingPluginChannel(this, SLAPI.getApiChannel());
        getProxy().getMessenger().registerIncomingPluginChannel(this, SLAPI.getApiChannel(), new PlatformListener.ProxyMessagingListener());

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

    abstract public void reload();

    public static void registerListener(Listener listener) {
        getInstance().getProxy().getPluginManager().registerEvents(listener, getInstance());
    }

    @Override
    public @NotNull Collection<StreamlinePlayer> getOnlinePlayers() {
        List<StreamlinePlayer> players = new ArrayList<>();

        for (Player player : onlinePlayers()) {
            players.add(getUserManager().getOrGetPlayer(player));
        }

        return players;
    }

    @Override
    public ProperCommand createCommand(StreamlineCommand command) {
        return new ProperCommand(command);
    }

    @Override
    public int getMaxPlayers() {
        return getInstance().getProxy().getMaxPlayers();
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        List<String> r = new ArrayList<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getName());
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
            if (! player.hasPermission(permission)) continue;
            getMessenger().sendMessage(player, message);
            people ++;
        }

        return people;
    }

    @Override
    public boolean hasPermission(StreamlineUser user, String permission) {
        Player player = getPlayer(user.getUuid());
        if (player == null) return false;
        return player.hasPermission(permission);
    }

    @Override
    public void chatAs(StreamlineUser as, String message) {
        if (as instanceof StreamlineConsole) {
            runAsStrictly(as, message);
        }
        if (as instanceof StreamlinePlayer) {
            if (getMessenger().isCommand(message)) runAsStrictly(as, message.substring("/".length()));
            Player player = getPlayer(as.getUuid());
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
            if (getMessenger().isCommand(command)) runAsStrictly(as, command.substring("/".length()));
            Player player = getPlayer(as.getUuid());
            if (player == null) return;
            getInstance().getProxy().dispatchCommand(player, command);
        }
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin) != null;
    }

    @Override
    public StreamlineServerInfo getStreamlineServer(String server) {
        return getProfileConfig().getServerInfo(server);
    }

    @Override
    public void setStreamlineServer(StreamlineServerInfo serverInfo) {
        getInstance().getProfileConfig().updateServerInfo(serverInfo);
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
        Player p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, String uuid) {
        Player p = getPlayer(uuid);

//        getMessenger().logInfo("Attempting to send a resource pack to a uuid of '" + whitelistedUuid + "'...");

        sendResourcePack(resourcePack, p);
    }

    public void sendResourcePack(StreamlineResourcePack resourcePack, Player player) {
        if (player == null) {
            getMessenger().logWarning("Tried to send a player a resource pack, but could not find their player!");
            return;
        }

//        getMessenger().logInfo("Sending resource pack to '" + player.getName() + "'.");

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
