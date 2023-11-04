package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Event;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;

@Getter
public class ProperEvent extends Event implements IProperEvent<Event> {
        @Setter
        private Event event;
        @Setter
        StreamlineEvent streamlineEvent;

        public ProperEvent(StreamlineEvent streamlineEvent) {
                setEvent(this);
                setStreamlineEvent(streamlineEvent);
        }
}
