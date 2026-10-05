package net.streamline.platform.compat;

import net.minecraft.server.level.ServerPlayer;

/**
 * Minecraft API calls whose shape differs between game versions, for 1.21 onward.
 * The same class for 1.20.1 lives in {@code mod-common/compat/mc1201}.
 */
public final class McCompat {

    private McCompat() {}

    /**
     * The player's latency in milliseconds, as tracked by their connection.
     */
    public static int getPing(ServerPlayer player) {
        return player.connection.latency();
    }
}
