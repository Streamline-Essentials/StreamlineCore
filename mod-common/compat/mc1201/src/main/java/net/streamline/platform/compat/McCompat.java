package net.streamline.platform.compat;

import net.minecraft.server.level.ServerPlayer;

/**
 * Minecraft API calls whose shape differs between game versions, for 1.20.1.
 * The same class for 1.21 onward lives in {@code mod-common/compat/current}.
 */
public final class McCompat {

    private McCompat() {}

    /**
     * The player's latency in milliseconds; on 1.20.1 this is a field on the player.
     */
    public static int getPing(ServerPlayer player) {
        return player.latency;
    }
}
