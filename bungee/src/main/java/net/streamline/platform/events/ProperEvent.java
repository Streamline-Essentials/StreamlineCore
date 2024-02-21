package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Event;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;

@Setter
@Getter
public class ProperEvent extends Event implements IProperEvent<Event> {
        private Event event;
        StreamlineEvent streamlineEvent;

        public ProperEvent(StreamlineEvent streamlineEvent) {
                setEvent(this);
                setStreamlineEvent(streamlineEvent);
        }
}
