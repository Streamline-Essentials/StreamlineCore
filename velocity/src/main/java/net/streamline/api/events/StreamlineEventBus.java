package net.streamline.api.events;

import java.util.ArrayList;
import java.util.List;

public class StreamlineEventBus {
    public interface StreamlineObserver {
        void update(StreamlineEvent<?> event);
    }

    private final List<StreamlineObserver> streamlineObservers = new ArrayList<>();

    public void notifyObservers(StreamlineEvent<?> event) {
        streamlineObservers.forEach(streamlineObserver -> streamlineObserver.update(event));
    }

    public void addObserver(StreamlineObserver streamlineObserver) {
        streamlineObservers.add(streamlineObserver);
    }
}