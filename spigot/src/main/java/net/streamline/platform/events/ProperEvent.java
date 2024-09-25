package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

import java.util.concurrent.ConcurrentHashMap;

public class ProperEvent extends Event implements IProperEvent<Event> {
        @Getter @Setter
        private Event event;
        @Getter @Setter
        CosmicEvent cosmicEvent;

        @Getter @Setter
        private static ConcurrentHashMap<CosmicEvent, HandlerList> handlerMap = new ConcurrentHashMap<>();

        public static HandlerList getHandlerList() {
                return new HandlerList();
        }

        public static void addHandler(CosmicEvent event, HandlerList list) {
                handlerMap.put(event, list);
        }

        public ProperEvent(CosmicEvent streamlineEvent) {
                this(streamlineEvent, false);
        }

        public ProperEvent(CosmicEvent streamlineEvent, boolean async) {
                super(async);
                setEvent(this);
                setCosmicEvent(streamlineEvent);
        }

        @NotNull
        @Override
        public HandlerList getHandlers() {
                HandlerList list = ProperEvent.getHandlerMap().get(this.getCosmicEvent());
                if (list == null) {
                        list = new HandlerList();
                        ProperEvent.getHandlerMap().put(this.getCosmicEvent(), list);
                }
                return list;
        }
}
