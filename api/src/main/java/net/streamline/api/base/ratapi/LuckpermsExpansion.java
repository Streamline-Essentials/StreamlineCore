package net.streamline.api.base.ratapi;

import gg.drak.thebase.objects.AtomicString;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.streamline.api.base.module.BaseModule;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * RAT placeholder expansion that exposes LuckPerms data for every
 * {@link singularity.data.console.CosmicSender}.
 *
 * <p>The expansion is registered under the {@code luckperms} namespace and
 * provides the following placeholders (all prefixed with
 * {@code %streamline_luckperms_}):
 * <ul>
 *   <li>{@code prefix} — the player's effective LuckPerms prefix.</li>
 *   <li>{@code suffix} — the player's effective LuckPerms suffix.</li>
 *   <li>{@code primary_group} — the player's primary group name.</li>
 *   <li>{@code highest_group} — the first inherited group in the contextual
 *       inheritance chain.</li>
 *   <li>{@code meta_<key>} — a specific meta value from the player's nodes or
 *       their primary group's nodes.</li>
 * </ul>
 * Console senders return an empty string for all placeholders because they
 * have no associated UUID.
 */
public class LuckpermsExpansion extends RATExpansion {

    /**
     * Constructs the expansion and logs its registration to the base module's
     * logger.
     */
    public LuckpermsExpansion() {
        super(new RATExpansionBuilder("luckperms"));
        BaseModule.getInstance().logInfo(getClass().getSimpleName() + " is registered!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers all LuckPerms-backed {@link singularity.placeholders.replaceables.IdentifiedUserReplaceable}
     * instances: {@code prefix}, {@code suffix}, {@code primary_group},
     * {@code highest_group}, and the regex-based {@code meta_<key>} pattern.</p>
     */
    @Override
    public void init() {
        new IdentifiedUserReplaceable(this, "prefix", (s, user) -> UserUtils.getPrefix(user)).register();
        new IdentifiedUserReplaceable(this, "suffix", (s, user) -> UserUtils.getSuffix(user)).register();

        new IdentifiedUserReplaceable(this, "primary_group", (s, user) -> {
            try {
                UUID uuid = null;

                try {
                    uuid = UUID.fromString(user.getUuid());

                    if (uuid == null) return s.string();
                } catch (Exception e) {
                    // is console.
                    return s.string();
                }

                try {
                    LuckPerms api = LuckPermsProvider.get();

                    User u = api.getUserManager().getUser(uuid);
                    if (u == null) return s.string();

                    return u.getPrimaryGroup();
                } catch (Exception e) {
                    return s.string();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();

        new IdentifiedUserReplaceable(this, "highest_group", (s, user) -> {
            try {
                UUID uuid = null;

                try {
                    uuid = UUID.fromString(user.getUuid());

                    if (uuid == null) return s.string();
                } catch (Exception e) {
                    // is console.
                    return s.string();
                }

                try {
                    LuckPerms api = LuckPermsProvider.get();
                    User u = api.getUserManager().getUser(uuid);
                    if (u == null) return s.string();

                    Optional<Group> group = u.getInheritedGroups(QueryOptions.builder(QueryMode.CONTEXTUAL).build()).stream().findFirst();

                    return group.map(Group::getName).orElse(s.string());
                } catch (Exception e) {
                    return s.string();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();

        new IdentifiedUserReplaceable(this, "[m][e][t][a][_]" + "(.*?)", 1, (s, user) -> {
            try {
                String params = s.get();

                UUID uuid;
                try {
                    uuid = UUID.fromString(user.getUuid());
                } catch (Exception e) {
                    // is console.
                    return s.string();
                }

                try {
                    LuckPerms api = LuckPermsProvider.get();
                    User u = api.getUserManager().getUser(uuid);
                    if (u == null) return s.string();

                    AtomicString s1 = new AtomicString("");
                    u.getNodes(NodeType.META).forEach(node -> {
                        if (node.getMetaKey().equals(params)) {
                            s1.set(node.getMetaValue());
                        }
                    });

                    if (!s1.get().isEmpty() || !s1.get().isBlank()) {
                        return s1.get();
                    }

                    Group group = api.getGroupManager().getGroup(u.getPrimaryGroup());
                    if (group == null) return s.string();

                    group.getNodes(NodeType.META).forEach(node -> {
                        if (node.getMetaKey().equals(params)) {
                            s1.set(node.getMetaValue());
                        }
                    });

                    if (!s1.get().isEmpty() || !s1.get().isBlank()) {
                        return s1.get();
                    }

                    return s.string();
                } catch (Exception e) {
                    return s.string();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();
    }
}