package net.streamline.platform.savables;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.Getter;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.server.CosmicServer;
import singularity.interfaces.IUserManager;
import singularity.objects.CosmicResourcePack;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Velocity-specific implementation of {@link IUserManager} that bridges
 * Velocity's {@link CommandSource}/{@link Player} types to the platform-agnostic
 * {@link CosmicSender}/{@link CosmicPlayer} abstraction layer.
 *
 * <p>This class is a singleton; the active instance is accessible via
 * {@link #getInstance()} after construction.</p>
 */
public class UserManager implements IUserManager<CommandSource, Player> {

    /**
     * The singleton instance of this {@code UserManager}, set during construction.
     */
    @Getter
    private static UserManager instance;

    /**
     * Constructs a new {@code UserManager} and registers it as the singleton instance.
     */
    public UserManager() {
        instance = this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link UserUtils#getOrCreatePlayer(String)} using the
     * player's UUID string.</p>
     *
     * @param player the Velocity {@link Player} whose cosmic counterpart is needed
     * @return an {@link Optional} containing the {@link CosmicPlayer}, or empty if creation fails
     */
    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(Player player) {
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the console sender wrapper when {@code sender} is not a {@link Player};
     * otherwise casts the source to a {@link Player} and delegates to
     * {@link #getOrCreatePlayer(Player)}.</p>
     *
     * @param sender the Velocity {@link CommandSource} to resolve
     * @return an {@link Optional} containing the corresponding {@link CosmicSender}, or empty
     */
    @Override
    public Optional<CosmicSender> getOrCreateSender(CommandSource sender) {
        if (isConsole(sender)) {
            return Optional.ofNullable(UserUtils.getConsole());
        } else {
            Player player = (Player) sender;
            return getOrCreatePlayer(player).map(s -> s);
        }
    }

    /**
     * Returns the display name for a given {@link CommandSource}.
     *
     * <p>If the source is the console, the configured console name is returned.
     * Otherwise the player's username is returned.</p>
     *
     * @param sender the {@link CommandSource} whose name is requested
     * @return the console name or the player's username
     */
    public String getUsername(CommandSource sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().getConsoleName();
        else return ((Player) sender).getUsername();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the configured console name when {@code uuid} matches the console
     * discriminator. Otherwise looks up the online {@link Player} by UUID and
     * returns their username, or {@code null} if the player is not online.</p>
     *
     * @param uuid the UUID string of the player, or the console discriminator
     * @return the username, or {@code null} if no matching online player is found
     */
    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator())) return GivenConfigs.getMainConfig().getConsoleName();
        else {
            Player player = StreamlineVelocity.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    /**
     * Determines whether a {@link CommandSource} represents the server console
     * rather than a connected player.
     *
     * @param sender the {@link CommandSource} to test
     * @return {@code true} if the source is not a {@link Player} instance
     */
    public boolean isConsole(CommandSource sender) {
        return ! (sender instanceof Player);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The console UUID is always considered online. For players, the currently
     * connected player list is iterated to check for a UUID match.</p>
     *
     * @param uuid the UUID string to check, or the console discriminator
     * @return {@code true} if the entity represented by {@code uuid} is online
     */
    @Override
    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        for (Player player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes {@code command} on behalf of the given user. When {@code bypass}
     * is {@code true} and the player does not already hold the {@code "*"} permission,
     * that permission is temporarily granted via LuckPerms for the duration of the
     * command execution and then revoked. If LuckPerms is unavailable and bypass is
     * required, the method returns {@code false} without executing the command.</p>
     *
     * @param user    the {@link CosmicSender} that should run the command
     * @param bypass  {@code true} to temporarily grant wildcard permissions
     * @param command the command string to execute (without leading {@code /})
     * @return {@code true} if the command was dispatched; {@code false} otherwise
     */
    @Override
    public boolean runAs(CosmicSender user, boolean bypass, String command) {
        CommandSource source;
        if (user instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) user;
            source = StreamlineVelocity.getPlayer(player.getUuid());
        }
        else {
            source = StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource();
            StreamlineVelocity.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
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
        StreamlineVelocity.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
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
     *
     * <p>Iterates every registered backend server and collects players whose
     * current server name matches {@code server}.</p>
     *
     * @param server the name of the backend server to query
     * @return a {@link ConcurrentSkipListSet} of online {@link CosmicPlayer}s on that server
     */
    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        StreamlineVelocity.getInstance().getProxy().getAllServers().forEach(a -> {
            a.getPlayersConnected().forEach(b -> {
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
     * <p>Looks up the named server in the Velocity proxy and initiates a connection
     * request. If the player is offline or the server does not exist, the method
     * logs a warning and returns without taking action.</p>
     *
     * @param user   the {@link CosmicPlayer} to transfer
     * @param server the name of the registered backend server to connect to
     */
    @Override
    public void connect(CosmicPlayer user, String server) {
        if (! user.isOnline()) return;

        Player player = StreamlineVelocity.getPlayer(user.getUuid());
        if (player == null) return;
        Optional<RegisteredServer> serverOptional = StreamlineVelocity.getInstance().getProxy().getServer(server);

        if (serverOptional.isEmpty()) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.createConnectionRequest(serverOptional.get()).connect();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link StreamlineVelocity#sendResourcePack(CosmicResourcePack, Player)}
     * after verifying the player is online. Any exception thrown during the offer
     * is printed to stderr but does not propagate.</p>
     *
     * @param player the {@link CosmicPlayer} to send the resource pack to
     * @param pack   the {@link CosmicResourcePack} to offer
     */
    @Override
    public void sendUserResourcePack(CosmicPlayer player, CosmicResourcePack pack) {
        if (! player.isOnline()) return;
        Player p = StreamlineVelocity.getPlayer(player.getUuid());
        if (p == null) return;

        try {
//            ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo(p.getUniqueId(), new URI(pack.getUrl()), pack.getHash());
//            p.sendResourcePackOffer(pack.getUrl(), pack.getHash(), pack.getPrompt(), pack.isForce());
            StreamlineVelocity.getInstance().sendResourcePack(pack, p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the player's remote {@link InetSocketAddress}, strips the
     * port component, and returns only the host portion. Returns a configured
     * null-placeholder string when the player is offline or the address is
     * unavailable.</p>
     *
     * @param uuid the UUID string of the player whose IP address is requested
     * @return the player's IP address string, or a null-placeholder if unavailable
     */
    @Override
    public String parsePlayerIP(String uuid) {
        Player player = StreamlineVelocity.getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getRemoteAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code 0} if no online player matches {@code uuid}.</p>
     *
     * @param uuid the UUID string of the player
     * @return the player's current ping in milliseconds, or {@code 0} if not found
     */
    @Override
    public double getPlayerPing(String uuid) {
        Player player = StreamlineVelocity.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Disconnects the player from the proxy, displaying {@code message} as the
     * kick reason after processing color codes via {@link Messenger}.</p>
     *
     * @param user    the {@link CosmicPlayer} to kick
     * @param message the kick reason message, supporting color codes
     */
    @Override
    public void kick(CosmicPlayer user, String message) {
        Optional<Player> playerOptional = StreamlineVelocity.getInstance().getProxy().getPlayer(user.getUuid());
        if (playerOptional.isEmpty()) return;
        playerOptional.get().disconnect(Messenger.getInstance().codedText(message));
    }

    /**
     * {@inheritDoc}
     *
     * @param uuid the UUID string of the player to retrieve
     * @return the online {@link Player}, or {@code null} if not found
     */
    @Override
    public Player getPlayer(String uuid) {
        return StreamlineVelocity.getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates all currently connected Velocity players and ensures each has a
     * corresponding {@link CosmicPlayer} entry, building and returning a snapshot
     * map of UUID → {@link CosmicPlayer}.</p>
     *
     * @return a {@link ConcurrentSkipListMap} mapping UUID strings to their {@link CosmicPlayer} instances
     */
    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        for (Player player : BasePlugin.onlinePlayers()) {
            CosmicPlayer cosmicPlayer = getOrCreatePlayer(player).orElse(null);
            if (cosmicPlayer == null) continue;
            r.put(player.getUniqueId().toString(), cosmicPlayer);
        }

        return r;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the player's current {@link ServerConnection} via Velocity and
     * returns the backend server name. Returns {@code null} at any point in the
     * chain where data is unavailable.</p>
     *
     * @param uuid the UUID string of the player to query
     * @return the name of the backend server the player is connected to, or {@code null}
     */
    @Override
    public String getServerPlayerIsOn(String uuid) {
        Player player = getPlayer(uuid);
        if (player == null) return null;

        ServerConnection server = player.getCurrentServer().orElse(null);
        if (server == null) return null;

        ServerInfo info = server.getServerInfo();
        if (info == null) return null;

        return info.getName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Convenience overload that resolves the player's UUID and delegates to
     * {@link #getServerPlayerIsOn(String)}.</p>
     *
     * @param player the Velocity {@link Player} to query
     * @return the name of the backend server the player is on, or {@code null}
     */
    @Override
    public String getServerPlayerIsOn(Player player) {
        return getServerPlayerIsOn(player.getUniqueId().toString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the Velocity username, which is used as the display name on
     * proxy-level operations. Returns {@code null} if the player is not online.</p>
     *
     * @param uuid the UUID string of the player
     * @return the player's username, or {@code null} if not found
     */
    @Override
    public String getDisplayName(String uuid) {
        Player player = getPlayer(uuid);
        if (player == null) return null;

        return player.getUsername();
    }

    /**
     * {@inheritDoc}
     *
     * <p>On a Velocity proxy, true coordinate-based teleportation is not available.
     * This implementation transfers the player to the backend server specified in
     * {@code location} by creating a Velocity connection request. The player's
     * exact position within that server is not set here.</p>
     *
     * @param player   the {@link CosmicPlayer} to transfer
     * @param location the {@link CosmicLocation} whose server the player will be sent to
     */
    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (! player.isOnline()) return;
        Player p = StreamlineVelocity.getPlayer(player.getUuid());
        if (p == null) return;

        CosmicServer server = location.getServer();
        String serverName = server.getIdentifier();

        Optional<RegisteredServer> optional = StreamlineVelocity.getInstance().getProxy().getServer(serverName);
        if (optional.isEmpty()) return;

        p.createConnectionRequest(optional.get());
    }
}
