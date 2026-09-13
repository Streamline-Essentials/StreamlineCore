package singularity.interfaces;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.events.CosmicEvent;
import singularity.objects.CosmicResourcePack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Primary contract that every platform-specific Streamline plugin entry-point must fulfill.
 *
 * <p>Each supported server platform (Velocity, BungeeCord, Spigot, Fabric, Forge) provides
 * an implementation of this interface through its {@code BasePlugin} class. The {@link Singularity}
 * singleton delegates all platform-specific operations — event firing, player enumeration,
 * command creation, resource-pack delivery, and shutdown — through this interface.</p>
 */
public interface ISingularityExtension {

    /**
     * Identifies the Minecraft server platform that is running this extension.
     */
    enum PlatformType {
        /** BungeeCord proxy platform. */
        BUNGEE,
        /** Spigot (or CraftBukkit/Paper) backend platform. */
        SPIGOT,
        /** Velocity proxy platform. */
        VELOCITY,
        /** Fabric mod loader platform. */
        FABRIC,
        /** Forge mod loader platform. */
        FORGE,
        ;
    }

    /**
     * Classifies the role of the server within the network topology.
     */
    enum ServerType {
        /** The server acts as a proxy (e.g., Velocity or BungeeCord). */
        PROXY,
        /** The server acts as a backend game server (e.g., Spigot). */
        BACKEND
        ;
    }

    /**
     * Returns the platform type this extension is running on.
     *
     * @return the {@link PlatformType} constant identifying the current platform
     */
    PlatformType getPlatformType();

    /**
     * Returns the server type (proxy or backend) for this extension.
     *
     * @return the {@link ServerType} constant identifying the server's role
     */
    ServerType getServerType();

    /**
     * Fires the given {@link IProperEvent} through the platform's native event system,
     * triggering both native platform listeners and cross-platform Streamline handlers.
     *
     * @param event the proper event wrapper to fire; must not be {@code null}
     */
    void fireEvent(IProperEvent<?> event);

    /**
     * Fires the given {@link CosmicEvent} synchronously through the Streamline event bus.
     *
     * @param event the cross-platform event to fire; must not be {@code null}
     */
    void fireEvent(CosmicEvent event);

    /**
     * Fires the given {@link CosmicEvent} through the Streamline event bus, with control
     * over whether the event is dispatched asynchronously.
     *
     * @param event the cross-platform event to fire; must not be {@code null}
     * @param async {@code true} to dispatch on an async thread; {@code false} to fire synchronously
     */
    void fireEvent(CosmicEvent event, boolean async);

    /**
     * Handles a mis-synchronized event by re-dispatching it on the correct thread context.
     *
     * <p>Called when an event was fired on the wrong thread type (sync vs. async) and must
     * be corrected without losing the event data.</p>
     *
     * @param event the event that was mis-synchronized; must not be {@code null}
     * @param async {@code true} if the event should be re-fired asynchronously
     */
    void handleMisSync(CosmicEvent event, boolean async);

    /**
     * Returns the set of all currently online players as {@link CosmicPlayer} wrappers.
     *
     * @return a non-null, thread-safe set of online players
     */
    @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers();

    /**
     * Returns the usernames of all currently online players.
     *
     * @return a thread-safe set of player names; never {@code null}
     */
    ConcurrentSkipListSet<String> getOnlinePlayerNames();

    /**
     * Returns the names of all servers registered with this proxy or known to the backend.
     *
     * @return a thread-safe set of server names; never {@code null}
     */
    ConcurrentSkipListSet<String> getServerNames();

    /**
     * Checks whether the specified plugin is loaded and enabled on this server.
     *
     * @param plugin the plugin name to look up (case-sensitive)
     * @return {@code true} if the plugin is present and enabled
     */
    boolean serverHasPlugin(String plugin);

    /**
     * Returns whether the given name matches any of the registered server names.
     *
     * @param servername the server name to compare against the registered set
     * @return {@code true} if at least one registered server has this name
     */
    boolean equalsAnyServer(String servername);

