package singularity.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import singularity.utils.profiles.CosmicProfile;
import singularity.utils.profiles.PlayerLookup;

import java.time.Duration;
import java.util.UUID;

/**
 * Blocking name and UUID lookups, answered through {@link PlayerLookup}: cached, never sent to
 * Mojang, skipped without internet, and bounded by {@link #TIMEOUT}. Prefer {@link PlayerLookup}'s
 * futures in new code.
 */
public class UUIDFetcher {
    /** The longest a lookup here blocks its caller. */
    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Nullable
    public static UUID getUUID(@NotNull String name) {
        return PlayerLookup.await(PlayerLookup.getUuid(name), TIMEOUT).orElse(null);
    }

    @Nullable
    public static String getName(@NotNull String uuid) {
        UUID parsed;
        try {
            parsed = UUID.fromString(uuid.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return getName(parsed);
    }

    @Nullable
    public static String getName(UUID uuid) {
        return PlayerLookup.await(PlayerLookup.lookup(uuid), TIMEOUT)
                .map(CosmicProfile::getName)
                .filter(name -> ! name.isEmpty())
                .orElse(null);
    }
}
