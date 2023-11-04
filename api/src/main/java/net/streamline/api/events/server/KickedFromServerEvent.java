package net.streamline.api.events.server;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

@Getter
public class KickedFromServerEvent extends StreamlineEvent {
    @Setter
    private StreamlinePlayer player;
    @Setter
    private String fromServer;
    @Setter
    private String reason;
    @Setter
    private String toServer;

    public KickedFromServerEvent(StreamlinePlayer player, String fromServer, String reason, String toServer) {
        this.fromServer = fromServer;
        this.reason = reason;
        this.player = player;
        this.toServer = toServer;
    }

    public boolean isFromServerNone() {
        return this.fromServer.equalsIgnoreCase("none");
    }

    public boolean isToServerNone() {
        return this.toServer.equalsIgnoreCase("none");
    }
}
