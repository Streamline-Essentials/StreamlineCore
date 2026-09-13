package singularity.data.teleportation;

import gg.drak.thebase.async.AsyncUtils;
import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.redis.OwnRedisClient;
import singularity.redis.RedisMessage;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Represents a pending teleportation request that may cross server boundaries.
 *
 * <p>A {@code TPTicket} encodes either a player-to-player teleport (by UUID) or a
 * player-to-location teleport (by explicit world/position/rotation). When Redis is
 * available the ticket is broadcast over the {@value #REDIS_CHANNEL} channel so that
 * the target backend server can execute the teleport as soon as the player arrives.
 * Without Redis the ticket is persisted to the main database instead.</p>
 *
 * <p>Tickets that have not been consumed within {@link #getOldMillis()} milliseconds
 * are considered stale and can be discarded.</p>
 */
@Getter @Setter
public class TPTicket implements Identifiable {

    /**
     * The Redis pub/sub channel on which teleportation tickets are broadcast.
     */
    public static final String REDIS_CHANNEL = "tp-ticket:put";

    /**
     * The UUID of the player being teleported.
     */
    private String identifier;

    /**
     * The UUID of the target player (for player-to-player teleports), or an empty/blank
     * string for location-based teleports.
     */
    private String toIdentifier;

    /**
     * The server the player should be sent to.
     */
    private CosmicServer targetServer;

    /**
     * The world within the target server (may be {@code null} for player-to-player teleports).
     */
    private PlayerWorld targetWorld;

    /**
     * The precise X/Y/Z coordinates within the target world (may be {@code null} for
     * player-to-player teleports).
     */
    private WorldPosition targetLocation;

    /**
     * The yaw/pitch rotation applied after teleportation (may be {@code null} for
     * player-to-player teleports).
     */
    private PlayerRotation targetRotation;

    /**
     * The timestamp at which this ticket was created, used for staleness checks.
     */
    private Date createDate;

    /**
     * Constructs a fully specified {@code TPTicket} for a location-based teleport.
     *
     * @param identifier     the UUID of the player to teleport
     * @param toIdentifier   the UUID of a target player, or empty/blank for a fixed location
     * @param targetServer   the server the player should be moved to
     * @param targetWorld    the world to teleport into
     * @param targetLocation the X/Y/Z destination coordinates
     * @param targetRotation the yaw/pitch rotation to apply on arrival
     * @param createDate     the creation timestamp for this ticket
     */
    public TPTicket(String identifier, String toIdentifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation, Date createDate) {
        this.identifier = identifier;
        this.toIdentifier = toIdentifier;
        this.targetServer = targetServer;
        this.targetWorld = targetWorld;
        this.targetLocation = targetLocation;
        this.targetRotation = targetRotation;

        this.createDate = createDate;
    }

    /**
     * Constructs a player-to-player teleport ticket targeting the given server.
     * World, position, and rotation are left as {@code null} and resolved on arrival.
     *
     * @param identifier   the UUID of the player to teleport
     * @param toIdentifier the UUID of the target player
     * @param server       the server on which the target player resides
     */
    public TPTicket(String identifier, String toIdentifier, CosmicServer server) {
        this(identifier, toIdentifier, server, null, null, null, new Date());
    }

    /**
     * Constructs a player-to-player teleport ticket using a raw server identifier string.
     *
     * @param identifier       the UUID of the player to teleport
     * @param toIdentifier     the UUID of the target player
     * @param serverIdentifier the name of the target server
     */
    public TPTicket(String identifier, String toIdentifier, String serverIdentifier) {
        this(identifier, toIdentifier, new CosmicServer(serverIdentifier));
    }

    /**
     * Constructs a ticket that teleports the identified player to the given {@link CosmicPlayer}'s
     * current server.
     *
     * @param identifier the UUID of the player to teleport
     * @param player     the player whose server is used as the teleport destination
     */
    public TPTicket(String identifier, CosmicPlayer player) {
        this(identifier, player.getUuid(), player.getServer());
    }

    /**
     * Constructs a location-based ticket with an explicit creation timestamp and no target player.
     *
     * @param identifier     the UUID of the player to teleport
     * @param targetServer   the destination server
     * @param targetWorld    the destination world
     * @param targetLocation the destination X/Y/Z coordinates
     * @param targetRotation the destination rotation
     * @param createDate     the creation timestamp for this ticket
     */
    public TPTicket(String identifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation, Date createDate) {
        this(identifier, "", targetServer, targetWorld, targetLocation, targetRotation, createDate);
    }

    /**
     * Constructs a location-based ticket from a {@link CosmicLocation} with an explicit creation timestamp.
     *
     * @param identifier the UUID of the player to teleport
     * @param location   the destination location (server, world, position, and rotation)
     * @param createDate the creation timestamp for this ticket
     */
    public TPTicket(String identifier, CosmicLocation location, Date createDate) {
        this(identifier, location.getServer(), location.getWorld(), location.getPosition(), location.getRotation(), createDate);
    }

    /**
     * Constructs a location-based ticket with no target player, using {@code new Date()} as the creation time.
     *
     * @param identifier     the UUID of the player to teleport
     * @param targetServer   the destination server
     * @param targetWorld    the destination world
     * @param targetLocation the destination X/Y/Z coordinates
     * @param targetRotation the destination rotation
     */
    public TPTicket(String identifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation) {
        this(identifier, targetServer, targetWorld, targetLocation, targetRotation, new Date());
    }

    /**
     * Constructs a location-based ticket from a {@link CosmicLocation}, using {@code new Date()} as the creation time.
     *
     * @param identifier the UUID of the player to teleport
     * @param location   the destination location (server, world, position, and rotation)
     */
    public TPTicket(String identifier, CosmicLocation location) {
        this(identifier, location.getServer(), location.getWorld(), location.getPosition(), location.getRotation());
    }

    /**
     * Returns the server the player identified by {@link #getIdentifier()} is currently connected to,
     * if that player is loaded in memory.
     *
     * @return an {@link Optional} containing the player's current server, or empty if the player is not loaded
     */
    public Optional<CosmicServer> getServerOfPlayer() {
        return UserUtils.getPlayer(getIdentifier()).map(CosmicPlayer::getServer);
    }

    /**
     * Returns the target player loaded in memory, if this is a player-to-player ticket.
     *
     * @return an {@link Optional} containing the target player, or empty if no target UUID is set
     *         or the target player is not loaded
     */
    public Optional<CosmicPlayer> getToPlayer() {
        if (getToIdentifier() == null || getToIdentifier().isBlank()) return Optional.empty();
        return UserUtils.getPlayer(getToIdentifier());
    }

    /**
     * Posts this ticket, initiating the teleportation sequence.
     *
     * <p>If Redis is available the ticket is broadcast via the {@value #REDIS_CHANNEL} channel
     * (and the proxy moves the player to the target server when applicable). Without Redis the
     * ticket is persisted to the main database for the target backend to pick up.</p>
     */
    public void post() {
        if (! isUseRedis()) {
            Singularity.getMainDatabase().postTPTicketAsync(this);
        } else {
            getServerOfPlayer().ifPresent(s -> {
                UserUtils.getPlayer(getIdentifier()).ifPresent(p -> {
                    if (Singularity.isProxy() || ! s.equals(getOwnServer())) {
                        if (Singularity.isProxy()) {
                            if (! s.equals(getTargetServer())) {
                                p.connect(getTargetServer());
                            }
                        }

                        RedisMessage redisMessage = toRedisMessage(this);
                        redisMessage.send();
                    } else {
                        if (s.equals(getTargetServer())) {
                            getToPlayer().ifPresentOrElse(
                                    to -> Singularity.getInstance().getUserManager().teleport(p, to),
                                    () -> Singularity.getInstance().getUserManager().teleport(p, toLocation())
                            );
                        }
                    }
                });
            });
        }
    }

    /**
     * Constructs a {@link CosmicLocation} from this ticket's target server, world,
     * position, and rotation.
     *
     * @return the destination as a {@link CosmicLocation}
     */
    public CosmicLocation toLocation() {
        return new CosmicLocation(getTargetServer(), getTargetWorld(), getTargetLocation(), getTargetRotation());
    }

    /**
     * Removes this ticket from the database and from the in-memory pending set.
     */
    public void clear() {
        Singularity.getMainDatabase().clearTPTicketAsync(this.getIdentifier());

        unpend();
    }

    /**
     * Handles this ticket after it has been received from a Redis message.
     *
     * <p>On a proxy: moves the player to the target server if they are not already there,
     * then unpends the ticket. On a backend: if the target server is not this server the
     * ticket is unpended and ignored; otherwise the player is teleported immediately (or
     * the ticket is pended if the player is not yet online).</p>
     */
    public void onFromRedis() {
        CosmicPlayer player = UserUtils.getPlayer(getIdentifier()).orElse(null);
        if (Singularity.isProxy()) {
            if (player == null) {
                MessageUtils.logWarning("Player with UUID " + getIdentifier() + " not found for teleportation ticket.");
                clear();
                return;
            }

            if (! player.getServer().equals(getTargetServer())) {
                player.connect(getTargetServer());
            }

            unpend();
        } else {
            if (! getTargetServer().equals(getOwnServer())) {
                unpend();
                return;
            }

            if (player == null || ! player.isOnline()) {
                pend(this);
//                MessageUtils.logInfo("Player with UUID " + getIdentifier() + " is not online, pending teleportation ticket.");
            } else {
                teleportWithDelayAndClear(0);
            }
        }
    }

    /**
     * Returns the {@link CosmicServer} representing the current server instance,
     * as configured in the server settings.
     *
     * @return the local server reference
     */
    public static CosmicServer getOwnServer() {
        return GivenConfigs.getServer().getCosmicServer();
    }

    /**
     * Returns {@code true} if the Redis client is currently connected, indicating
     * that teleportation tickets should be distributed via Redis rather than the database.
     *
     * @return {@code true} when Redis is connected and available
     */
    public static boolean isUseRedis() {
        return OwnRedisClient.isConnected();
    }

    /**
     * Removes this ticket from the in-memory pending set (convenience wrapper).
     */
    public void unpend() {
        unpend(this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two tickets are equal when their {@link #getIdentifier() identifiers} match.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a {@code TPTicket} with the same identifier
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TPTicket)) return false;
        TPTicket other = (TPTicket) obj;
        return this.getIdentifier().equals(other.getIdentifier());
    }

    /**
     * Returns the number of milliseconds after which a ticket is considered stale
     * and may be discarded (currently 7 000 ms / 7 seconds).
     *
     * @return the staleness threshold in milliseconds
     */
    public static long getOldMillis() {
        return 7 * 1000; // 7 seconds
    }

    /**
     * Returns the {@link Date} at which this ticket becomes stale
     * ({@link #getCreateDate()} + {@link #getOldMillis()}).
     *
     * @return the expiry date of this ticket
     */
    public Date getOldDate() {
        return new Date(getCreateDate().getTime() + getOldMillis());
    }

    /**
     * Returns {@code true} if the current time is past this ticket's expiry date.
     *
     * @return {@code true} if this ticket is stale
     */
    public boolean isOld() {
        Date now = new Date();
        return now.after(getOldDate());
    }

    /**
     * Teleports the player identified by this ticket if they are currently online.
     * Teleports to the target player if one is specified; otherwise teleports to the
     * fixed location encoded in this ticket.
     */
    public void teleportPlayerIfOnline() {
        UserUtils.getPlayer(getIdentifier()).ifPresent(player -> {
            getToPlayer().ifPresentOrElse(
                    to -> Singularity.getInstance().getUserManager().teleport(player, to),
                    () -> Singularity.getInstance().getUserManager().teleport(player, toLocation())
            );

//            MessageUtils.logInfo("Teleported player " + player.getCurrentName() + " using TPTicket.");
        });
    }

    /**
     * Teleports the player if online (see {@link #teleportPlayerIfOnline()}) and then
     * removes this ticket from the database and the pending set.
     */
    public void teleportPlayerIfOnlineThenClear() {
        teleportPlayerIfOnline();
        clear();
    }

    /**
     * Schedules {@link #teleportPlayerIfOnlineThenClear()} to run asynchronously after
     * the given number of ticks.
     *
     * @param delayTicks the delay in server ticks before the teleport is attempted
     */
    public void teleportWithDelayAndClear(long delayTicks) {
        AsyncUtils.runAsync(this::teleportPlayerIfOnlineThenClear, delayTicks);
    }

    /**
     * Deserializes a {@code TPTicket} from a {@link RedisMessage}.
     *
     * <p>The message content is a semicolon-delimited string. Three parts indicate a
     * player-to-player ticket; eight or nine parts indicate a location-based ticket
     * (nine parts also include a target player UUID).</p>
     *
     * @param redisMessage the Redis message to parse
     * @return the deserialized {@code TPTicket}
     * @throws IllegalArgumentException if the message format is not recognized
     */
    public static TPTicket fromRedisMessage(RedisMessage redisMessage) {
        String content = redisMessage.getMessage();
        String[] parts = content.split(";");

        if (parts.length == 3) {
            String identifier = parts[0];
            String toIdentifier = parts[1];
            String serverIdentifier = parts[2];
            return new TPTicket(identifier, toIdentifier, serverIdentifier);
        } else if (parts.length == 8) {
            String identifier = parts[0];
            String serverIdentifier = parts[1];
            CosmicServer server = new CosmicServer(serverIdentifier);

            String worldName = parts[2];
            PlayerWorld targetWorld = new PlayerWorld(worldName);

            double x = Double.parseDouble(parts[3]);
            double y = Double.parseDouble(parts[4]);
            double z = Double.parseDouble(parts[5]);
            WorldPosition position = new WorldPosition(x, y, z);

            float yaw = Float.parseFloat(parts[6]);
            float pitch = Float.parseFloat(parts[7]);
            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

            return new TPTicket(identifier, server, targetWorld, position, rotation);
        } else if (parts.length == 9) {
            String identifier = parts[0];
            String serverIdentifier = parts[1];
            CosmicServer server = new CosmicServer(serverIdentifier);

            String worldName = parts[2];
            PlayerWorld targetWorld = new PlayerWorld(worldName);

            double x = Double.parseDouble(parts[3]);
            double y = Double.parseDouble(parts[4]);
            double z = Double.parseDouble(parts[5]);
            WorldPosition position = new WorldPosition(x, y, z);

            float yaw = Float.parseFloat(parts[6]);
            float pitch = Float.parseFloat(parts[7]);
            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

            String toIdentifier = parts[8];

            return new TPTicket(identifier, toIdentifier, server, targetWorld, position, rotation, new Date());
        } else {
            throw new IllegalArgumentException("Invalid Redis message format for TPTicket: " + content);
        }
    }

    /**
     * Serializes a {@code TPTicket} into a {@link RedisMessage} for broadcast over
     * the {@value #REDIS_CHANNEL} channel.
     *
     * <p>Player-to-player tickets produce a three-part payload; location-based tickets
     * produce an eight-part payload. The content is semicolon-delimited.</p>
     *
     * @param tpTicket the ticket to serialize
     * @return a {@link RedisMessage} ready to be sent
     */
    public static RedisMessage toRedisMessage(TPTicket tpTicket) {
        String content = "";
        if (tpTicket.getToIdentifier() != null && ! tpTicket.getToIdentifier().isBlank()) {
            content = String.join(";",
                    tpTicket.getIdentifier(),
                    tpTicket.getToIdentifier(),
                    tpTicket.getTargetServer().getIdentifier()
            ) + ";";
        } else {
            content = String.join(";",
                    tpTicket.getIdentifier(),
                    tpTicket.getTargetServer().getIdentifier(),
                    tpTicket.getTargetWorld().getIdentifier(),
                    String.valueOf(tpTicket.getTargetLocation().getX()),
                    String.valueOf(tpTicket.getTargetLocation().getY()),
                    String.valueOf(tpTicket.getTargetLocation().getZ()),
                    String.valueOf(tpTicket.getTargetRotation().getYaw()),
                    String.valueOf(tpTicket.getTargetRotation().getPitch())
            ) + ";";
        }

        return new RedisMessage(REDIS_CHANNEL, content);
    }

    /**
     * The global set of teleportation tickets that are waiting for the target player
     * to come online on the correct backend server.
     */
    @Getter @Setter
    private static ConcurrentSkipListSet<TPTicket> pendingTickets = new ConcurrentSkipListSet<>();

    /**
     * Adds a ticket to the pending set.  If a ticket for the same player already exists
     * it is replaced to avoid duplicate entries.
     *
     * @param ticket the ticket to pend; ignored if {@code null}
     */
    public static void pend(TPTicket ticket) {
        if (ticket == null) return;
        if (isPending(ticket)) {
            unpend(ticket); // Remove existing ticket if already pending
        }

        getPendingTickets().add(ticket);
    }

    /**
     * Removes a ticket from the pending set.
     *
     * @param ticket the ticket to remove; ignored if {@code null}
     */
    public static void unpend(TPTicket ticket) {
//        MessageUtils.logDebug("Unpending a TPTicket");

        if (ticket == null) return;
        getPendingTickets().removeIf(t -> t.equals(ticket));
    }

    /**
     * Retrieves the pending ticket for the player with the given UUID, if one exists.
     *
     * @param uuid the player's UUID
     * @return the matching {@code TPTicket}, or {@code null} if none is pending
     */
    public static TPTicket get(String uuid) {
        return getPendingTickets().stream().filter(t -> t.getIdentifier().equalsIgnoreCase(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns {@code true} if the given ticket is currently in the pending set.
     *
     * @param ticket the ticket to check
     * @return {@code true} if the ticket is pending
     */
    public static boolean isPending(TPTicket ticket) {
        return getPendingTickets().stream().anyMatch(t -> t.equals(ticket));
    }
}
