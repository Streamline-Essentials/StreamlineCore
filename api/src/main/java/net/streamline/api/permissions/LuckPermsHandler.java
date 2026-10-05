package net.streamline.api.permissions;

import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;
import net.streamline.api.SLAPI;
import singularity.utils.UuidUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for performing common LuckPerms permission operations through
 * the Streamline API. All methods are no-ops when LuckPerms is not present on
 * the server or when the supplied UUID is invalid.
 */
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
        String sUuid = UuidUtils.toUuid(uuid);
        if (sUuid == null) return;

        UUID playerUuid = UUID.fromString(sUuid);

        SLAPI.getLpOptional().ifPresent(lp -> {
            lp.getUserManager().modifyUser(playerUuid, user -> user.data().add(Node.builder(permission).build()));
        });
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
        String sUuid = UuidUtils.toUuid(uuid);
        if (sUuid == null) return;

        UUID playerUuid = UUID.fromString(sUuid);

        SLAPI.getLpOptional().ifPresent(lp -> {
            lp.getUserManager().modifyUser(playerUuid, user -> user.data().remove(Node.builder(permission).build()));
        });
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
        return permissionValue(uuid, permission).orElse(false);
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
        String sUuid = UuidUtils.toUuid(uuid);
        if (sUuid == null) return Optional.empty();

        UUID playerUuid = UUID.fromString(sUuid);

        return SLAPI.getLpOptional()
                .map(lp -> lp.getUserManager().getUser(playerUuid))
                .map(user -> user.getCachedData().getPermissionData().checkPermission(permission))
                .filter(tristate -> tristate != Tristate.UNDEFINED)
                .map(Tristate::asBoolean);
    }

    /**
     * Returns whether a valid LuckPerms API instance is currently available.
     *
     * @return {@code true} if LuckPerms is loaded and reachable, {@code false} otherwise
     */
    public static boolean hasLuckPerms() {
        return SLAPI.getLpOptional().isPresent();
    }
}
