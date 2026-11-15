package net.streamline.api.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import singularity.data.uuid.UuidManager;
import singularity.utils.UUIDFetcher;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.SortedMap;

/**
 * Utility class for reading LuckPerms chat-meta nodes (prefix and suffix)
 * associated with a player's primary group and personal overrides. Results are
 * resolved by highest priority, with user-level nodes taking precedence over
 * group-level nodes of equal priority.
 */
public class LuckPermsUtil {

    /**
     * An immutable pair holding a resolved chat-meta value (e.g. a prefix or
     * suffix string) together with the LuckPerms priority at which it was found.
     */
    @Getter @Setter @AllArgsConstructor
    public static class MetaRecord {
        /** The resolved meta value string (prefix or suffix text). */
        private String thing;
        /** The LuckPerms node priority at which the value was resolved. */
        private int priority;
    }

    /**
     * Resolves the prefix or suffix of the requested type for a player identified by
     * username: the UUID comes from the in-memory cache, falling back to a remote UUID
     * lookup, and is then resolved by {@link #grabThing(LuckPerms, UUID, NodeType)}.
     *
     * @param <N>        the concrete {@link ChatMetaNode} type
     * @param <B>        the builder type for {@code N}
     * @param <T>        the {@link NodeType} used to filter nodes
     * @param luckPerms  the LuckPerms API instance to query
     * @param username   the player's current display name
     * @param type       the node type to query (e.g. {@code NodeType.PREFIX})
     * @return a {@link MetaRecord} containing the resolved value and its priority;
     *         returns a record with an empty string and priority 0 when the player
     *         cannot be found or holds no matching nodes
     */
    public static <N extends ChatMetaNode<N, B>, B extends ChatMetaNode.Builder<N, B>, T extends NodeType<N>> MetaRecord
    grabThing(LuckPerms luckPerms, String username, T type) {
        String uuid = "";
        Optional<String> s = UuidManager.getUuidFromName(username);
        if (s.isEmpty()) {
            UUID u = UUIDFetcher.getUUID(username);
            if (u != null) uuid = u.toString();
        } else {
            uuid = s.get();
        }

        if (Objects.equals(uuid, "")) return new MetaRecord("", 0);

        return grabThing(luckPerms, UUID.fromString(uuid), type);
    }

    /**
     * Resolves the prefix or suffix of the requested type for the loaded player with the
     * given UUID, as {@link #grabThing(LuckPerms, String, NodeType)} does once it has
     * resolved a username.
     *
     * <p>The value is LuckPerms' own resolution ({@link CachedMetaData#getPrefix()} /
     * {@link CachedMetaData#getSuffix()}): inherited groups, tracks, contexts, weights and
     * meta stacking all apply, exactly as chat plugins reading LuckPerms see it.</p>
     *
     * @param <N>        the concrete {@link ChatMetaNode} type
     * @param <B>        the builder type for {@code N}
     * @param <T>        the {@link NodeType} used to filter nodes
     * @param luckPerms  the LuckPerms API instance to query
     * @param uuid       the player's UUID
     * @param type       {@code NodeType.PREFIX} or {@code NodeType.SUFFIX}
     * @return a {@link MetaRecord} containing the resolved value and the priority of the
     *         highest entry; an empty string and priority 0 when the player is not loaded
     *         or has none
     */
    public static <N extends ChatMetaNode<N, B>, B extends ChatMetaNode.Builder<N, B>, T extends NodeType<N>> MetaRecord
    grabThing(LuckPerms luckPerms, UUID uuid, T type) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return new MetaRecord("", 0);

        CachedMetaData meta = user.getCachedData().getMetaData();
        boolean prefix = type == NodeType.PREFIX;
        String value = prefix ? meta.getPrefix() : meta.getSuffix();
        SortedMap<Integer, String> all = prefix ? meta.getPrefixes() : meta.getSuffixes();
        int priority = all.isEmpty() ? 0 : all.firstKey();

        return new MetaRecord(value == null ? "" : value, priority);
    }

    /**
     * Whether LuckPerms has the player with this UUID loaded, i.e. whether
     * {@link #grabThing(LuckPerms, UUID, NodeType)} reflects their real prefix and suffix
     * rather than the empty answer it gives for players it does not hold.
     */
    public static boolean isLoaded(LuckPerms luckPerms, UUID uuid) {
        return luckPerms.getUserManager().getUser(uuid) != null;
    }

    /**
     * Resolves the highest-priority prefix for the given player username.
     *
     * @param luckPerms the LuckPerms API instance to query
     * @param username  the player's current display name
     * @return a {@link MetaRecord} containing the prefix string and its priority
     */
    public static MetaRecord grabPrefix(LuckPerms luckPerms, String username) {
        return grabThing(luckPerms, username, NodeType.PREFIX);
    }

    /**
     * Resolves the highest-priority suffix for the given player username.
     *
     * @param luckPerms the LuckPerms API instance to query
     * @param username  the player's current display name
     * @return a {@link MetaRecord} containing the suffix string and its priority
     */
    public static MetaRecord grabSuffix(LuckPerms luckPerms, String username) {
        return grabThing(luckPerms, username, NodeType.SUFFIX);
    }

    /**
     * Resolves the highest-priority prefix for the loaded player with the given UUID.
     *
     * @param luckPerms the LuckPerms API instance to query
     * @param uuid      the player's UUID
     * @return a {@link MetaRecord} containing the prefix string and its priority
     */
    public static MetaRecord grabPrefix(LuckPerms luckPerms, UUID uuid) {
        return grabThing(luckPerms, uuid, NodeType.PREFIX);
    }

    /**
     * Resolves the highest-priority suffix for the loaded player with the given UUID.
     *
     * @param luckPerms the LuckPerms API instance to query
     * @param uuid      the player's UUID
     * @return a {@link MetaRecord} containing the suffix string and its priority
     */
    public static MetaRecord grabSuffix(LuckPerms luckPerms, UUID uuid) {
        return grabThing(luckPerms, uuid, NodeType.SUFFIX);
    }
}