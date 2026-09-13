package singularity.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.holders.builtin.CosmicGeyserHolder;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Central registry for all active {@link HolderInit} instances in the Streamline framework.
 *
 * <p>Provides static methods to register, deregister, and look up holders by their
 * string identifier. Convenience accessors are also provided for well-known holders such as
 * {@link CosmicGeyserHolder}.</p>
 */
public class HoldersHolder {

    /**
     * The canonical identifier string used to register and retrieve the Geyser holder.
     */
    public static String GEYSER_IDENTIFIER = "geyser";

    /**
     * The live set of all currently registered holder initialisers.
     * Thread-safe via {@link ConcurrentSkipListSet}.
     */
    @Getter @Setter
    private static ConcurrentSkipListSet<HolderInit<?>> holders = new ConcurrentSkipListSet<>();

    /**
     * Registers a {@link HolderInit} in the global registry, replacing any existing entry
     * with the same identifier.
     *
     * @param holderInit the holder initialiser to register; must not be {@code null}
     */
    public static void load(HolderInit<?> holderInit) {
        unload(holderInit);

        holders.add(holderInit);
    }

    /**
     * Removes any registered {@link HolderInit} whose identifier matches that of
     * {@code holderInit}.
     *
     * @param holderInit the holder initialiser whose identifier should be removed from the registry
     */
    public static void unload(HolderInit<?> holderInit) {
        getHolders().removeIf(h -> h.getIdentifier().equals(holderInit.getIdentifier()));
    }

    /**
     * Retrieves the {@link HolderInit} registered under the given identifier.
     *
     * @param identifier the unique string identifier to search for
     * @return the matching {@link HolderInit}, or {@code null} if none is registered
     */
    public static HolderInit<?> get(String identifier) {
        return getHolders().stream().filter(h -> h.getIdentifier().equals(identifier)).findFirst().orElse(null);
    }

    /**
     * Returns {@code true} if a {@link HolderInit} is registered under the given identifier.
     *
     * @param identifier the unique string identifier to check
     * @return {@code true} if a holder with that identifier exists, {@code false} otherwise
     */
    public static boolean has(String identifier) {
        return getHolders().stream().anyMatch(h -> h.getIdentifier().equals(identifier));
    }

    /**
     * Convenience accessor that retrieves the registered {@link CosmicGeyserHolder}, if present.
     *
     * <p>Returns {@code null} if no holder is registered under {@link #GEYSER_IDENTIFIER}, or if
     * the registered holder's inner holder is not a {@link CosmicGeyserHolder} instance.</p>
     *
     * @return the active {@link CosmicGeyserHolder}, or {@code null}
     */
    public static CosmicGeyserHolder getGeyserHolder() {
        HolderInit<?> holderInit = get(GEYSER_IDENTIFIER);
        if (holderInit == null) return null;
        if (! (holderInit.getHolder() instanceof CosmicGeyserHolder)) return null;
        return (CosmicGeyserHolder) holderInit.getHolder();
    }
}
