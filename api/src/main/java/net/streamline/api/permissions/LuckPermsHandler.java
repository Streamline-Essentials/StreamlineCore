package net.streamline.api.permissions;

import net.luckperms.api.node.Node;
import net.streamline.api.SLAPI;
import singularity.utils.UuidUtils;

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
     * Returns whether a valid LuckPerms API instance is currently available.
     *
     * @return {@code true} if LuckPerms is loaded and reachable, {@code false} otherwise
     */
    public static boolean hasLuckPerms() {
        return SLAPI.getLpOptional().isPresent();
    }
}