    /**
     * Creates a platform-native command wrapper ({@link IProperCommand}) for the given
     * cross-platform command descriptor.
     *
     * @param command the Streamline command to adapt for this platform; must not be {@code null}
     * @return the platform-specific command object, ready to be registered
     */
    IProperCommand createCommand(CosmicCommand command);

    /**
     * Returns the configured maximum number of players this server will accept.
     *
     * @return the max player count
     */
    int getMaxPlayers();

    /**
     * Returns the connection throttle delay (in milliseconds) configured on this server.
     *
     * @return the throttle value in milliseconds, or {@code -1} if not applicable
     */
    long getConnectionThrottle();

    /**
     * Returns whether this server is running in online mode (authenticating players against
     * Mojang's session servers).
     *
     * @return {@code true} if online mode is active
     */
    boolean getOnlineMode();

    /**
     * Initiates a graceful shutdown of the server platform.
     */
    void shutdown();

    /**
     * Broadcasts a message to all players who hold the specified permission.
     *
     * @param message    the message to broadcast; must not be {@code null}
     * @param permission the permission node required to receive the broadcast; must not be {@code null}
     * @return the number of recipients who received the message
     */
    int broadcast(@NotNull String message, @NotNull String permission);

    /**
     * Returns the version string of this platform/server software.
     *
     * @return the version string; never {@code null}
     */
    String getVersion();

    /**
     * Sends the given resource pack to the specified player.
     *
     * @param resourcePack the resource pack to send; must not be {@code null}
     * @param player       the online player to send it to; must not be {@code null}
     */
    void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player);

    /**
     * Sends the given resource pack to the player identified by UUID string.
     *
     * @param resourcePack the resource pack to send; must not be {@code null}
     * @param uuid         the UUID string of the target player; must not be {@code null}
     */
    void sendResourcePack(CosmicResourcePack resourcePack, String uuid);

    /**
     * Returns the class loader used to load this platform plugin, which is suitable for
     * resolving resources and classes from the plugin's JAR.
     *
     * @return the plugin's main class loader; never {@code null}
     */
    ClassLoader getMainClassLoader();

    /**
     * Returns the name of this platform plugin as declared in its descriptor file.
     *
     * @return the plugin name; never {@code null}
     */
    String getName();

    /**
     * Returns whether this server is running in offline mode (not authenticating against
     * Mojang's session servers).
     *
     * <p>This is the logical inverse of {@link #getOnlineMode()}.</p>
     *
     * @return {@code true} if the server is in offline mode
     */
    boolean isOfflineMode();

    /**
     * Returns the {@link java.util.logging.Logger} associated with this extension, if one is
     * configured by the platform.
     *
     * @return the JUL logger, or {@code null} if the platform does not provide one
     */
    Logger getLoggerLogger();

    /**
     * Returns whether this extension has a {@link java.util.logging.Logger} available.
     *
     * @return {@code true} if {@link #getLoggerLogger()} returns a non-null value
     */
    default boolean hasLoggerLogger() {
        return getLoggerLogger() != null;
    }

    /**
     * Returns the SLF4J {@link org.slf4j.Logger} associated with this extension, if one is
     * configured by the platform.
     *
     * @return the SLF4J logger, or {@code null} if the platform does not provide one
     */
    org.slf4j.Logger getSLFLogger();

    /**
     * Returns whether this extension has an SLF4J logger available.
     *
     * @return {@code true} if {@link #getSLFLogger()} returns a non-null value
     */
    default boolean hasSLFLogger() {
        return getSLFLogger() != null;
    }

    /**
     * Marks the Singularity platform as fully enabled by invoking
     * {@link Singularity#platformEnabled(boolean)}.
     *
     * <p>Call this at the end of the platform plugin's enable sequence once all services
     * and listeners are registered.</p>
     */
    default void setPlatformAsEnabled() {
        Singularity.platformEnabled(true);
    }
}
