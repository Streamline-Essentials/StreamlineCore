package net.streamline.platform.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Minecraft API calls whose shape differs between game versions, for 1.21.1.
 * The same class for other versions lives in {@code mod-common/compat/mc1201} and
 * {@code mod-common/compat/current}.
 */
public final class McCompat {

    private McCompat() {}

    /**
     * The player's latency in milliseconds, as tracked by their connection.
     */
    public static int getPing(ServerPlayer player) {
        return player.connection.latency();
    }

    /** Lowest block Y in the level. */
    public static int minY(ServerLevel level) {
        return level.getMinBuildHeight();
    }

    /** Highest block Y in the level, inclusive; 1.21.1 reports the exclusive bound. */
    public static int maxY(ServerLevel level) {
        return level.getMaxBuildHeight() - 1;
    }

    /** The level's dimension id, such as {@code minecraft:overworld}. */
    public static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    /** Where new players spawn, with the facing they spawn with. */
    public static SpawnPoint worldSpawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return new SpawnPoint(level, level.getSharedSpawnPos(), level.getSharedSpawnAngle(), 0F);
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    /** Operator level 2 (gamemaster) or higher. */
    public static boolean isOperator(ServerPlayer player) {
        return player.hasPermissions(2);
    }


    /** A spawn position and the facing that goes with it. */
    public static final class SpawnPoint {
        public final ServerLevel level;
        public final BlockPos pos;
        public final float yaw;
        public final float pitch;

        public SpawnPoint(ServerLevel level, BlockPos pos, float yaw, float pitch) {
            this.level = level;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
