package singularity.events.server.ping;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.objects.PingedResponse;

@Setter
@Getter
public class PingReceivedEvent extends CosmicEvent {
    private PingedResponse response;
    private String hostname;

    public PingReceivedEvent(PingedResponse response, String hostname) {
        this.response = response;
        this.hostname = hostname;
    }
}
