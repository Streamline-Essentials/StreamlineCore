package net.streamline.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.events.StreamEventHandler;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.listeners.PlatformListener;
import net.streamline.platform.messaging.ProxyPluginMessenger;
import net.streamline.platform.profile.VelocityProfiler;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public abstract class BasePlugin implements IStreamline {
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
    private final PlatformType platformType = PlatformType.VELOCITY;
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

    @Setter
    private VelocityProfiler profiler;

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

        Path parentPath = dd.getParent();
        if (parentPath != null) {
            File parentFile = new File(parentPath.toUri());
            File[] files = parentFile.listFiles((f) -> {
                if (! f.isDirectory()) return false;
                if (f.getName().equals("streamlineapi")) return true;
                return false;
            });

            if (files != null) {
                Arrays.stream(files).forEach(file -> {
                    file.renameTo(new File(parentPath.toFile(), "streamlinecore"));
                });
            }
        }

        onLoad();
    }

    public void onLoad() {
        instance = this;
        name = "StreamlineAPI";
        version = "${{project.version}}";

        this.load();
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        userManager = new UserManager();
        messenger = new Messenger();
        slapi = new SLAPI<>(this, getUserManager(), getMessenger(), getDataFolder());
        getSlapi().setProxyMessenger(new ProxyPluginMessenger());
        getSlapi().setProfiler(new VelocityProfiler());

        registerListener(new PlatformListener());
        getProxy().getScheduler().buildTask(this, new Runner()).repeat(50, TimeUnit.MILLISECONDS).schedule();

        UserUtils.loadUser(new StreamlineConsole());

        getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.enable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        for (StreamlineUser user : UserUtils.getLoadedUsersSet()) {
            user.saveAll();
        }

        getProxy().getChannelRegistrar().unregister(MinecraftChannelIdentifier.from(SLAPI.getApiChannel()));

        this.disable();
    }

    abstract public void enable();

    abstract public void disable();

    abstract public void load();

    public static void registerListener(Object listener) {
        getInstance().getProxy().getEventManager().register(getInstance(), listener);
    }

    @Override
    public @NotNull ConcurrentSkipListSet<StreamlinePlayer> getOnlinePlayers() {
        ConcurrentSkipListSet<StreamlinePlayer> players = new ConcurrentSkipListSet<>();

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
        return getInstance().getProxy().getConfiguration().getShowMaxPlayers();
    }

    @Override
    public ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach(a -> {
            r.add(a.getName());
        });

//        r.add(getUserManager().getConsole().latestName);

        return r;
    }

    @Override
    public long getConnectionThrottle() {
        return getInstance().getProxy().getConfiguration().getCompressionThreshold();
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
        Optional<Player> player = getInstance().getProxy().getPlayer(getInstance().getUserManager().getUsername(sender));
        if (player.isEmpty()) return null;
        return player.get();
    }

    @Override
    public boolean getOnlineMode() {
        return getInstance().getProxy().getConfiguration().isOnlineMode();
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
            if (MessageUtils.isCommand(message)) runAsStrictly(as, message.substring("/".length()));
            Player player = getPlayer(as.getUuid());
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
            if (MessageUtils.isCommand(command)) runAsStrictly(as, command.substring("/".length()));
            Player player = getPlayer(as.getUuid());
            if (player == null) return;
            getInstance().getProxy().getCommandManager().executeAsync(player, command);
        }
    }

    @Override
    public boolean serverHasPlugin(String plugin) {
        return getInstance().getProxy().getPluginManager().getPlugin(plugin).isPresent();
    }

    @Override
    public boolean equalsAnyServer(String servername) {
        return getServerNames().contains(servername);
    }

    @Override
    public void fireEvent(IProperEvent<?> event) {
        if (! (event.getEvent() instanceof CompletableFuture<?> e)) return;
        getInstance().getProxy().getEventManager().fire(e).join();
    }

    @Override
    public void fireEvent(StreamlineEvent event) {
        fireEvent(event, true);
    }

    @Override
    public void fireEvent(StreamlineEvent event, boolean async) {
        try {
            StreamEventHandler.fireEvent(event);
        } catch (Exception e) {
            handleMisSync(event, async);
        }
    }

    @Override
    public void handleMisSync(StreamlineEvent event, boolean async) {
        StreamEventHandler.fireEvent(event);
    }

    @Override
    public ConcurrentSkipListSet<String> getServerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getInstance().getProxy().getAllServers().forEach(a -> {
            r.add(a.getServerInfo().getName());
        });

        return r;
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, StreamlineUser player) {
        Player p = getPlayer(player.getUuid());
        sendResourcePack(resourcePack, p);
    }

    @Override
    public void sendResourcePack(StreamlineResourcePack resourcePack, String uuid) {
        Player p = getPlayer(uuid);
        sendResourcePack(resourcePack, p);
    }

    public void sendResourcePack(StreamlineResourcePack resourcePack, Player player) {
        if (player == null) return;
        try {
            ResourcePackInfo.Builder infoBuilder = getInstance().getProxy().createResourcePackBuilder(resourcePack.getUrl()).setShouldForce(resourcePack.isForce());
            if (resourcePack.getHash().length > 0) infoBuilder.setHash(resourcePack.getHash());
            if (! resourcePack.getPrompt().equals("")) infoBuilder.setPrompt(getMessenger().codedText(resourcePack.getPrompt()));
            player.sendResourcePackOffer(infoBuilder.build());
        } catch (Exception e) {
            MessageUtils.logWarning("Sent '" + player.getUsername() + "' a resourcepack, but it returned null! This is probably due to an incorrect link to the pack.");
        }
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return getProxy().getClass().getClassLoader();
    }
}
