package net.streamline.api.permissions;

import java.util.Optional;
import java.util.UUID;

/**
 * A permission backend Streamline reads from and writes to. {@link Permissions} holds the
 * active provider: {@link LuckPermsPermissionProvider} when LuckPerms is installed, and
 * {@link NoPermissionProvider} otherwise.
 *
 * <p>Signatures use only JDK types so that callers, and every class on the common path,
 * load without any permission plugin's API on the classpath.
 */
public interface PermissionProvider {

    /**
     * @return a short name for the backend, e.g. {@code "LuckPerms"}
     */
    String getName();

    /**
     * @return whether {@link #addPermission} and {@link #removePermission} take effect
     */
    boolean canModify();

    /**
     * The value the backend explicitly assigns the node for a loaded player. Empty means
     * the backend leaves the node unset, so the caller can apply its own default.
     *
     * @param uuid       the player's UUID
     * @param permission the permission node
     * @return the explicit value, or empty when unset, unknown, or the player is not loaded
     */
    Optional<Boolean> permissionValue(UUID uuid, String permission);

    /**
     * Grants a permission node.
     *
     * @return whether the backend accepted the change
     */
    boolean addPermission(UUID uuid, String permission);

    /**
     * Revokes a permission node.
     *
     * @return whether the backend accepted the change
     */
    boolean removePermission(UUID uuid, String permission);

    /**
     * @return the player's primary group, or empty when unknown
     */
    Optional<String> getPrimaryGroup(UUID uuid);

    /**
     * @return the first group the player inherits in the current context, or empty when unknown
     */
    Optional<String> getHighestGroup(UUID uuid);

    /**
     * A meta value set on the player, falling back to their primary group.
     *
     * @return the value, or empty when neither defines the key
     */
    Optional<String> getMeta(UUID uuid, String key);

    /**
     * @return the player's highest-priority prefix; an empty value when there is none
     */
    ChatMeta getPrefix(UUID uuid);

    /**
     * @return the player's highest-priority suffix; an empty value when there is none
     */
    ChatMeta getSuffix(UUID uuid);
}
