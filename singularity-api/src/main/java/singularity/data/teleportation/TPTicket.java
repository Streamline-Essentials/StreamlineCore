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
import singularity.modules.ModuleUtils;
import singularity.redis.OwnRedisClient;
import singularity.redis.RedisMessage;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Date;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class TPTicket implements Identifiable {
    public static final String REDIS_CHANNEL = "tp-ticket:put";

    private String identifier;
    private CosmicServer targetServer;
    private PlayerWorld targetWorld;
    private WorldPosition targetLocation;
    private PlayerRotation targetRotation;

    private Date createDate;

    public TPTicket(String identifier, CosmicServer targetServer, PlayerWorld targetWorld, WorldPosition targetLocation, PlayerRotation targetRotation, Date createDate) {
        this.identifier = identifier;
        this.targetServer = targetServer;
        this.targetWorld = targetWorld;
        this.targetLocation = targetLocation;
        this.targetRotation = targetRotation;

        this.createDate = createDate;
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

    public void post() {
        if (! isUseRedis()) {
            Singularity.getMainDatabase().postTPTicketAsync(this);
        } else {
            RedisMessage redisMessage = toRedisMessage(this);
            redisMessage.send();

            onFromRedis();
        }
    }

    public CosmicLocation toLocation() {
        return new CosmicLocation(getTargetServer(), getTargetWorld(), getTargetLocation(), getTargetRotation());
    }

    public void clear() {
        Singularity.getMainDatabase().clearTPTicketAsync(this.getIdentifier());

        unpend(this);
    }

    public void onFromRedis() {
        CosmicServer server = this.getTargetServer();
        String uuid = this.getIdentifier();
        CosmicPlayer player = UserUtils.getPlayer(uuid).orElse(null);
        if (Singularity.isProxy()) {
            if (player == null) {
                MessageUtils.logWarning("Player with UUID " + uuid + " not found for teleportation ticket.");
                return;
            }

            if (! player.getServer().equals(server)) {
                ModuleUtils.connect(player, server.getIdentifier());
            }

            clear();
        } else {
            if (! server.equals(getOwnServer())) {
                MessageUtils.logDebug("&cTPTicket on Redis&f: &dTPTicket &ffor &d" + uuid + " &fis not for this server, clearing it.");
                return;
            }

            if (player == null) {
                pend(this);
            } else {
                Singularity.getInstance().getUserManager().teleport(player, this.toLocation());

                clear();
            }
        }
    }

    public static CosmicServer getOwnServer() {
        return GivenConfigs.getServer().getCosmicServer();
    }

    public static boolean isUseRedis() {
        return OwnRedisClient.isConnected();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TPTicket)) return false;
        TPTicket other = (TPTicket) obj;
        return this.getCreateDate().equals(other.getCreateDate());
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

    public static TPTicket fromRedisMessage(RedisMessage redisMessage) {
        String content = redisMessage.getMessage();
        String[] parts = content.split(";");

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
    }

    public static RedisMessage toRedisMessage(TPTicket tpTicket) {
        String content = String.join(";",
                tpTicket.getIdentifier(),
                tpTicket.getTargetServer().getIdentifier(),
                tpTicket.getTargetWorld().getIdentifier(),
                String.valueOf(tpTicket.getTargetLocation().getX()),
                String.valueOf(tpTicket.getTargetLocation().getY()),
                String.valueOf(tpTicket.getTargetLocation().getZ()),
                String.valueOf(tpTicket.getTargetRotation().getYaw()),
                String.valueOf(tpTicket.getTargetRotation().getPitch())
        ) + ";";

        return new RedisMessage(REDIS_CHANNEL, content);
    }

    @Getter @Setter
    private static ConcurrentSkipListSet<TPTicket> pendingTickets = new ConcurrentSkipListSet<>();

    public static void pend(TPTicket ticket) {
        if (ticket == null) return;
        pendingTickets.add(ticket);
    }

    public static void unpend(TPTicket ticket) {
        if (ticket == null) return;
        getPendingTickets().removeIf(t -> t.equals(ticket));
    }

    public static TPTicket get(Date createDate) {
        return getPendingTickets().stream().filter(t -> t.getCreateDate().equals(createDate))
                .findFirst()
                .orElse(null);
    }

    public static boolean isPending(TPTicket ticket) {
        return getPendingTickets().stream().anyMatch(t -> t.equals(ticket));
    }
}
