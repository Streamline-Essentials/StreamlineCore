package singularity.redis;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class RedisHandler {
    @Getter @Setter
    private static ConcurrentSkipListSet<AbstractRedisListener> listeners = new ConcurrentSkipListSet<>();

    public static void load(AbstractRedisListener listener) {
        unload(listener);

        listeners.add(listener);
    }

    public static void unload(AbstractRedisListener listener) {
        unload(listener.getIdentifier());
    }

    public static void unload(String identifier) {
        listeners.removeIf(listener -> listener.getIdentifier().equals(identifier));
    }

    public static Optional<AbstractRedisListener> get(String identifier) {
        return listeners.stream()
                .filter(listener -> listener.getIdentifier().equals(identifier))
                .findFirst();
    }

    public static boolean isLoaded(String identifier) {
        return listeners.stream()
                .anyMatch(listener -> listener.getIdentifier().equals(identifier));
    }

    public static boolean isLoaded(AbstractRedisListener listener) {
        return isLoaded(listener.getIdentifier());
    }

    public static AbstractRedisListener getOrNull(String identifier) {
        return get(identifier).orElse(null);
    }
}
