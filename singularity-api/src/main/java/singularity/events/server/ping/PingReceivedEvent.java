package singularity.events.server.ping;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.objects.PingedResponse;

@Setter
@Getter
public class PingReceivedEvent extends CosmicEvent {
    PingedResponse response;

    public PingReceivedEvent(PingedResponse response) {
        this.response = response;
    }
}
