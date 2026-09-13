package net.streamline.platform.savables;

import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.streamline.api.SLAPI;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineBungee;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.server.CosmicServer;
import singularity.interfaces.IUserManager;
import singularity.messages.builders.ResourcePackMessageBuilder;
import singularity.objects.CosmicResourcePack;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * BungeeCord implementation of {@link IUserManager} that manages the lifecycle
 * of {@link CosmicPlayer} and {@link CosmicSender} objects on a BungeeCord proxy.
 *
 * <p>Provides player lookup, IP parsing, command execution (with optional
 * wildcard-permission bypass), server transfer, resource-pack delivery,
 * kick, teleport, and player-state utilities backed by the BungeeCord API.
 */
public class UserManager implements IUserManager<CommandSender, ProxiedPlayer> {

    /** The singleton instance of this user manager, set during construction. */
    @Getter
    private static UserManager instance;

    /**
     * Constructs the {@code UserManager} and registers it as the singleton instance.
     */
    public UserManager() {
        instance = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(ProxiedPlayer player) {
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the console sender if {@code sender} is not a
     * {@link ProxiedPlayer}; otherwise delegates to
     * {@link #getOrCreatePlayer(ProxiedPlayer)}.
     */
    @Override
    public Optional<CosmicSender> getOrCreateSender(CommandSender sender) {
        if (isConsole(sender)) {
            return Optional.ofNullable(UserUtils.getConsole());
        } else {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            return getOrCreatePlayer(player).map(s -> s);
        }
    }

    /**
     * Returns the display name for a {@link CommandSender}.
     *
     * <p>Returns the configured console name when the sender is not a player;
     * otherwise returns the sender's own name.
     *
     * @param sender the BungeeCord command sender
     * @return the display name
     */
    public String getUsername(CommandSender sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().getConsoleName();
        else return sender.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator())) return GivenConfigs.getMainConfig().getConsoleName();
        else {
            ProxiedPlayer player = StreamlineBungee.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    /**
     * Returns {@code true} if the given {@link CommandSender} is the proxy
     * console rather than an online player.
     *
     * @param sender the sender to test
     * @return {@code true} if {@code sender} is not a {@link ProxiedPlayer}
     */
    public boolean isConsole(CommandSender sender) {
        return ! (sender instanceof ProxiedPlayer);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The console UUID is always considered online. For players, the proxy
     * player list is scanned for a matching UUID.
     */
    @Override
    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        for (ProxiedPlayer player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    /**
     * Extracts the IP address string from a {@link ProxiedPlayer}'s socket address.
     *
     * <p>Returns a configured placeholder string when the player or address is
     * {@code null}.
     *
     * @param player the player whose IP is required
     * @return the host portion of the player's socket address, without port
     */
    public String parsePlayerIP(ProxiedPlayer player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        SocketAddress address = player.getSocketAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code bypass} is {@code true} and LuckPerms is available, the
     * wildcard permission {@code "*"} is temporarily granted, the command is
     * dispatched, and the permission is immediately revoked.
     */
    @Override
    public boolean runAs(CosmicSender user, boolean bypass, String command) {
        CommandSender source;
        if (user instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) user;
            source = StreamlineBungee.getPlayer(player.getUuid());
        }
        else {
            source = StreamlineBungee.getInstance().getProxy().getConsole();
            StreamlineBungee.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
            return true;
        }
        CosmicPlayer player = (CosmicPlayer) user;
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.addPermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        StreamlineBungee.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.removePermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        StreamlineBungee.getInstance().getProxy().getServers().values().forEach(a -> {
            a.getPlayers().forEach(b -> {
                CosmicPlayer player = getOrCreatePlayer(b).orElse(null);
                if (player == null) return;
                if (player.isOnline() && player.getServerName().equals(server)) r.add(player);
            });
        });

        return r;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the player to the named backend server using the plugin-triggered
     * connection reason.
     */
    @Override
    public void connect(CosmicPlayer user, String server) {
        if (! user.isOnline()) return;

        ProxiedPlayer player = StreamlineBungee.getPlayer(user.getUuid());
        if (player == null) return;
        ServerInfo serverInfo = StreamlineBungee.getInstance().getProxy().getServerInfo(server);

        if (serverInfo == null) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.connect(serverInfo, ServerConnectEvent.Reason.PLUGIN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        if (! user.isOnline()) return;
        ProxiedPlayer p = StreamlineBungee.getPlayer(user.getUuid());
        if (p == null) return;

        SLAPI.getInstance().getProxyMessenger().sendMessage(ResourcePackMessageBuilder.build(user, true, user, pack));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String parsePlayerIP(String uuid) {
        ProxiedPlayer player = StreamlineBungee.getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = (InetSocketAddress) player.getSocketAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code 0} if the player is not online.
     */
    @Override
    public double getPlayerPing(String uuid) {
        ProxiedPlayer player = StreamlineBungee.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Disconnects the player with a colour-processed message via
     * {@link Messenger#codedText}.
     */
    @Override
    public void kick(CosmicPlayer user, String message) {
        ProxiedPlayer player = StreamlineBungee.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.disconnect(Messenger.getInstance().codedText(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProxiedPlayer getPlayer(String uuid) {
        return StreamlineBungee.getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only players whose data is already loaded (i.e. present in the cache)
     * are included in the returned map.
     */
    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        for (ProxiedPlayer player : BasePlugin.onlinePlayers()) {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) {
                CosmicPlayer cosmicPlayer = getOrCreatePlayer(player).orElse(null);
                if (cosmicPlayer == null) continue;
                r.put(player.getUniqueId().toString(), cosmicPlayer);
            }
        }

        return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerPlayerIsOn(String uuid) {
        ProxiedPlayer player = getPlayer(uuid);
        if (player == null) return null;

        Server server = player.getServer();
        if (server == null) return null;

        ServerInfo info = server.getInfo();
        if (info == null) return null;

        return info.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerPlayerIsOn(ProxiedPlayer player) {
        return getServerPlayerIsOn(player.getUniqueId().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName(String uuid) {
        ProxiedPlayer player = getPlayer(uuid);
        if (player == null) return null;

        return player.getDisplayName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>On BungeeCord, teleportation is limited to connecting the player to
     * the backend server specified in the location's {@link CosmicServer}.
     * Fine-grained coordinate teleportation must be handled by a backend plugin.
     */
    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (! player.isOnline()) return;
        ProxiedPlayer p = StreamlineBungee.getPlayer(player.getUuid());
        if (p == null) return;

        CosmicServer server = location.getServer();
        String serverName = server.getIdentifier();

        ServerInfo info = StreamlineBungee.getInstance().getProxy().getServerInfo(serverName);
        if (info == null) return;

        p.connect(info, ServerConnectEvent.Reason.PLUGIN);
    }
}
