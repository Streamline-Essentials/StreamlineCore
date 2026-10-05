package net.streamline.platform.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.streamline.platform.compat.McCompat;
import singularity.data.players.location.RandomTeleportArea;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** Finds positions where a player can stand without suffocating, falling, burning or drowning. */
public final class SafeSpots {

    private SafeSpots() {}

    /** True when {@code feet} and the block above are open and the block below is solid, safe ground. */
    public static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= McCompat.minY(level) || feet.getY() + 1 > topY(level)) return false;
        BlockPos groundPos = feet.below();
        BlockState ground = level.getBlockState(groundPos);
        if (! ground.getFluidState().isEmpty() || isHazard(ground)) return false;
        if (! ground.isFaceSturdy(level, groundPos, Direction.UP)) return false;
        return isOpen(level, feet) && isOpen(level, feet.above());
    }

    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty() && ! isHazard(state);
    }

    private static boolean isHazard(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE)
                || state.is(Blocks.LAVA) || state.is(Blocks.LAVA_CAULDRON);
    }

    /**
     * Highest Y a player's head may occupy. In dimensions with a ceiling (the Nether) this stays
     * below the logical height so players never land on the bedrock roof.
     */
    public static int topY(ServerLevel level) {
        if (level.dimensionType().hasCeiling()) {
            return McCompat.minY(level) + level.dimensionType().logicalHeight() - 1;
        }
        return McCompat.maxY(level);
    }

    /** The safe feet Y in column (x, z) closest to {@code preferredY}, searching up and down alternately. */
    public static OptionalInt nearestSafeY(ServerLevel level, int x, int z, int preferredY) {
        return nearestSafeYWithin(level, x, z, preferredY, McCompat.minY(level) + 1, topY(level) - 1);
    }

    /** The first safe feet Y at or above {@code fromY} in column (x, z). */
    public static OptionalInt firstSafeAbove(ServerLevel level, int x, int z, int fromY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
        for (int y = Math.max(fromY, McCompat.minY(level) + 1); y < topY(level); y++) {
            if (isSafe(level, pos.setY(y))) return OptionalInt.of(y);
        }
        return OptionalInt.empty();
    }

    /** The first safe feet Y from {@code fromY} toward {@code toY}, both inclusive, within the level's height. */
    public static OptionalInt firstSafeBetween(ServerLevel level, int x, int z, int fromY, int toY) {
        int low = Math.max(McCompat.minY(level) + 1, Math.min(fromY, toY));
        int high = Math.min(topY(level) - 1, Math.max(fromY, toY));
        if (low > high) return OptionalInt.empty();
        boolean up = toY >= fromY;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, low, z);
        for (int y = up ? low : high; up ? y <= high : y >= low; y += up ? 1 : -1) {
            if (isSafe(level, pos.setY(y))) return OptionalInt.of(y);
        }
        return OptionalInt.empty();
    }

    /** The safe feet Y in [min, max] closest to {@code preferredY}, searching up and down alternately. */
    private static OptionalInt nearestSafeYWithin(ServerLevel level, int x, int z, int preferredY, int min, int max) {
        int start = Math.max(min, Math.min(max, preferredY));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, start, z);
        for (int offset = 0; start - offset >= min || start + offset <= max; offset++) {
            int up = start + offset;
            if (up <= max && isSafe(level, pos.setY(up))) return OptionalInt.of(up);
            int down = start - offset;
            if (offset > 0 && down >= min && isSafe(level, pos.setY(down))) return OptionalInt.of(down);
        }
        return OptionalInt.empty();
    }

    /**
     * A random safe landing spot inside the area; see
     * {@link singularity.interfaces.IGameplayHandler#findRandomSafeLocation(RandomTeleportArea, int)}.
     */
    public static Optional<BlockPos> random(ServerLevel level, RandomTeleportArea area, int attempts) {
        int minY = Math.max(area.getMinY(), McCompat.minY(level) + 1);
        int maxY = Math.min(area.getMaxY(), topY(level) - 1);
        if (minY > maxY) return Optional.empty();
        boolean ceiling = level.dimensionType().hasCeiling();
        Random random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < attempts; attempt++) {
            int[] column = area.sample(random);
            if (column == null) continue;
            int x = column[0];
            int z = column[1];
            if (! level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;

            // The biome is sampled from the generator without loading the chunk, so most
            // avoided columns are rejected before paying for chunk generation.
            int sampleY = ceiling ? (minY + maxY) / 2 : Math.max(minY, Math.min(maxY, level.getSeaLevel()));
            Holder<Biome> noise = level.getUncachedNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(sampleY), QuartPos.fromBlock(z));
            if (area.isAvoided(McCompat.biomeId(noise))) continue;

            OptionalInt y;
            if (ceiling) {
                y = nearestSafeYWithin(level, x, z, (minY + maxY) / 2, minY, maxY);
            } else {
                int surface = level.getChunk(x >> 4, z >> 4)
                        .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
                // An empty column (the End's void) reports the bottom of the world.
                if (surface <= McCompat.minY(level) || surface < minY) continue;
                // A surface inside the range is the only candidate, so water and lava
                // surfaces are skipped rather than searched through into the caves below.
                if (surface <= maxY) y = isSafe(level, new BlockPos(x, surface, z)) ? OptionalInt.of(surface) : OptionalInt.empty();
                else y = firstSafeBetween(level, x, z, maxY, minY);
            }
            if (y.isEmpty()) continue;
            BlockPos spot = new BlockPos(x, y.getAsInt(), z);
            // The landing's own biome can differ from the sampled one, as in caves.
            if (area.isAvoided(McCompat.biomeId(level.getBiome(spot)))) continue;
            return Optional.of(spot);
        }
        return Optional.empty();
    }

    /**
     * A random safe landing spot in the ring between the radii around the world spawn (around
     * 0,0 outside the overworld), skipping oceans and rivers and respecting the world border.
     */
    public static Optional<BlockPos> random(ServerLevel level, RandomSource random, int minRadius, int maxRadius, int attempts) {
        int min = Math.max(0, minRadius);
        int max = Math.max(min + 1, maxRadius);
        BlockPos center = level.dimension() == Level.OVERWORLD
                ? McCompat.worldSpawn(level.getServer()).pos
                : BlockPos.ZERO;
        boolean ceiling = level.dimensionType().hasCeiling();

        for (int attempt = 0; attempt < attempts; attempt++) {
            // Uniform over the ring's area rather than its radius, so landings do not cluster near the center.
            double angle = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(min * (double) min + random.nextDouble() * (max * (double) max - min * (double) min));
            int x = center.getX() + (int) Math.round(Math.cos(angle) * r);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * r);
            if (! level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;

            // The biome is sampled from the generator without loading the chunk, so ocean and
            // river columns are rejected before paying for chunk generation.
            int sampleY = ceiling ? 64 : level.getSeaLevel();
            Holder<Biome> biome = level.getUncachedNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(sampleY), QuartPos.fromBlock(z));
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) continue;

            OptionalInt y;
            if (ceiling) {
                y = nearestSafeY(level, x, z, 64);
            } else {
                // Level#getHeight reports the bottom of the world for chunks that are not
                // loaded, so the chunk is loaded (generating it if needed) and asked directly.
                int surface = level.getChunk(x >> 4, z >> 4)
                        .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
                // An empty column (the End's void) reports the bottom of the world.
                if (surface <= McCompat.minY(level)) continue;
                y = isSafe(level, new BlockPos(x, surface, z)) ? OptionalInt.of(surface) : OptionalInt.empty();
            }
            if (y.isPresent()) return Optional.of(new BlockPos(x, y.getAsInt(), z));
        }
        return Optional.empty();
    }
}
