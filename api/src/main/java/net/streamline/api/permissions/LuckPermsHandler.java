package net.streamline.api.permissions;

import net.luckperms.api.node.Node;
import net.streamline.api.SLAPI;
import singularity.utils.UuidUtils;

import java.util.UUID;

public class LuckPermsHandler {
    public static void addPermission(String uuid, String permission) {
        String sUuid = UuidUtils.toUuid(uuid);
        if (sUuid == null) return;

        UUID playerUuid = UUID.fromString(sUuid);

        SLAPI.getLpOptional().ifPresent(lp -> {
            lp.getUserManager().modifyUser(playerUuid, user -> user.data().add(Node.builder(permission).build()));
        });
    }

    public static void removePermission(String uuid, String permission) {
        String sUuid = UuidUtils.toUuid(uuid);
        if (sUuid == null) return;

        UUID playerUuid = UUID.fromString(sUuid);

        SLAPI.getLpOptional().ifPresent(lp -> {
            lp.getUserManager().modifyUser(playerUuid, user -> user.data().remove(Node.builder(permission).build()));
        });
    }

    public static boolean hasLuckPerms() {
        return SLAPI.getLpOptional().isPresent();
    }
}
