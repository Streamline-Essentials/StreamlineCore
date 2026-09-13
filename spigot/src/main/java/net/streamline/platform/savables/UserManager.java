package net.streamline.platform.savables;

import host.plas.bou.scheduling.TaskManager;
import lombok.Getter;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineSpigot;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.WorldPosition;
import singularity.interfaces.IUserManager;
import singularity.objects.CosmicResourcePack;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Spigot implementation of {@link IUserManager} for managing {@link CommandSender} and
 * {@link Player} instances on a Bukkit/Spigot backend server.
 *
 * <p>Provides player resolution, IP parsing, permission-bypassed command dispatch,
 * resource-pack delivery, and world teleportation backed by Bukkit APIs.
 */
public class UserManager implements IUserManager<CommandSender, Player> {
    /**
     * The singleton instance of {@code UserManager}, set during construction.
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
     * <p>Delegates to {@link UserUtils#getOrCreatePlayer(String)} using the player's UUID string.
     *
     * @param player the Bukkit player whose {@link CosmicPlayer} should be retrieved or created
     * @return an {@link Optional} containing the {@link CosmicPlayer}, or empty if creation failed
     */
    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(Player player) {
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the console sender wrapper if {@code sender} is not a {@link Player};
     * otherwise delegates to {@link #getOrCreatePlayer(Player)}.
     *
     * @param sender the Bukkit command sender to resolve
     * @return an {@link Optional} containing the matching {@link CosmicSender}, or empty on failure
     */
    @Override
    public Optional<CosmicSender> getOrCreateSender(CommandSender sender) {
        if (isConsole(sender)) {
            return Optional.ofNullable(UserUtils.getConsole());
        } else {
            Player player = (Player) sender;
            return getOrCreatePlayer(player).map(s -> s);
        }
    }

    /**
     * Returns the display name for the given {@link CommandSender}.
     *
     * <p>Returns the configured console name if the sender is the console, otherwise the
     * sender's {@link CommandSender#getName()}.
     *
     * @param sender the command sender whose name to retrieve
     * @return the sender's display name
     */
    public String getUsername(CommandSender sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().getConsoleName();
        else return sender.getName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the configured console name for the console UUID discriminator, or
     * resolves the online player's name via {@link #getUsername(CommandSender)}.
     *
     * @param uuid the UUID string of the sender
     * @return the display name, or {@code null} if the player is not online
     */
    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator())) return GivenConfigs.getMainConfig().getConsoleName();
        else {
            Player player = StreamlineSpigot.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    /**
     * Determines whether the given {@link CommandSender} is the server console.
     *
     * @param sender the sender to test
     * @return {@code true} if the sender is not a player (i.e., is the console)
     */
    public boolean isConsole(CommandSender sender) {
        return ! (sender instanceof Player);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code true} for the console UUID. For player UUIDs, iterates
     * the online player list to find a matching UUID.
     *
     * @param uuid the UUID string to test
     * @return {@code true} if the console or a matching online player is found
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
     * Parses the IP address from a Bukkit {@link Player}'s socket address.
     *
     * @param player the player whose IP to parse; if {@code null} returns the null placeholder
     * @return the player's IP address string, or a configured null/placeholder string on failure
     */
    public String parsePlayerIP(Player player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        SocketAddress address = player.getAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If {@code bypass} is {@code true} and LuckPerms is available, temporarily grants
     * the {@code *} permission node before dispatching the command via
     * {@link Bukkit#dispatchCommand(CommandSender, String)}, then removes it afterwards.
     *
     * @param player  the sender to execute the command as
     * @param bypass  whether to temporarily grant all permissions
     * @param command the command string (without leading slash)
     * @return {@code true} if the command was dispatched; {@code false} if the source
     *         player is offline or bypass was requested but LuckPerms is unavailable
     */
    @Override
    public boolean runAs(CosmicSender player, boolean bypass, String command) {
        CommandSender source;
        if (! player.isConsole()) {
            source = StreamlineSpigot.getPlayer(player.getUuid());
        }
        else {
            source = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.addPermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        Bukkit.dispatchCommand(source, command);
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
     * <p>Returns all online {@link CosmicPlayer} instances whose recorded server name
     * matches {@code server}.
     *
     * @param server the server name to filter by
     * @return a set of online players currently on the specified server
     */
    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        for (Player player : BasePlugin.onlinePlayers()) {
            CosmicPlayer p = getOrCreatePlayer(player).orElse(null);
            if (p == null) continue;
            if (p.isOnline() && p.getServerName().equals(server)) r.add(p);
        }

        return r;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Not applicable on a Spigot backend — this method is a no-op.
     *
     * @param user   the player to connect
     * @param server the target server name (ignored)
     */
    @Override
    public void connect(CosmicPlayer user, String server) {
        // not applicable
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the resource pack to the online Bukkit player via
     * {@link Player#setResourcePack(String, byte[])}.
     *
     * @param user the {@link CosmicPlayer} to send the pack to; must be online
     * @param pack the resource pack descriptor containing the URL and hash
     */
    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        if (! user.isOnline()) return;
        Player p = StreamlineSpigot.getPlayer(user.getUuid());
        if (p == null) return;

//        p.setResourcePack(pack.getUrl(), pack.getHash(), pack.getPrompt(), pack.isForce());
        p.setResourcePack(pack.getUrl(), pack.getHash());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the player by UUID string, then parses the IP from the player's
     * {@link java.net.InetSocketAddress}.
     *
     * @param uuid the UUID string of the target player
     * @return the player's IP address, or a configured null/placeholder string if unavailable
     */
    @Override
    public String parsePlayerIP(String uuid) {
        Player player = StreamlineSpigot.getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the player's network ping via {@link Player#getPing()}.
     *
     * @param uuid the UUID string of the target player
     * @return the ping in milliseconds, or {@code 0} if the player is not online
     */
    @Override
    public double getPlayerPing(String uuid) {
        Player player = StreamlineSpigot.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Kicks the player with the given message, converting colour codes via
     * {@link Messenger#codedString(String)}.
     *
     * @param user    the {@link CosmicPlayer} to kick; must be online
     * @param message the kick message (supports colour codes)
     */
    @Override
    public void kick(CosmicPlayer user, String message) {
        Player player = StreamlineSpigot.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.kickPlayer(Messenger.getInstance().codedString(message));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link StreamlineSpigot#getPlayer(String)}.
     *
     * @param uuid the UUID string of the player to retrieve
     * @return the online Bukkit {@link Player}, or {@code null} if not found
     */
    @Override
    public Player getPlayer(String uuid) {
        return StreamlineSpigot.getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a map of all currently online players, creating or retrieving their
     * {@link CosmicPlayer} entries.
     *
     * @return a sorted map of UUID strings to their corresponding {@link CosmicPlayer} instances
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
     * <p>Delegates to {@link #getServerPlayerIsOn(String)} using the player's UUID.
     *
     * @param player the Bukkit player
     * @return always {@code "--null"} because Spigot backends cannot determine the proxy server
     */
    @Override
    public String getServerPlayerIsOn(Player player) {
        return getServerPlayerIsOn(player.getUniqueId().toString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Spigot backends do not have awareness of proxy-level server routing; this
     * method always returns {@code "--null"}.
     *
     * @param uuid the player's UUID string (unused)
     * @return {@code "--null"} unconditionally
     */
    @Override
    public String getServerPlayerIsOn(String uuid) {
        return "--null";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the Bukkit display name of the online player via {@link Player#getDisplayName()}.
     *
     * @param uuid the UUID string of the target player
     * @return the display name, or {@code null} if the player is not online
     */
    @Override
    public String getDisplayName(String uuid) {
        Player player = getPlayer(uuid);
        if (player == null) return null;

        return player.getDisplayName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Constructs a Bukkit {@link Location} from the supplied {@link CosmicLocation} and
     * schedules a safe teleport via {@link TaskManager#teleport(org.bukkit.entity.Entity, Location)}.
     *
     * @param player   the {@link CosmicPlayer} to teleport; must be online
     * @param location the target location including world, coordinates, yaw, and pitch
     */
    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (! player.isOnline()) return;
        Player p = StreamlineSpigot.getPlayer(player.getUuid());
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) return;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        PlayerRotation rot = location.getRotation();
        float yaw = rot.getYaw();
        float pitch = rot.getPitch();

        Location loc = new Location(world, x, y, z, yaw, pitch);

        TaskManager.teleport(p, loc);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Teleports {@code player} to the current location of {@code to} using
     * {@link TaskManager#teleport(org.bukkit.entity.Entity, Location)}.
     * Both players must be online.
     *
     * @param player the {@link CosmicPlayer} to teleport; must be online
     * @param to     the target {@link CosmicPlayer} whose location is used; must be online
     */
    @Override
    public void teleport(CosmicPlayer player, CosmicPlayer to) {
        if (! player.isOnline()) return;
        Player p = StreamlineSpigot.getPlayer(player.getUuid());
        if (p == null) return;

        if (! to.isOnline()) return;
        Player toP = StreamlineSpigot.getPlayer(to.getUuid());
        if (toP == null) return;

        TaskManager.teleport(p, toP.getLocation());
    }
}
