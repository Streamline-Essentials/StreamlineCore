package singularity.text;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Optional;

/**
 * A per-user cache of placeholder-to-value string replacements.
 *
 * <p>Each {@code UsersReplacements} instance is associated with a single user
 * (identified by {@link #identifier}) and holds a Caffeine cache that maps
 * placeholder keys to their most recently resolved values.  Cache entries expire
 * 30 minutes after they are written, preventing stale values from persisting
 * indefinitely.
 */
@Getter @Setter
public class UsersReplacements implements Identifiable {

    /** The unique identifier (typically the user's UUID) that owns this replacement cache. */
    private String identifier;

    /** The underlying Caffeine cache that stores placeholder-to-value mappings. */
    private Cache<String, String> replacements;

    /**
     * Creates a {@code UsersReplacements} with a pre-built cache.
     *
     * @param identifier   the owner's unique identifier
     * @param replacements the Caffeine cache to use for storing replacements
     */
    public UsersReplacements(String identifier, Cache<String, String> replacements) {
        this.identifier = identifier;
        this.replacements = replacements;
    }

    /**
     * Creates a {@code UsersReplacements} with a default Caffeine cache configured
     * to expire entries 30 minutes after writing.
     *
     * @param identifier the owner's unique identifier
     */
    public UsersReplacements(String identifier) {
        this(identifier, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .build()
        );
    }

    /**
     * Adds or updates a replacement mapping.  If a mapping for {@code key} already
     * exists it is removed before the new value is inserted, effectively refreshing
     * the expiry timer.
     *
     * @param key   the placeholder key
     * @param value the resolved replacement value
     */
    public void addReplacement(String key, String value) {
        if (hasReplacement(key)) {
            removeReplacement(key);
        }
        replacements.put(key, value);
    }

    /**
     * Invalidates (removes) the mapping for the given key from the cache.
     *
     * @param key the placeholder key to remove
     */
    public void removeReplacement(String key) {
        replacements.invalidate(key);
    }

    /**
     * Returns the cached replacement value for the given key, if present.
     *
     * @param key the placeholder key to look up
     * @return an {@link Optional} containing the cached value, or empty if absent or expired
     */
    public Optional<String> getReplacement(String key) {
        return Optional.ofNullable(replacements.getIfPresent(key));
    }

    /**
     * Checks whether a non-expired cached value exists for the given key.
     *
     * @param key the placeholder key to check
     * @return {@code true} if a cached value for {@code key} is present
     */
    public boolean hasReplacement(String key) {
        return getReplacement(key).isPresent();
    }

    /**
     * Returns the cached replacement value for the given key, or a fallback if absent.
     *
     * @param key    the placeholder key to look up
     * @param orElse the value to return when no cached entry exists for {@code key}
     * @return the cached value, or {@code orElse}
     */
    public String getReplacement(String key, String orElse) {
        return getReplacement(key).orElse(orElse);
    }
}
