package net.streamline.api.permissions;

import host.plas.bou.utils.UuidUtils;
import net.luckperms.api.node.Node;
import net.streamline.api.SLAPI;

import java.util.UUID;

public class LuckPermsHandler {
    public static void addPermission(String uuid, String permission) {
        UUID playerUuid = UUID.fromString(UuidUtils.toUuid(uuid));

        SLAPI.getLpOptional().ifPresent(lp -> {
            lp.getUserManager().modifyUser(playerUuid, user -> user.data().add(Node.builder(permission).build()));
        });
    }

    public static void removePermission(String uuid, String permission) {
        UUID playerUuid = UUID.fromString(UuidUtils.toUuid(uuid));

        SLAPI.getLpOptional().ifPresent(lp -> {
            lp.getUserManager().modifyUser(playerUuid, user -> user.data().remove(Node.builder(permission).build()));
        });
    }

    public static boolean hasLuckPerms() {
        return SLAPI.getLpOptional().isPresent();
    }
}
