package net.streamline.api.events;

import lombok.Getter;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.util.concurrent.CompletableFuture;

public class ProperEvent extends AsyncEvent<ProperEvent> {
        @Getter
        private final StreamlineEvent event;

        public ProperEvent(StreamlineEvent event) {
                super((result, error) -> {
                        if (error != null) MessagingUtils.logWarning("ProperEvent of '" + event.getEventName() + "' threw an error: " + error.getMessage());
                });
                this.event = event;
        }

        public void voidComplete() {
                this.completeIntent(Streamline.getInstance());
        }
}
