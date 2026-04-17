package singularity.data.uuid;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory registry that maps player UUIDs to their associated {@link UuidInfo}
 * records.
 *
 * <p>{@code UuidManager} keeps a thread-safe set of all currently cached
 * {@link UuidInfo} objects and provides static helpers for registering,
 * unregistering, and querying them. It also handles loading and persisting UUID
 * data through the main database when a player connects.</p>
 */
@Getter @Setter
public class UuidManager {

    /**
     * The set of all currently cached {@link UuidInfo} instances. Thread-safe via
     * {@link ConcurrentSkipListSet}.
     */
    @Getter @Setter
    private static ConcurrentSkipListSet<UuidInfo> uuids = new ConcurrentSkipListSet<>();

    /**
     * Adds a {@link UuidInfo} to the in-memory registry. If an entry for the same
     * UUID is already present it is removed first to avoid duplicates.
     *
     * @param uuidInfo the UUID info to register; must not be {@code null}
     */
    public static void registerUuid(UuidInfo uuidInfo) {
        if (uuids.contains(uuidInfo)) unregisterUuid(uuidInfo);
        uuids.add(uuidInfo);
    }

    /**
     * Removes the given {@link UuidInfo} from the in-memory registry.
     *
     * @param uuidInfo the UUID info to remove
     * @return an {@link Optional} containing the removed instance, or empty if it
     *         was not found in the registry
     */
    public static Optional<UuidInfo> unregisterUuid(UuidInfo uuidInfo) {
        if (! uuids.contains(uuidInfo)) return Optional.empty();
        uuids.removeIf(uuidInfo1 -> uuidInfo1.getUuid().equals(uuidInfo.getUuid()));
        return Optional.of(uuidInfo);
    }

    /**
     * Looks up the cached {@link UuidInfo} for a given UUID string.
     *
     * @param uuid the UUID string to search for
     * @return an {@link Optional} containing the matching info, or empty if none is
     *         cached
     */
    public static Optional<UuidInfo> getUuid(String uuid) {
        return uuids.stream().filter(uuidInfo -> uuidInfo.getUuid().equals(uuid)).findFirst();
    }

    /**
     * Removes the cached {@link UuidInfo} entry identified by the given UUID string.
     *
     * @param uuid the UUID string whose entry should be removed
     * @return an {@link Optional} containing the removed info, or empty if no entry
     *         existed for that UUID
     */
    public static Optional<UuidInfo> unregister(String uuid) {
        AtomicReference<Optional<UuidInfo>> uuidInfo = new AtomicReference<>(Optional.empty());
        getUuid(uuid).ifPresent(u -> uuidInfo.set(unregisterUuid(u)));
        return uuidInfo.get();
    }

    /**
     * Adds all entries from the given set to the in-memory registry without checking
     * for pre-existing duplicates.
     *
     * @param set the set of {@link UuidInfo} instances to register
     */
    public static void registerAll(ConcurrentSkipListSet<UuidInfo> set) {
        uuids.addAll(set);
    }

    /**
     * Removes all entries in the given set from the in-memory registry by delegating
     * to {@link #unregisterUuid(UuidInfo)} for each element.
     *
     * @param set the set of {@link UuidInfo} instances to remove
     */
    public static void unregisterAll(ConcurrentSkipListSet<UuidInfo> set) {
        set.forEach(UuidManager::unregisterUuid);
    }

    /**
     * Persists all currently cached {@link UuidInfo} entries to the database and
     * then removes them from the in-memory registry. Intended to be called during
     * shutdown.
     */
    public static void clear() {
        getUuids().forEach(ui -> {
            ui.save();
            ui.unregister();
        });
    }

    /**
     * Finds the {@link UuidInfo} whose name history contains the given username
     * (case-insensitive).
     *
     * @param name the username to search for
     * @return an {@link Optional} containing the first matching {@link UuidInfo}, or
     *         empty if no cached entry has that name
     */
    public static Optional<UuidInfo> getFromName(String name) {
        AtomicReference<Optional<UuidInfo>> uuidInfo = new AtomicReference<>(Optional.empty());

        getUuids().forEach(ui -> {
            if (ui.getNamesCaseInsensitive().contains(name.toLowerCase())) {
                uuidInfo.set(Optional.of(ui));
            }
        });

        return uuidInfo.get();
    }

    /**
     * Returns the UUID string associated with the given username by searching the
     * in-memory registry.
     *
     * @param name the username to look up
     * @return an {@link Optional} containing the UUID string, or empty if no match
     *         is found
     */
    public static Optional<String> getUuidFromName(String name) {
        return getFromName(name).map(UuidInfo::getUuid);
    }

    /**
     * Converts a compact 32-character UUID string (no hyphens) to the canonical
     * 8-4-4-4-12 hyphenated format. If the input is not exactly 32 characters it is
     * returned unchanged.
     *
     * @param uuid the UUID string to format
     * @return the hyphenated UUID string, or the original value if it could not be
     *         reformatted
     */
    public static String makeDashedUUID(String uuid) {
        if (uuid.length() != 32) return uuid;
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20);
    }

    /**
     * Asynchronously loads or creates a {@link UuidInfo} record for the given player,
     * appends the current name and IP address, and saves the result to the database.
     *
     * <p>The lookup sequence is:</p>
     * <ol>
     *   <li>If the UUID is already cached in memory, update that entry.</li>
     *   <li>Otherwise, query the database. If a record exists, load it, update it,
     *       and re-register it.</li>
     *   <li>If no record exists at all, create a new {@link UuidInfo}, register it,
     *       and save it.</li>
     * </ol>
     *
     * @param uuid the player's UUID string
     * @param name the username observed at login
     * @param ip   the IP address observed at login
     */
    public static void cachePlayer(String uuid, String name, String ip) {
        CompletableFuture.runAsync(() -> {
            Optional<UuidInfo> infoOptional = getUuid(uuid);
            if (infoOptional.isEmpty()) {
                Optional<UuidInfo> optional = Singularity.getMainDatabase().loadUuidInfo(uuid).join();
                if (optional.isEmpty()) {
                    UuidInfo u = new UuidInfo(uuid, name, ip);
                    u.register();

                    u.save();
                } else {
                    UuidInfo u = optional.get();
                    u.register();

                    u.addName(name);
                    u.addIp(ip);

                    u.save();
                }
            } else {
                UuidInfo u = infoOptional.get();
                u.register();

                u.addName(name);
                u.addIp(ip);

                u.save();
            }
        });
    }
}
