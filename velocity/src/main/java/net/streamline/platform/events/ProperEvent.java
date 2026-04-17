package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Velocity bridge that wraps a {@link CosmicEvent} inside a {@link CompletableFuture}{@code <Void>}
 * so it can be submitted to and fired by the Velocity event bus.
 *
 * <p>The instance itself acts as both the carrier future and the
 * {@link IProperEvent} wrapper, allowing {@link BasePlugin#fireEvent(IProperEvent)} to
 * hand it directly to {@code ProxyServer#getEventManager().fire()}.
 */
@Setter
@Getter
public class ProperEvent extends CompletableFuture<Void> implements IProperEvent<CompletableFuture<Void>> {
        /**
         * The underlying {@link CompletableFuture} representing this event (self-referential).
         */
        private CompletableFuture<Void> event;

        /**
         * The cross-platform event payload being wrapped and fired.
         */
        CosmicEvent cosmicEvent;

        /**
         * Constructs a new {@code ProperEvent} that wraps the given {@link CosmicEvent}.
         *
         * @param streamlineEvent the cross-platform event to fire through the Velocity event bus
         */
        public ProperEvent(CosmicEvent streamlineEvent) {
                setEvent(this);
                setCosmicEvent(streamlineEvent);
        }
}
