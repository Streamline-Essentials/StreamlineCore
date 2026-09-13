package singularity.permissions;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

import java.util.Optional;

/**
 * Static utility facade for permission meta-data operations.
 *
 * <p>All methods delegate to the registered {@link MetaGrabber} (if present).
 * When no grabber is registered the optional is empty and all retrieval
 * methods return {@link Optional#empty()} while mutation methods are no-ops.
 *
 * <p>A grabber can be installed at runtime (e.g. by the LuckPerms integration)
 * via {@link #setMetaGrabber(MetaGrabber)} and removed via
 * {@link #removeMetaGrabber()}.
 */
public class PermissionUtil {

    /**
     * The currently active {@link MetaGrabber}, wrapped in an {@link Optional}.
     * Defaults to {@link Optional#empty()} until a grabber is registered.
     */
    @Getter @Setter
    private static Optional<MetaGrabber> optionalMetaGrabber = Optional.empty();

    /**
     * Retrieves the meta-value for the given key from the active {@link MetaGrabber}.
     *
     * @param player the player whose meta should be retrieved
     * @param key    the meta-data key to look up
     * @return an {@link Optional} containing the {@link MetaValue}, or
     *         {@link Optional#empty()} if no grabber is registered or no value exists
     */
    public static Optional<MetaValue> getMeta(CosmicPlayer player, MetaKey key) {
        return optionalMetaGrabber.map(metaGrabber -> metaGrabber.getMeta(player, key)).filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Retrieves the prefix meta-value for the given player.
     *
     * @param player the player whose prefix should be retrieved
     * @return an {@link Optional} containing the prefix {@link MetaValue}, or
     *         {@link Optional#empty()} if none is set or no grabber is registered
     */
    public static Optional<MetaValue> getPrefix(CosmicPlayer player) {
        return getMeta(player, MetaKey.PREFIX);
    }

    /**
     * Retrieves the suffix meta-value for the given player.
     *
     * @param player the player whose suffix should be retrieved
     * @return an {@link Optional} containing the suffix {@link MetaValue}, or
     *         {@link Optional#empty()} if none is set or no grabber is registered
     */
    public static Optional<MetaValue> getSuffix(CosmicPlayer player) {
        return getMeta(player, MetaKey.SUFFIX);
    }

    /**
     * Applies the given {@link MetaValue} via the active {@link MetaGrabber}.
     * If no grabber is registered this method is a no-op.
     *
     * @param value the meta-value to set
     */
    public static void setMeta(MetaValue value) {
        optionalMetaGrabber.ifPresent(metaGrabber -> metaGrabber.setMeta(value));
    }

    /**
     * Convenience method that constructs a {@link MetaValue} for a prefix and
     * forwards it to {@link #setMeta(MetaValue)}.
     *
     * @param player   the target player
     * @param prefix   the prefix string to apply
     * @param duration the expiration timestamp in milliseconds, or {@code 0} for permanent
     * @param priority the priority of the entry; higher values win
     */
    public static void setPrefix(CosmicPlayer player, String prefix, long duration, int priority) {
        setMeta(new MetaValue(player.getIdentifier(), MetaKey.PREFIX, prefix, duration, priority));
    }

    /**
     * Convenience method that constructs a {@link MetaValue} for a suffix and
     * forwards it to {@link #setMeta(MetaValue)}.
     *
     * @param player   the target player
     * @param suffix   the suffix string to apply
     * @param duration the expiration timestamp in milliseconds, or {@code 0} for permanent
     * @param priority the priority of the entry; higher values win
     */
    public static void setSuffix(CosmicPlayer player, String suffix, long duration, int priority) {
        setMeta(new MetaValue(player.getIdentifier(), MetaKey.SUFFIX, suffix, duration, priority));
    }

    /**
     * Registers the given {@link MetaGrabber} as the active grabber, replacing
     * any previously registered instance.
     *
     * @param metaGrabber the grabber to register; must not be {@code null}
     */
    public static void setMetaGrabber(MetaGrabber metaGrabber) {
        optionalMetaGrabber = Optional.of(metaGrabber);
    }

    /**
     * Clears the active {@link MetaGrabber}, reverting to the no-op state where
     * all retrieval methods return empty and all mutation methods do nothing.
     */
    public static void removeMetaGrabber() {
        optionalMetaGrabber = Optional.empty();
    }
}
