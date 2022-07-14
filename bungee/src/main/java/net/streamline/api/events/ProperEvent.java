package net.streamline.api.events;

import lombok.Getter;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;
import net.streamline.utils.MessagingUtils;

public class ProperEvent<T> extends AsyncEvent<T> {
        @Getter
        private final StreamlineEvent<T> event;

        public ProperEvent(StreamlineEvent<T> event) {
                super(new Callback<T>() {
                        @Override
                        public void done(T result, Throwable error) {
                                MessagingUtils.logWarning("A module tried to run a callback on an event when it is disabled!");
                        }
                });
                this.event = event;
        }
}
