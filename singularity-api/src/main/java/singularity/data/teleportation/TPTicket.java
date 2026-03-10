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

@Getter @Setter
public class TPTicket implements Identifiable {
    public static final String REDIS_CHANNEL = "tp-ticket:put";

    private String identifier;
    private String toIdentifier;
    private CosmicServer targetServer;
    private PlayerWorld targetWorld;
    private WorldPosition targetLocation;
    private PlayerRotation targetRotation;

    private Date createDate;

    public TPTicket(String identifier, String toIdentifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation, Date createDate) {
        this.identifier = identifier;
        this.toIdentifier = toIdentifier;
        this.targetServer = targetServer;
        this.targetWorld = targetWorld;
        this.targetLocation = targetLocation;
        this.targetRotation = targetRotation;

        this.createDate = createDate;
    }

    public TPTicket(String identifier, String toIdentifier, CosmicServer server) {
        this(identifier, toIdentifier, server, null, null, null, new Date());
    }

    public TPTicket(String identifier, String toIdentifier, String serverIdentifier) {
        this(identifier, toIdentifier, new CosmicServer(serverIdentifier));
    }

    public TPTicket(String identifier, CosmicPlayer player) {
        this(identifier, player.getUuid(), player.getServer());
    }

    public TPTicket(String identifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation, Date createDate) {
        this(identifier, "", targetServer, targetWorld, targetLocation, targetRotation, createDate);
    }

    public TPTicket(String identifier, CosmicLocation location, Date createDate) {
        this(identifier, location.getServer(), location.getWorld(), location.getPosition(), location.getRotation(), createDate);
    }

    public TPTicket(String identifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation) {
        this(identifier, targetServer, targetWorld, targetLocation, targetRotation, new Date());
    }

    public TPTicket(String identifier, CosmicLocation location) {
        this(identifier, location.getServer(), location.getWorld(), location.getPosition(), location.getRotation());
    }

    public Optional<CosmicServer> getServerOfPlayer() {
        return UserUtils.getPlayer(getIdentifier()).map(CosmicPlayer::getServer);
    }

    public Optional<CosmicPlayer> getToPlayer() {
        if (getToIdentifier() == null || getToIdentifier().isBlank()) return Optional.empty();
        return UserUtils.getPlayer(getToIdentifier());
    }

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

    public CosmicLocation toLocation() {
        return new CosmicLocation(getTargetServer(), getTargetWorld(), getTargetLocation(), getTargetRotation());
    }

    public void clear() {
        Singularity.getMainDatabase().clearTPTicketAsync(this.getIdentifier());

        unpend();
    }

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

    public static CosmicServer getOwnServer() {
        return GivenConfigs.getServer().getCosmicServer();
    }

    public static boolean isUseRedis() {
        return OwnRedisClient.isConnected();
    }

    public void unpend() {
        unpend(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TPTicket)) return false;
        TPTicket other = (TPTicket) obj;
        return this.getIdentifier().equals(other.getIdentifier());
    }

    public static long getOldMillis() {
        return 7 * 1000; // 7 seconds
    }

    public Date getOldDate() {
        return new Date(getCreateDate().getTime() + getOldMillis());
    }

    public boolean isOld() {
        Date now = new Date();
        return now.after(getOldDate());
    }

    public void teleportPlayerIfOnline() {
        UserUtils.getPlayer(getIdentifier()).ifPresent(player -> {
            getToPlayer().ifPresentOrElse(
                    to -> Singularity.getInstance().getUserManager().teleport(player, to),
                    () -> Singularity.getInstance().getUserManager().teleport(player, toLocation())
            );

//            MessageUtils.logInfo("Teleported player " + player.getCurrentName() + " using TPTicket.");
        });
    }

    public void teleportPlayerIfOnlineThenClear() {
        teleportPlayerIfOnline();
        clear();
    }

    public void teleportWithDelayAndClear(long delayTicks) {
        AsyncUtils.runAsync(this::teleportPlayerIfOnlineThenClear, delayTicks);
    }

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

    @Getter @Setter
    private static ConcurrentSkipListSet<TPTicket> pendingTickets = new ConcurrentSkipListSet<>();

    public static void pend(TPTicket ticket) {
        if (ticket == null) return;
        if (isPending(ticket)) {
            unpend(ticket); // Remove existing ticket if already pending
        }

        getPendingTickets().add(ticket);
    }

    public static void unpend(TPTicket ticket) {
//        MessageUtils.logDebug("Unpending a TPTicket");

        if (ticket == null) return;
        getPendingTickets().removeIf(t -> t.equals(ticket));
    }

    public static TPTicket get(String uuid) {
        return getPendingTickets().stream().filter(t -> t.getIdentifier().equalsIgnoreCase(uuid))
                .findFirst()
                .orElse(null);
    }

    public static boolean isPending(TPTicket ticket) {
        return getPendingTickets().stream().anyMatch(t -> t.equals(ticket));
    }
}
