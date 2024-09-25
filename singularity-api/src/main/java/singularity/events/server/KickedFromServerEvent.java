package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.StreamSenderEvent;

@Setter
@Getter
public class KickedFromServerEvent extends StreamSenderEvent {
    private String fromServer;
    private String reason;
    private String toServer;

    public KickedFromServerEvent(CosmicPlayer player, String fromServer, String reason, String toServer) {
        super(player);
        this.fromServer = fromServer;
        this.reason = reason;
        this.toServer = toServer;
    }

    public boolean isFromServerNone() {
        return this.fromServer.equalsIgnoreCase("none");
    }

    public boolean isToServerNone() {
        return this.toServer.equalsIgnoreCase("none");
    }
}
