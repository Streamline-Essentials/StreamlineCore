package net.streamline.api.permissions;

import gg.drak.thebase.objects.AtomicString;
import gg.drak.thebase.utils.MathUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.streamline.api.SLAPI;
import net.streamline.api.utils.LuckPermsUtil;
import singularity.data.players.CosmicPlayer;
import singularity.data.uuid.UuidManager;
import singularity.permissions.MetaGrabber;
import singularity.permissions.MetaKey;
import singularity.permissions.MetaValue;
import singularity.utils.UUIDFetcher;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * LuckPerms-backed implementation of {@link MetaGrabber} that reads chat-meta
 * nodes (prefix and suffix) for {@link CosmicPlayer} instances. Writing meta
 * is not supported through this implementation.
 */
public class MetaGrabberImpl implements MetaGrabber {

    /**
     * Ensures the LuckPerms API is resolved and returns the current optional
     * wrapper. A failed resolution leaves the optional empty.
     *
     * @return an {@link Optional} containing the {@link LuckPerms} API, or
     *         empty if LuckPerms is unavailable
     */
    public static Optional<LuckPerms> tryGetLuckPerms() {
        SLAPI.tryGetLuckPerms();
        return SLAPI.getLpOptional();
    }

    /**
     * Executes the given consumer against the LuckPerms API if it is
     * available, resolving the API first if necessary.
     *
     * @param consumer the action to perform with the {@link LuckPerms} instance
     */
    public static void withLuckPerms(Consumer<LuckPerms> consumer) {
        SLAPI.withLuckPerms(consumer);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the highest-priority LuckPerms prefix for the player's current
     * username and wraps it in a {@link MetaValue}.
     *
     * @param player the player whose prefix should be retrieved
     * @return an {@link Optional} containing the resolved prefix {@link MetaValue},
     *         or an {@link Optional} with an empty-string value when LuckPerms
     *         is absent
     */
    @Override
    public Optional<MetaValue> getPrefix(CosmicPlayer player) {
        String username = player.getCurrentName();

        AtomicString prefix = new AtomicString("");
        AtomicInteger priority = new AtomicInteger(0);
        withLuckPerms(luckPerms -> {
            LuckPermsUtil.MetaRecord metaRecord = LuckPermsUtil.grabPrefix(luckPerms, username);
            prefix.set(metaRecord.getThing());
            priority.set(metaRecord.getPriority());
        });

        MetaValue metaValue = new MetaValue(player.getIdentifier(), MetaKey.PREFIX, prefix.get(), -1, priority.get());

        return Optional.of(metaValue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the highest-priority LuckPerms suffix for the player's current
     * username and wraps it in a {@link MetaValue}.
     *
     * @param player the player whose suffix should be retrieved
     * @return an {@link Optional} containing the resolved suffix {@link MetaValue},
     *         or an {@link Optional} with an empty-string value when LuckPerms
     *         is absent
     */
    @Override
    public Optional<MetaValue> getSuffix(CosmicPlayer player) {
        String username = player.getCurrentName();

        AtomicString suffix = new AtomicString("");
        AtomicInteger priority = new AtomicInteger(0);
        withLuckPerms(luckPerms -> {
            LuckPermsUtil.MetaRecord metaRecord = LuckPermsUtil.grabSuffix(luckPerms, username);
            suffix.set(metaRecord.getThing());
            priority.set(metaRecord.getPriority());
        });

        MetaValue metaValue = new MetaValue(player.getIdentifier(), MetaKey.SUFFIX, suffix.get(), -1, priority.get());

        return Optional.of(metaValue);
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
