package net.streamline.platform.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.IProperEvent;

import java.util.concurrent.CompletableFuture;

public class ProperEvent extends CompletableFuture<Void> implements IProperEvent<CompletableFuture<Void>> {
        @Getter @Setter
        private CompletableFuture<Void> event;
        @Getter @Setter
        StreamlineEvent streamlineEvent;

        public ProperEvent(StreamlineEvent streamlineEvent) {
                setEvent(this);
                setStreamlineEvent(streamlineEvent);
        }
}
