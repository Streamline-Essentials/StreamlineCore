package net.streamline.api.permissions;

import java.util.Optional;

/**
 * Convenience permission operations for modules written against LuckPerms. Every method
 * delegates to {@link Permissions}, so this class does not link against the LuckPerms API
 * and is safe to call whether or not LuckPerms is installed; without it, every method is a
 * no-op or reports nothing set.
 *
 * @deprecated use {@link Permissions}, which is not tied to one permission plugin
 */
@Deprecated
public class LuckPermsHandler {

    /**
     * Grants the specified permission node to the player identified by the
     * given UUID string. If LuckPerms is unavailable or the UUID cannot be
     * resolved the call is silently ignored.
     *
     * @param uuid       the player's UUID (any format accepted by {@code UuidUtils.toUuid})
     * @param permission the permission node string to add
     */
    public static void addPermission(String uuid, String permission) {
        Permissions.addPermission(uuid, permission);
    }

    /**
     * Revokes the specified permission node from the player identified by the
     * given UUID string. If LuckPerms is unavailable or the UUID cannot be
     * resolved the call is silently ignored.
     *
     * @param uuid       the player's UUID (any format accepted by {@code UuidUtils.toUuid})
     * @param permission the permission node string to remove
     */
    public static void removePermission(String uuid, String permission) {
        Permissions.removePermission(uuid, permission);
    }

    /**
     * Checks the specified permission node against the player's cached LuckPerms data.
     * Only players LuckPerms has loaded (i.e. online players) can be checked; for anyone
     * else, or when LuckPerms is unavailable or the UUID cannot be resolved, this returns
     * {@code false}.
     *
     * @param uuid       the player's UUID (any format accepted by {@code UuidUtils.toUuid})
     * @param permission the permission node string to check
     * @return {@code true} if LuckPerms grants the permission to the loaded player
     */
    public static boolean hasPermission(String uuid, String permission) {
        return Permissions.hasPermission(uuid, permission);
    }

    /**
     * The value LuckPerms explicitly assigns the node for the loaded player. Unlike
     * {@link #hasPermission(String, String)}, this distinguishes a node LuckPerms leaves
     * unset (empty) from one it denies, so a caller can apply its own default.
     *
     * @param uuid       the player's UUID (any format accepted by {@code UuidUtils.toUuid})
     * @param permission the permission node string to check
     * @return the explicit value, or empty when LuckPerms is unavailable, the player is not
     *         loaded, or the node is undefined
     */
    public static Optional<Boolean> permissionValue(String uuid, String permission) {
        return Permissions.permissionValue(uuid, permission);
    }

    /**
     * Returns whether a valid LuckPerms API instance is currently available.
     *
     * @return {@code true} if LuckPerms is loaded and reachable, {@code false} otherwise
     */
    public static boolean hasLuckPerms() {
        return Permissions.isHooked();
    }
}
