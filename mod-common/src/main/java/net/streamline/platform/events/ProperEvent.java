package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

@Getter
@Setter
public class ProperEvent implements IProperEvent<CosmicEvent> {
    private CosmicEvent event;
    private CosmicEvent cosmicEvent;

    public ProperEvent(CosmicEvent event) {
        this.event = event;
        this.cosmicEvent = event;
    }
}
