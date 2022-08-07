package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Event;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;

public class ProperEvent extends Event implements IProperEvent<Event> {
        @Getter @Setter
        private Event event;
        @Getter @Setter
        StreamlineEvent streamlineEvent;

        public ProperEvent(StreamlineEvent streamlineEvent) {
                setEvent(this);
                setStreamlineEvent(streamlineEvent);
        }
}
