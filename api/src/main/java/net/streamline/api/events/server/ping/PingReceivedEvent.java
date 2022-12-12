package net.streamline.api.events.server.ping;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.objects.PingedResponse;

public class PingReceivedEvent extends StreamlineEvent {
    @Getter @Setter
    PingedResponse response;

    public PingReceivedEvent(PingedResponse response) {
        this.response = response;
    }
}
