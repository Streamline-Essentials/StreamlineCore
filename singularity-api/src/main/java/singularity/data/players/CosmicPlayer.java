package singularity.data.players;

import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.events.SaveSenderEvent;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.server.CosmicServer;
import singularity.database.CoreDBOperator;
import singularity.interfaces.audiences.real.RealPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a cross-platform Minecraft player tracked by the Streamline framework.
 *
 * <p>{@code CosmicPlayer} extends {@link CosmicSender} with player-specific data
 * such as the current IP address and in-world location. All location information
 * (world, coordinates, yaw, pitch, server) is delegated to a backing
 * {@link CosmicLocation} instance so that mutations remain consistent.</p>
 *
 * <p>Instances are normally created by the platform's {@code UserManager} during
 * login and are persisted to the configured database via {@link #save()}.</p>
 */
public class CosmicPlayer extends CosmicSender {

    /** The most recently recorded IP address of this player. May be spoofed per config. */
    private String currentIp;

    /**
     * The in-world location of this player, including world, coordinates, and server.
     * Lombok-generated setter is used by {@link #augmentMore} and internal reload logic.
     */
    @Setter
    private CosmicLocation location;

    /**
     * Constructs a {@code CosmicPlayer} with the given UUID, optionally marking it
     * as a temporary (non-persisted) player record.
     *
     * @param uuid      the unique identifier of the player
     * @param temporary {@code true} if this record should not be persisted to the database
     */
    public CosmicPlayer(String uuid, boolean temporary) {
        super(uuid, temporary);

        setServerName("");

        this.currentIp = "";

        this.location = new CosmicLocation(this);
    }

    /**
     * Constructs a persistent {@code CosmicPlayer} with the given UUID.
     *
     * @param uuid the unique identifier of the player
     */
    public CosmicPlayer(String uuid) {
        this(uuid, false);
    }

    /**
     * Returns the current {@link CosmicLocation} for this player, lazily initialising
     * it if it was set to {@code null} externally.
     *
     * @return the non-null location of this player
     */
    public CosmicLocation getLocation() {
        if (location == null) {
            location = new CosmicLocation(this);
        }

        return location;
    }

    /**
     * Sets the current IP address for this player, respecting the IP-spoofing
     * configuration. If IP spoofing is enabled the stored value is always the
     * configured spoofed IP. If the supplied address is blank or {@code null} and
     * spoofing is disabled, the address is resolved from the platform's
     * {@code UserManager}.
     *
     * @param currentIP the raw IP address string to store, or {@code null}/{@code ""}
     *                  to trigger a platform lookup
     * @return this {@link CosmicSender} instance for chaining
     */
    public CosmicSender setCurrentIp(String currentIP) {
        String processed = currentIP;

        if (! GivenConfigs.getMainConfig().isSpoofIPs()) {
            if (processed == null || processed.isBlank() || processed.isEmpty()) {
                processed = Singularity.getInstance().getUserManager().parsePlayerIP(getUuid());
            }
        } else {
            processed = GivenConfigs.getMainConfig().getSpoofedIP();
        }

        this.currentIp = processed;
        return this;
    }

    /**
     * Returns the effective IP address of this player. If IP spoofing is configured,
     * the spoofed address is always returned; otherwise the stored address is returned.
     *
     * @return the effective IP address string, never {@code null}
     */
    public String getCurrentIp() {
        if (! GivenConfigs.getMainConfig().isSpoofIPs()) {
            return this.currentIp;
        } else {
            return GivenConfigs.getMainConfig().getSpoofedIP();
        }
    }

    /**
     * Copies player-specific data (IP and location) from the supplied {@code player}
     * into this instance, then refreshes the IP and server from the live platform state.
     *
     * @param player the source player whose data should be merged into this instance
     */
    @Override
    public void augmentMore(CosmicPlayer player) {
        setCurrentIp(player.getCurrentIp());
        setLocation(player.getLocation());

        setCurrentIpAsProper(); // might need to be forced... need to check this...
        setCurrentServerAsProper(); // might need to be forced... need to check this...
    }

    /**
     * Refreshes the stored IP address by querying the platform's {@code UserManager}
     * for the current connection IP of this player's UUID.
     */
    public void setCurrentIpAsProper() {
        setCurrentIp(Singularity.getInstance().getUserManager().parsePlayerIP(getUuid()));
    }

    /**
     * Returns the primary database operator used to persist and load player data.
     *
     * @return the {@link CoreDBOperator} for the main Streamline database
     */
    public CoreDBOperator getDatabase() {
        return Singularity.getMainDatabase();
    }

    /**
     * Checks whether a persisted record for this player exists in the database.
     *
     * @return {@code true} if a saved record for this player's UUID can be found
     */
    public boolean exists() {
        return UserUtils.userExists(this.getUuid());
    }

    /**
     * Synchronises the player's display name, IP address, and current server with
     * the live platform state. This should be called before persisting data to
     * ensure stale values are not written.
     */
    public void ensureCorrect() {
        setCurrentNameAsProper();
        setCurrentIpAsProper();
        setCurrentServerAsProper();
    }

    /**
     * Refreshes the stored server name by querying the platform's {@code UserManager}
     * for the server this player is currently on.
     */
    public void setCurrentServerAsProper() {
        setServerName(Singularity.getInstance().getUserManager().getServerPlayerIsOn(getUuid()));
    }

    /**
     * Saves this player to the database after synchronising live platform data,
     * then fires a {@link SaveSenderEvent} to notify listeners.
     */
    @Override
    public void save() {
        ensureCorrect();

        getDatabase().savePlayer(this);

        new SaveSenderEvent(this).fire();
    }

    /**
     * Returns {@code true} if the player is currently connected to the server network
     * according to the platform's {@code UserManager}.
     *
     * @return {@code true} if the player is online
     */
    @Override
    public boolean isOnline() {
        return Singularity.getInstance().getUserManager().isOnline(this.getUuid());
    }

    /**
     * Returns the platform-specific {@link RealPlayer} wrapper for this player,
     * looked up by UUID from the active Singularity instance.
     *
     * @return the {@link RealPlayer} instance, or {@code null} if the player is offline
     */
    @Override
    public RealPlayer<?> asReal() {
        return Singularity.getPlayerFromUuid(this.getUuid());
    }

    /**
     * Returns the {@link PlayerWorld} associated with this player's current location.
     *
     * @return the world the player is currently in
     */
    public PlayerWorld getWorld() {
        return getLocation().getWorld();
    }

    /**
     * Sets the world component of this player's current location.
     *
     * @param world the new {@link PlayerWorld} to assign
     */
    public void setWorld(PlayerWorld world) {
        getLocation().setWorld(world);
    }

    /**
     * Returns the name of the world this player is currently in.
     *
     * @return the world name string
     */
    public String getWorldName() {
        return getLocation().getWorldName();
    }

    /**
     * Sets the world name on this player's current location.
     *
     * @param worldName the name of the target world
     */
    public void setWorldName(String worldName) {
        getLocation().setWorldName(worldName);
    }

    /**
     * Returns the X coordinate of this player's current location.
     *
     * @return the X coordinate
     */
    public double getX() {
        return getLocation().getX();
    }

    /**
     * Sets the X coordinate on this player's current location.
     *
     * @param x the new X coordinate
     */
    public void setX(double x) {
        getLocation().setX(x);
    }

    /**
     * Returns the Y coordinate of this player's current location.
     *
     * @return the Y coordinate
     */
    public double getY() {
        return getLocation().getY();
    }

    /**
     * Sets the Y coordinate on this player's current location.
     *
     * @param y the new Y coordinate
     */
    public void setY(double y) {
        getLocation().setY(y);
    }

    /**
     * Returns the Z coordinate of this player's current location.
     *
     * @return the Z coordinate
     */
    public double getZ() {
        return getLocation().getZ();
    }

    /**
     * Sets the Z coordinate on this player's current location.
     *
     * @param z the new Z coordinate
     */
    public void setZ(double z) {
        getLocation().setZ(z);
    }

    /**
     * Returns the yaw (horizontal rotation) of this player's current location.
     *
     * @return the yaw in degrees
     */
    public float getYaw() {
        return getLocation().getYaw();
    }

    /**
     * Sets the yaw (horizontal rotation) on this player's current location.
     *
     * @param yaw the new yaw in degrees
     */
    public void setYaw(float yaw) {
        getLocation().setYaw(yaw);
    }

    /**
     * Returns the pitch (vertical rotation) of this player's current location.
     *
     * @return the pitch in degrees
     */
    public float getPitch() {
        return getLocation().getPitch();
    }

    /**
     * Sets the pitch (vertical rotation) on this player's current location.
     *
     * @param pitch the new pitch in degrees
     */
    public void setPitch(float pitch) {
        getLocation().setPitch(pitch);
    }

    /**
     * Asynchronously reloads this player's persisted data from the database,
     * overwriting the in-memory fields (join dates, play time, meta, permissions,
     * IP, and location) with the stored values if a record is found.
     */
    @Override
    public void reload() {
        CompletableFuture.runAsync(() -> {
            Optional<CosmicSender> optional = Singularity.getMainDatabase().loadPlayer(getUuid()).join();
            if (optional.isEmpty()) return;
            CosmicSender sender = optional.get();
            if (! (sender instanceof CosmicPlayer)) return;
            CosmicPlayer streamPlayer = (CosmicPlayer) sender;

            setFirstJoinMillis(streamPlayer.getFirstJoinDate().getTime());
            setLastJoinMillis(streamPlayer.getLastJoinDate().getTime());
            setLastQuitMillis(streamPlayer.getLastQuitDate().getTime());
            setPlaySeconds(streamPlayer.getPlaySeconds());
            setMeta(streamPlayer.getMeta());
            setPermissions(streamPlayer.getPermissions());

            setCurrentIp(streamPlayer.getCurrentIp());
            setLocation(streamPlayer.getLocation());
        });
    }

    /**
     * Returns the total play time in minutes, derived from the stored play-seconds counter.
     *
     * @return play time in minutes as a {@code double}
     */
    public double getPlayMinutes() {
        return getPlaySeconds() / 60d;
    }

    /**
     * Returns the total play time in minutes formatted to two decimal places.
     *
     * @return a formatted string representation of the play time in minutes
     */
    public String getPlayMinutesAsString() {
        return String.format("%.2f", getPlayMinutes());
    }

    /**
     * Returns the total play time in hours, derived from the play-minutes value.
     *
     * @return play time in hours as a {@code double}
     */
    public double getPlayHours() {
        return getPlayMinutes() / 60d;
    }

    /**
     * Returns the total play time in hours formatted to two decimal places.
     *
     * @return a formatted string representation of the play time in hours
     */
    public String getPlayHoursAsString() {
        return String.format("%.2f", getPlayHours());
    }

    /**
     * Returns the total play time in days, derived from the play-hours value.
     *
     * @return play time in days as a {@code double}
     */
    public double getPlayDays() {
        return getPlayHours() / 24d;
    }

    /**
     * Returns the total play time in days formatted to two decimal places.
     *
     * @return a formatted string representation of the play time in days
     */
    public String getPlayDaysAsString() {
        return String.format("%.2f", getPlayDays());
    }

    /**
     * Returns the raw play-seconds counter as a string.
     *
     * @return the play seconds as a plain decimal string
     */
    public String getPlaySecondsAsString() {
        return String.valueOf(getPlaySeconds());
    }

    /**
     * Queries the platform's {@code UserManager} for the name of the server this
     * player is currently on, bypassing any cached value.
     *
     * @return the live server name, or an empty string if not connected
     */
    public String getRealServer() {
        return Singularity.getInstance().getUserManager().getServerPlayerIsOn(getUuid());
    }

    /**
     * Updates the stored server name to the live value returned by {@link #getRealServer()}.
     */
    public void setServerToRealServer() {
        setServerName(getRealServer());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the player's {@link CosmicLocation}, keeping server and location
     * state consistent.</p>
     *
     * @param server the server to assign
     */
    @Override
    public void setServer(CosmicServer server) {
        getLocation().setServer(server);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the player's {@link CosmicLocation}.</p>
     *
     * @param serverName the name of the server to assign
     */
    @Override
    public void setServerName(String serverName) {
        getLocation().setServerName(serverName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the player's {@link CosmicLocation}.</p>
     *
     * @return the server the player is currently on
     */
    @Override
    public CosmicServer getServer() {
        return getLocation().getServer();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the player's {@link CosmicLocation}.</p>
     *
     * @return the name of the server the player is currently on
     */
    @Override
    public String getServerName() {
        return getLocation().getServerName();
    }
}
