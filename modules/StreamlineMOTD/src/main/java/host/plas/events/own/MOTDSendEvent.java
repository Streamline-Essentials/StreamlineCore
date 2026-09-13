package host.plas.events.own;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.events.server.ping.PingReceivedEvent;
import singularity.objects.PingedResponse;

@Getter @Setter
public class MOTDSendEvent extends CosmicEvent {
    private PingReceivedEvent parentEvent;

    public MOTDSendEvent(PingReceivedEvent parentEvent) {
        this.parentEvent = parentEvent;
    }

    public PingedResponse getResponse() {
        return parentEvent.getResponse();
    }

    public void setResponse(PingedResponse response) {
        parentEvent.setResponse(response);
    }
}
