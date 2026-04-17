package net.streamline.api.utils;

import gg.drak.thebase.utils.MathUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import singularity.data.uuid.UuidManager;
import singularity.utils.UUIDFetcher;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

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
     * Generic helper that resolves the highest-priority chat-meta node of
     * the requested type for a player identified by username.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Determine the player's UUID from the in-memory cache, falling back
     *       to a remote UUID lookup.</li>
     *   <li>Fetch the LuckPerms {@code User} and their primary {@code Group}.</li>
     *   <li>Collect all matching nodes from the group (if present) then from
     *       the user, storing them in priority order.</li>
     *   <li>Return the value at the ceiling (highest) priority, or {@code ""}
     *       when no nodes are found.</li>
     * </ol>
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
        String toReturn = "";
        int priority = 0;

        String uuid = "";
        Optional<String> s = UuidManager.getUuidFromName(username);
        if (s.isEmpty()) {
            UUID u = UUIDFetcher.getUUID(username);
            if (u != null) uuid = u.toString();
        } else {
            uuid = s.get();
        }

        if (Objects.equals(uuid, "")) return new MetaRecord("", 0);

        User user = luckPerms.getUserManager().getUser(UUID.fromString(uuid));
        if (user == null) return new MetaRecord("", 0);

        Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());

        ConcurrentSkipListMap<Integer, String> preWeight = new ConcurrentSkipListMap<>();
        if (group == null) {
            for (N node : user.getNodes(type)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }
        } else {
            for (N node : group.getNodes(type)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            for (N node : user.getNodes(type)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }
        }

        toReturn = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));
        priority = MathUtils.getCeilingInt(preWeight.keySet());

        if (toReturn == null) toReturn = "";

        return new MetaRecord(toReturn, priority);
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
}
