package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Event;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

/**
 * BungeeCord-specific wrapper that bridges a cross-platform {@link CosmicEvent}
 * into the BungeeCord {@link Event} system.
 *
 * <p>When fired through the BungeeCord plugin manager this event is intercepted
 * by {@link net.streamline.platform.listeners.PlatformListener}, which then
 * delegates to the {@link singularity.modules.ModuleManager}.
 */
@Setter
@Getter
public class ProperEvent extends Event implements IProperEvent<Event> {

        /** The underlying BungeeCord event; set to {@code this} on construction. */
        private Event event;

        /** The cross-platform event carried by this wrapper. */
        CosmicEvent cosmicEvent;

        /**
         * Constructs a new {@code ProperEvent} wrapping the given cross-platform event.
         *
         * @param streamlineEvent the {@link CosmicEvent} to wrap
         */
        public ProperEvent(CosmicEvent streamlineEvent) {
                setEvent(this);
                setCosmicEvent(streamlineEvent);
        }
}
