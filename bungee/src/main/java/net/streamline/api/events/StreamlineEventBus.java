package net.streamline.api.events;

import lombok.Getter;
import net.streamline.api.modules.BundledModule;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class StreamlineEventBus {
    public abstract static class StreamlineObserver {
        public StreamlineObserver() {
            Streamline.getStreamlineEventBus().addObserver(this);
        }

        protected abstract void update(StreamlineEvent<?> event);
    }

    public abstract static class ModularizedObserver extends StreamlineObserver {
        @Getter
        private final BundledModule module;

        public ModularizedObserver(BundledModule module) {
            super();
            this.module = module;
            this.module.logInfo("Registered ModularizedObserver '" + getClass().getSimpleName() + "'!");
        }
    }

    private final List<StreamlineObserver> streamlineObservers = new ArrayList<>();

    public void notifyObservers(StreamlineEvent<?> event) {
        streamlineObservers.forEach(streamlineObserver -> streamlineObserver.update(event));
    }

    public void addObserver(StreamlineObserver streamlineObserver) {
        streamlineObservers.add(streamlineObserver);
    }
}