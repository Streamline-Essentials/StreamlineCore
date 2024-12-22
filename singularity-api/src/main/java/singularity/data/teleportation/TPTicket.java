package singularity.data.teleportation;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import tv.quaint.objects.Identifiable;

import java.util.Date;

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
        Singularity.getMainDatabase().clearTPTicketAsync(getIdentifier());
    }

    public CosmicLocation toLocation() {
        return new CosmicLocation(getTargetServer(), getTargetWorld(), getTargetLocation(), getTargetRotation());
    }
}
