package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.interfaces.IProperEvent;

import java.util.concurrent.CompletableFuture;

@Setter
@Getter
public class ProperEvent extends CompletableFuture<Void> implements IProperEvent<CompletableFuture<Void>> {
        private CompletableFuture<Void> event;
        CosmicEvent cosmicEvent;

        public ProperEvent(CosmicEvent streamlineEvent) {
                setEvent(this);
                setCosmicEvent(streamlineEvent);
        }
}
