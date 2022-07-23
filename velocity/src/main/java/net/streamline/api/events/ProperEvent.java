package net.streamline.api.events;

import lombok.Getter;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.util.concurrent.CompletableFuture;

public class ProperEvent extends CompletableFuture<String> {
        @Getter
        private final StreamlineEvent event;

        public ProperEvent(StreamlineEvent event) {
//                super(new Callback<T>() {
//                        @Override
//                        public void done(T result, Throwable error) {
//                                MessagingUtils.logWarning("A module tried to run a callback on an event when it is disabled!");
//                        }
//                });
                this.event = event;
        }

        public void voidComplete() {
                this.complete(null);
        }
}
