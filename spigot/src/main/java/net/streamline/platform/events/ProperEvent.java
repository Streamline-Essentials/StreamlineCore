package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit {@link Event} wrapper that carries a cross-platform
 * {@link CosmicEvent} through the Bukkit event bus.
 *
 * <p>Implements {@link IProperEvent} so that the platform-agnostic Streamline
 * layer can fire events without knowing about Bukkit specifics. Each instance
 * lazily creates or retrieves a {@link HandlerList} from the static
 * {@link #handlerMap} keyed by the wrapped {@link CosmicEvent}.
 */
public class ProperEvent extends Event implements IProperEvent<Event> {
        /**
         * The Bukkit {@link Event} instance — set to {@code this} on construction
         * so that the {@link IProperEvent} contract is satisfied.
         */
        @Getter @Setter
        private Event event;

        /**
         * The cross-platform event wrapped by this Bukkit event.
         */
        @Getter @Setter
        CosmicEvent cosmicEvent;

        /**
         * Mapping from {@link CosmicEvent} instances to their associated Bukkit
         * {@link HandlerList}s, lazily populated as events are fired.
         */
        @Getter @Setter
        private static ConcurrentHashMap<CosmicEvent, HandlerList> handlerMap = new ConcurrentHashMap<>();

        /**
         * Returns a fresh, empty {@link HandlerList}.
         *
         * <p>Required by the Bukkit event API. Per-event handler lists are managed
         * through {@link #handlerMap} and returned by {@link #getHandlers()}.
         *
         * @return a new empty {@link HandlerList}
         */
        public static HandlerList getHandlerList() {
                return new HandlerList();
        }

        /**
         * Associates the given {@link HandlerList} with the specified
         * {@link CosmicEvent} in the global handler map.
         *
         * @param event the cosmic event to associate
         * @param list  the handler list to register for that event
         */
        public static void addHandler(CosmicEvent event, HandlerList list) {
                handlerMap.put(event, list);
        }

        /**
         * Constructs a synchronous {@code ProperEvent} wrapping the given
         * {@link CosmicEvent}.
         *
         * @param streamlineEvent the cross-platform event to wrap
         */
        public ProperEvent(CosmicEvent streamlineEvent) {
                this(streamlineEvent, false);
        }

        /**
         * Constructs a {@code ProperEvent} wrapping the given
         * {@link CosmicEvent}, with an optional async flag.
         *
         * @param streamlineEvent the cross-platform event to wrap
         * @param async           {@code true} if this event is being fired from an
         *                        asynchronous thread
         */
        public ProperEvent(CosmicEvent streamlineEvent, boolean async) {
                super(async);
                setEvent(this);
                setCosmicEvent(streamlineEvent);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns the {@link HandlerList} for the wrapped {@link CosmicEvent},
         * creating and caching a new list if one does not already exist.
         */
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
