package singularity.data.teleportation;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.redis.RedisClient;
import singularity.redis.RedisMessage;

import java.util.Date;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class TPTicket implements Identifiable {
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
        Singularity.getMainDatabase().postTPTicketAsync(this);
    }

    public void clear() {
        if (! isUseRedis()) {
            Singularity.getMainDatabase().clearTPTicketAsync(getIdentifier());
        } else {
            removeTicket(this);
        }
    }

    public CosmicLocation toLocation() {
        return new CosmicLocation(getTargetServer(), getTargetWorld(), getTargetLocation(), getTargetRotation());
    }

    public static boolean isUseRedis() {
        return RedisClient.isConnected();
    }

    public static TPTicket fromRedisMessage(RedisMessage redisMessage) {
        String content = redisMessage.getMessage();
        String[] parts = content.split(";");

        String identifier = parts[0];
        CosmicServer server = new CosmicServer(parts[1]);

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

        return new RedisMessage("tp-ticket:put", content);
    }

    @Getter @Setter
    private static ConcurrentSkipListSet<TPTicket> tickets = new ConcurrentSkipListSet<>();

    public static void addTicket(TPTicket ticket) {
        if (ticket != null) {
            getTickets().add(ticket);
        }
    }

    public static void removeTicket(TPTicket ticket) {
        if (ticket != null) {
            getTickets().removeIf(t -> t.getIdentifier().equalsIgnoreCase(ticket.getIdentifier()));
        }
    }
}
