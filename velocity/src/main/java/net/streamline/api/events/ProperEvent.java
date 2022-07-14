package net.streamline.api.events;

import lombok.Getter;
import net.streamline.utils.MessagingUtils;

import java.util.concurrent.CompletableFuture;

public class ProperEvent<T> extends CompletableFuture<T> {
        @Getter
        private final StreamlineEvent<T> event;

        public ProperEvent(StreamlineEvent<T> event) {
//                super(new Callback<T>() {
//                        @Override
//                        public void done(T result, Throwable error) {
//                                MessagingUtils.logWarning("A module tried to run a callback on an event when it is disabled!");
//                        }
//                });
                this.event = event;
        }
}
