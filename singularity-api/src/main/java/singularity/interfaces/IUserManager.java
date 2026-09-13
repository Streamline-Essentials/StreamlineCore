package singularity.interfaces;

import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.objects.CosmicResourcePack;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Platform-agnostic facade for all player and sender management operations.
 *
 * <p>Each platform provides its own implementation that bridges between the platform-native
 * player/sender types ({@code C} and {@code P}) and the Streamline-level wrappers
 * ({@link CosmicSender} and {@link CosmicPlayer}).  The user manager is the single point of
 * truth for looking up, creating, connecting, kicking, and teleporting players.</p>
 *
 * @param <C> the platform-native command-sender type
 * @param <P> the platform-native player type, which must extend {@code C}
 */
public interface IUserManager<C, P extends C> {

    /**
     * Retrieves the {@link CosmicPlayer} for the given platform player, creating one if it
     * does not yet exist in the loaded user set.
     *
     * @param player the platform-native player object; must not be {@code null}
     * @return an {@link Optional} containing the player wrapper, or empty if creation failed
     */
    Optional<CosmicPlayer> getOrCreatePlayer(P player);

    /**
     * Retrieves the {@link CosmicSender} for the given platform sender, creating one if it
     * does not yet exist in the loaded user set.
     *
     * @param sender the platform-native command sender object; must not be {@code null}
     * @return an {@link Optional} containing the sender wrapper, or empty if creation failed
     */
    Optional<CosmicSender> getOrCreateSender(C sender);

    /**
     * Looks up the username associated with the given UUID string.
     *
     * @param uuid the UUID string of the player whose name should be retrieved
     * @return the username, or an empty/fallback string if the player is unknown
     */
    String getUsername(String uuid);

    /**
     * Returns whether the player identified by the given UUID is currently online.
     *
     * @param uuid the UUID string of the player to check
     * @return {@code true} if the player is connected and online
     */
    boolean isOnline(String uuid);

    /**
     * Executes a command on behalf of the given sender, optionally bypassing permission checks.
     *
     * @param user    the sender who "runs" the command
     * @param bypass  {@code true} to skip permission enforcement for this execution
     * @param command the command string to execute (without the leading slash)
     * @return {@code true} if the command was dispatched successfully
     */
    boolean runAs(CosmicSender user, boolean bypass, String command);

    /**
     * Returns all {@link CosmicPlayer}s currently connected to the named server.
     *
     * @param server the name of the server to query
     * @return a thread-safe set of players on that server; empty if the server is unknown or empty
     */
    ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server);

    /**
     * Transfers the given player to the specified backend server.
     *
     * @param user   the player to connect; must be online
     * @param server the name of the target server as registered with the proxy
     */
    void connect(CosmicPlayer user, String server);

    /**
     * Kicks the given player from the server with the provided reason message.
     *
     * @param user    the player to kick; must be online
     * @param message the disconnect message shown to the player
     */
    void kick(CosmicPlayer user, String message);

    /**
     * Sends the given resource pack to the specified player.
     *
     * @param user the player to send the resource pack to; must be online
     * @param pack the resource pack to send; must not be {@code null}
     */
    void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack);

    /**
     * Resolves the IP address string of the player identified by the given UUID.
     *
     * @param uuid the UUID string of the player whose IP should be retrieved
     * @return the player's IP address as a string, or an empty/fallback value if unavailable
     */
    String parsePlayerIP(String uuid);

    /**
     * Returns the network ping (latency) of the player identified by the given UUID.
     *
     * @param uuid the UUID string of the player to query
     * @return the ping in milliseconds, or {@code -1} if unavailable
     */
    double getPlayerPing(String uuid);

    /**
     * Returns the name of the server that the player identified by the given UUID is currently on.
     *
     * @param uuid the UUID string of the player to query
     * @return the server name, or an empty/fallback string if the player is not online
     */
    String getServerPlayerIsOn(String uuid);

    /**
     * Returns the name of the server that the given platform-native player is currently on.
     *
     * @param player the platform-native player object; must not be {@code null}
     * @return the server name, or an empty/fallback string if unavailable
     */
    String getServerPlayerIsOn(P player);

    /**
     * Returns the display name of the player identified by the given UUID.
     *
     * @param uuid the UUID string of the player whose display name should be retrieved
     * @return the display name, or the raw username if no custom display name is set
     */
    String getDisplayName(String uuid);

    /**
     * Returns the platform-native player object for the given UUID string.
     *
     * @param uuid the UUID string of the player to look up
     * @return the platform player object, or {@code null} if the player is not online
     */
    P getPlayer(String uuid);

    /**
     * Ensures all currently online players have corresponding {@link CosmicPlayer} entries
     * and returns the full map of UUID-to-player wrappers.
     *
     * @return a thread-safe map from UUID string to {@link CosmicPlayer}; never {@code null}
     */
    ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers();

    /**
     * Teleports the given player to the specified location.
     *
     * @param player   the player to teleport; must be online
     * @param location the target location including world, coordinates, and optionally rotation
     */
    void teleport(CosmicPlayer player, CosmicLocation location);

    /**
     * Teleports the given player to the current location of the specified sender.
     *
     * <p>If the sender is the console or not a valid online player, the teleport is skipped.
     * If the target player is offline, the teleport falls back to their last known location.</p>
     *
     * @param player the player to teleport; must be online
     * @param to     the sender whose location or position is used as the destination
     */
    default void teleport(CosmicPlayer player, CosmicSender to) {
        if (! player.isOnline()) return;
        if (to.isConsole()) return;

        CosmicPlayer toPlayer = UserUtils.getOrCreatePlayer(to.getUuid()).orElse(null);
        if (toPlayer == null) return;

        if (! toPlayer.isOnline()) {
            teleport(player, toPlayer.getLocation());
        } else {
            teleport(player, toPlayer);
        }
    }

    /**
     * Teleports the given player to the current location of the specified target player.
     *
     * <p>Delegates to {@link #teleport(CosmicPlayer, CosmicLocation)} using the target's
     * last known location.</p>
     *
     * @param player the player to teleport; must be online
     * @param to     the target player whose location is used as the destination
     */
    default void teleport(CosmicPlayer player, CosmicPlayer to) {
        teleport(player, to.getLocation());
    }
}
