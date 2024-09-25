package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Event;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

@Setter
@Getter
public class ProperEvent extends Event implements IProperEvent<Event> {
        private Event event;
        CosmicEvent cosmicEvent;

        public ProperEvent(CosmicEvent streamlineEvent) {
                setEvent(this);
                setCosmicEvent(streamlineEvent);
        }
}
