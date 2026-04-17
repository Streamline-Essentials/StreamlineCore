package singularity.permissions;

import singularity.data.players.CosmicPlayer;

import java.util.Optional;

/**
 * A strategy interface for retrieving and setting permission meta-data
 * (prefix, suffix, and other keyed values) for a {@link CosmicPlayer}.
 *
 * <p>Implementations are typically backed by a permission plugin such as
 * LuckPerms. A single active implementation is held by {@link PermissionUtil}
 * and used across the framework.
 */
public interface MetaGrabber {

    /**
     * Retrieves the prefix meta-value associated with the given player.
     *
     * @param player the player whose prefix should be fetched
     * @return an {@link Optional} containing the prefix {@link MetaValue},
     *         or {@link Optional#empty()} if none is set
     */
    Optional<MetaValue> getPrefix(CosmicPlayer player);

    /**
     * Retrieves the suffix meta-value associated with the given player.
     *
     * @param player the player whose suffix should be fetched
     * @return an {@link Optional} containing the suffix {@link MetaValue},
     *         or {@link Optional#empty()} if none is set
     */
    Optional<MetaValue> getSuffix(CosmicPlayer player);

    /**
     * Retrieves the meta-value for the given {@link MetaKey} and player.
     *
     * <p>Dispatches to {@link #getPrefix(CosmicPlayer)} or
     * {@link #getSuffix(CosmicPlayer)} based on the key. Unknown keys
     * return {@link Optional#empty()}.
     *
     * @param player the player whose meta should be fetched
     * @param key    the meta-data key to look up
     * @return an {@link Optional} containing the matching {@link MetaValue},
     *         or {@link Optional#empty()} if none is found
     */
    default Optional<MetaValue> getMeta(CosmicPlayer player, MetaKey key) {
        switch (key) {
            case PREFIX:
                return getPrefix(player);
            case SUFFIX:
                return getSuffix(player);
            default:
                return Optional.empty();
        }
    }

    /**
     * Applies a {@link MetaValue} (prefix or suffix) to the underlying
     * permission system.
     *
     * @param value the meta-value to set; the target player, key, and
     *              expiration are encoded within the value object
     */
    void setMeta(MetaValue value);
}
