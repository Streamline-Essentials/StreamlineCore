package singularity.redis;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Central registry for active {@link AbstractRedisListener} instances.
 *
 * <p>Listeners are stored in a thread-safe {@link ConcurrentSkipListSet} keyed
 * by their {@linkplain AbstractRedisListener#getIdentifier() identifier}. The
 * registry enforces uniqueness: loading a listener with a duplicate identifier
 * first removes the existing entry before adding the new one.</p>
 */
public class RedisHandler {

    /**
     * The set of all currently loaded Redis listeners.
     * Managed by Lombok-generated {@code getListeners()} / {@code setListeners()}.
     */
    @Getter @Setter
    private static ConcurrentSkipListSet<AbstractRedisListener> listeners = new ConcurrentSkipListSet<>();

    /**
     * Registers a listener in the registry, replacing any existing listener with the
     * same identifier.
     *
     * @param listener the listener to register; must not be {@code null}
     */
    public static void load(AbstractRedisListener listener) {
        unload(listener);

        listeners.add(listener);
    }

    /**
     * Removes the listener with the same identifier as {@code listener} from the
     * registry.
     *
     * @param listener the listener whose identifier is used to find the entry to remove
     */
    public static void unload(AbstractRedisListener listener) {
        unload(listener.getIdentifier());
    }

    /**
     * Removes the listener with the given identifier from the registry.
     *
     * @param identifier the identifier of the listener to remove
     */
    public static void unload(String identifier) {
        listeners.removeIf(listener -> listener.getIdentifier().equals(identifier));
    }

    /**
     * Returns the registered listener with the given identifier, if present.
     *
     * @param identifier the identifier to look up
     * @return an {@link Optional} containing the listener, or empty if not found
     */
    public static Optional<AbstractRedisListener> get(String identifier) {
        return listeners.stream()
                .filter(listener -> listener.getIdentifier().equals(identifier))
                .findFirst();
    }

    /**
     * Returns {@code true} if a listener with the given identifier is currently
     * registered.
     *
     * @param identifier the identifier to check
     * @return {@code true} if a matching listener is loaded
     */
    public static boolean isLoaded(String identifier) {
        return listeners.stream()
                .anyMatch(listener -> listener.getIdentifier().equals(identifier));
    }

    /**
     * Returns {@code true} if the given listener's identifier is currently
     * registered.
     *
     * @param listener the listener to check
     * @return {@code true} if a listener with the same identifier is loaded
     */
    public static boolean isLoaded(AbstractRedisListener listener) {
        return isLoaded(listener.getIdentifier());
    }

    /**
     * Returns the registered listener with the given identifier, or {@code null}
     * if no such listener is loaded.
     *
     * @param identifier the identifier to look up
     * @return the matching {@link AbstractRedisListener}, or {@code null}
     */
    public static AbstractRedisListener getOrNull(String identifier) {
        return get(identifier).orElse(null);
    }
}
