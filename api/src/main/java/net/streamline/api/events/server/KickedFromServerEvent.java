package net.streamline.api.events.server;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class KickedFromServerEvent extends StreamlineEvent {
    @Getter @Setter
    private StreamlinePlayer player;
    @Getter @Setter
    private String fromServer;
    @Getter @Setter
    private String reason;
    @Getter @Setter
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
