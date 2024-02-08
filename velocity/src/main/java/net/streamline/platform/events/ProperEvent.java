package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;

import java.util.concurrent.CompletableFuture;

@Getter
public class ProperEvent extends CompletableFuture<Void> implements IProperEvent<CompletableFuture<Void>> {
        @Setter
        private CompletableFuture<Void> event;
        @Setter
        StreamlineEvent streamlineEvent;

        public ProperEvent(StreamlineEvent streamlineEvent) {
                setEvent(this);
                setStreamlineEvent(streamlineEvent);
        }
}
