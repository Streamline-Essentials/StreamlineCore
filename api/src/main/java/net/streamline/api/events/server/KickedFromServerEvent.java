package net.streamline.api.events.server;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.events.StreamSenderEvent;

@Getter
public class KickedFromServerEvent extends StreamSenderEvent {
    @Setter
    private String fromServer;
    @Setter
    private String reason;
    @Setter
    private String toServer;

    public KickedFromServerEvent(StreamPlayer player, String fromServer, String reason, String toServer) {
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
