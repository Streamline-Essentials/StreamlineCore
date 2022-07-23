package net.streamline.api.events;

import lombok.Getter;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.util.concurrent.CompletableFuture;

public class ProperEvent extends AsyncEvent<Void> {
        @Getter
        private final StreamlineEvent event;

        public ProperEvent(StreamlineEvent event) {
                super(new Callback<Void>() {
                        @Override
                        public void done(Void result, Throwable error) {
                                MessagingUtils.logWarning("A module tried to run a callback on an event when it is disabled!");
                        }
                });
                this.event = event;
        }

        public void voidComplete() {
                this.completeIntent(Streamline.getInstance());
        }
}
