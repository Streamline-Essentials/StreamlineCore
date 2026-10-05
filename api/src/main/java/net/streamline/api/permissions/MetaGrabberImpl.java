package net.streamline.api.permissions;

import net.luckperms.api.LuckPerms;
import net.streamline.api.SLAPI;
import singularity.data.players.CosmicPlayer;
import singularity.permissions.MetaGrabber;
import singularity.permissions.MetaKey;
import singularity.permissions.MetaValue;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@link MetaGrabber} that reads chat-meta (prefix and suffix) for {@link CosmicPlayer}
 * instances from the active {@link PermissionProvider}. Without a permission plugin both
 * resolve to an empty string. Writing meta is not supported through this implementation.
 */
public class MetaGrabberImpl implements MetaGrabber {

    /**
     * Hooks LuckPerms if necessary and returns the current optional wrapper.
     *
     * @return an {@link Optional} containing the {@link LuckPerms} API, or
     *         empty if LuckPerms is unavailable
     * @deprecated use {@link Permissions}, which works without LuckPerms
     */
    @Deprecated
    public static Optional<LuckPerms> tryGetLuckPerms() {
        Permissions.getProvider();
        return SLAPI.getLpOptional();
    }

    /**
     * Executes the given consumer against the LuckPerms API if it is available.
     *
     * @param consumer the action to perform with the {@link LuckPerms} instance
     * @deprecated use {@link Permissions}, which works without LuckPerms
     */
    @Deprecated
    public static void withLuckPerms(Consumer<LuckPerms> consumer) {
        SLAPI.withLuckPerms(consumer);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the player's highest-priority prefix and wraps it in a {@link MetaValue}.
     *
     * @param player the player whose prefix should be retrieved
     * @return an {@link Optional} containing the resolved prefix {@link MetaValue},
     *         holding an empty string when no prefix is set
     */
    @Override
    public Optional<MetaValue> getPrefix(CosmicPlayer player) {
        ChatMeta prefix = Permissions.getPrefix(player.getUuid());
        return Optional.of(new MetaValue(player.getIdentifier(), MetaKey.PREFIX, prefix.getValue(), -1, prefix.getPriority()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the player's highest-priority suffix and wraps it in a {@link MetaValue}.
     *
     * @param player the player whose suffix should be retrieved
     * @return an {@link Optional} containing the resolved suffix {@link MetaValue},
     *         holding an empty string when no suffix is set
     */
    @Override
    public Optional<MetaValue> getSuffix(CosmicPlayer player) {
        ChatMeta suffix = Permissions.getSuffix(player.getUuid());
        return Optional.of(new MetaValue(player.getIdentifier(), MetaKey.SUFFIX, suffix.getValue(), -1, suffix.getPriority()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Setting meta is not supported by this implementation; this method is
     * intentionally a no-op.
     *
     * @param value the meta value to set (ignored)
     */
    @Override
    public void setMeta(MetaValue value) {
        // not supported
    }
}
