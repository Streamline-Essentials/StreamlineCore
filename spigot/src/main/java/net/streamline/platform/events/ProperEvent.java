package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

public class ProperEvent extends Event implements IProperEvent<Event> {
        @Getter @Setter
        private Event event;
        @Getter @Setter
        StreamlineEvent streamlineEvent;

        @Getter @Setter
        private static ConcurrentHashMap<StreamlineEvent, HandlerList> handlerMap = new ConcurrentHashMap<>();

        public static void addHandler(StreamlineEvent event, HandlerList list) {
                handlerMap.put(event, list);
        }

        public ProperEvent(StreamlineEvent streamlineEvent) {
                super(true);
                setEvent(this);
                setStreamlineEvent(streamlineEvent);
        }

        @NotNull
        @Override
        public HandlerList getHandlers() {
                return ProperEvent.getHandlerMap().get(this.getStreamlineEvent());
        }
}
