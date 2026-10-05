package net.streamline.api.permissions;

import java.util.Optional;
import java.util.UUID;

/**
 * The provider used when no permission plugin is installed. It defines no nodes, groups
 * or meta, and refuses changes, so permission checks fall through to each platform's own
 * rules (Bukkit/proxy permissions, or defaults and operator level on mod loaders).
 */
public final class NoPermissionProvider implements PermissionProvider {
    public static final NoPermissionProvider INSTANCE = new NoPermissionProvider();

    private NoPermissionProvider() {}

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public boolean canModify() {
        return false;
    }

    @Override
    public Optional<Boolean> permissionValue(UUID uuid, String permission) {
        return Optional.empty();
    }

    @Override
    public boolean addPermission(UUID uuid, String permission) {
        return false;
    }

    @Override
    public boolean removePermission(UUID uuid, String permission) {
        return false;
    }

    @Override
    public Optional<String> getPrimaryGroup(UUID uuid) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getHighestGroup(UUID uuid) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMeta(UUID uuid, String key) {
        return Optional.empty();
    }

    @Override
    public ChatMeta getPrefix(UUID uuid) {
        return ChatMeta.EMPTY;
    }

    @Override
    public ChatMeta getSuffix(UUID uuid) {
        return ChatMeta.EMPTY;
    }
}
