package net.streamline.api.permissions;

import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.streamline.api.utils.LuckPermsUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * The LuckPerms backend. This is the only {@link PermissionProvider} that links against the
 * LuckPerms API, so it is created solely through {@link Permissions}, after LuckPerms has
 * been found on the classpath.
 *
 * <p>Lookups read LuckPerms' cache of loaded users, which holds online players.
 */
public final class LuckPermsPermissionProvider implements PermissionProvider {
    @Getter
    private final LuckPerms api;

    private LuckPermsPermissionProvider(LuckPerms api) {
        this.api = api;
    }

    /**
     * @return a provider bound to the running LuckPerms instance
     * @throws IllegalStateException when LuckPerms is installed but not yet enabled
     */
    static LuckPermsPermissionProvider create() {
        return new LuckPermsPermissionProvider(LuckPermsProvider.get());
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    public boolean canModify() {
        return true;
    }

    @Override
    public Optional<Boolean> permissionValue(UUID uuid, String permission) {
        return user(uuid)
                .map(user -> user.getCachedData().getPermissionData().checkPermission(permission))
                .filter(tristate -> tristate != Tristate.UNDEFINED)
                .map(Tristate::asBoolean);
    }

    @Override
    public boolean addPermission(UUID uuid, String permission) {
        api.getUserManager().modifyUser(uuid, user -> user.data().add(Node.builder(permission).build()));
        return true;
    }

    @Override
    public boolean removePermission(UUID uuid, String permission) {
        api.getUserManager().modifyUser(uuid, user -> user.data().remove(Node.builder(permission).build()));
        return true;
    }

    @Override
    public Optional<String> getPrimaryGroup(UUID uuid) {
        return user(uuid).map(User::getPrimaryGroup);
    }

    @Override
    public Optional<String> getHighestGroup(UUID uuid) {
        return user(uuid).flatMap(user -> user.getInheritedGroups(QueryOptions.builder(QueryMode.CONTEXTUAL).build())
                .stream()
                .findFirst()
                .map(Group::getName));
    }

    @Override
    public Optional<String> getMeta(UUID uuid, String key) {
        User user = user(uuid).orElse(null);
        if (user == null) return Optional.empty();

        Optional<String> own = metaOf(user.getNodes(NodeType.META), key);
        if (own.isPresent()) return own;

        Group group = api.getGroupManager().getGroup(user.getPrimaryGroup());
        if (group == null) return Optional.empty();
        return metaOf(group.getNodes(NodeType.META), key);
    }

    @Override
    public ChatMeta getPrefix(UUID uuid) {
        LuckPermsUtil.MetaRecord record = LuckPermsUtil.grabPrefix(api, uuid);
        return new ChatMeta(record.getThing(), record.getPriority());
    }

    @Override
    public ChatMeta getSuffix(UUID uuid) {
        LuckPermsUtil.MetaRecord record = LuckPermsUtil.grabSuffix(api, uuid);
        return new ChatMeta(record.getThing(), record.getPriority());
    }

    private Optional<User> user(UUID uuid) {
        return Optional.ofNullable(api.getUserManager().getUser(uuid));
    }

    /**
     * The last value set for {@code key} among the nodes, matching the order LuckPerms
     * iterates them in; empty values count as unset.
     */
    private static Optional<String> metaOf(Iterable<? extends MetaNode> nodes, String key) {
        String value = null;
        for (MetaNode node : nodes) {
            if (node.getMetaKey().equals(key)) value = node.getMetaValue();
        }
        return Optional.ofNullable(value).filter(v -> ! v.isEmpty());
    }
}
