package singularity.data.players.location;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Where a random teleport may land in one world: a horizontal band around a center, a
 * vertical range, and biomes to stay out of.
 *
 * <p>Horizontally the band is the area inside the {@code max} extents and outside the
 * {@code min} extents, both measured from the center along each axis. When {@link #isCircular()
 * circular} those extents are the radii of two ellipses; otherwise they are the half-widths of
 * two rectangles, so a spot qualifies when it is beyond {@code min} on either axis.</p>
 *
 * <p>Avoided biomes are matched against biome ids such as {@code minecraft:deep_ocean}. An
 * entry without a namespace also matches the id's path, and {@code *} matches any run of
 * characters, so {@code *ocean*} covers every ocean.</p>
 */
@Getter
public class RandomTeleportArea {

    /** Samples drawn per {@link #sample(Random)} call before it reports no spot. */
    private static final int SAMPLE_TRIES = 256;

    private final String world;
    private final int fromX;
    private final int fromZ;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int minY;
    private final int maxY;
    private final boolean circular;
    private final List<String> avoidedBiomes;

    private final List<Pattern> biomePatterns = new ArrayList<>();

    /**
     * Negative extents count as their absolute value, and a {@code max} below its {@code min}
     * is raised to it. The Y range is clamped to the world's height by the platform.
     */
    public RandomTeleportArea(String world, int fromX, int fromZ, int minX, int minZ, int maxX, int maxZ,
                              int minY, int maxY, boolean circular, List<String> avoidedBiomes) {
        this.world = world;
        this.fromX = fromX;
        this.fromZ = fromZ;
        this.minX = Math.abs(minX);
        this.minZ = Math.abs(minZ);
        this.maxX = Math.max(this.minX, Math.abs(maxX));
        this.maxZ = Math.max(this.minZ, Math.abs(maxZ));
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.circular = circular;
        this.avoidedBiomes = avoidedBiomes == null ? new ArrayList<>() : new ArrayList<>(avoidedBiomes);
        for (String entry : this.avoidedBiomes) {
            if (entry == null || entry.isBlank()) continue;
            String regex = Pattern.quote(entry.trim().toLowerCase(Locale.ROOT)).replace("*", "\\E.*\\Q");
            biomePatterns.add(Pattern.compile(regex));
        }
    }

    /**
     * A uniformly random block column in the band, as absolute {@code {x, z}}.
     *
     * @return the column, or {@code null} if no draw landed in the band (only likely when the
     *         band is very thin)
     */
    public int[] sample(Random random) {
        for (int i = 0; i < SAMPLE_TRIES; i++) {
            int dx = maxX == 0 ? 0 : random.nextInt(2 * maxX + 1) - maxX;
            int dz = maxZ == 0 ? 0 : random.nextInt(2 * maxZ + 1) - maxZ;
            if (inBand(dx, dz)) return new int[] {fromX + dx, fromZ + dz};
        }
        return null;
    }

    private boolean inBand(int dx, int dz) {
        if (! circular) return Math.abs(dx) >= minX || Math.abs(dz) >= minZ;
        if (ellipse(dx, dz, maxX, maxZ) > 1.0) return false;
        // An ellipse with a zero radius has no inside, so nothing is excluded by it.
        if (minX == 0 || minZ == 0) return true;
        return ellipse(dx, dz, minX, minZ) >= 1.0;
    }

    /** {@code (dx/rx)^2 + (dz/rz)^2}, where a zero radius admits only a zero offset. */
    private static double ellipse(int dx, int dz, int rx, int rz) {
        double x = rx == 0 ? (dx == 0 ? 0 : Double.POSITIVE_INFINITY) : dx / (double) rx;
        double z = rz == 0 ? (dz == 0 ? 0 : Double.POSITIVE_INFINITY) : dz / (double) rz;
        return x * x + z * z;
    }

    /**
     * @param biomeId a namespaced biome id such as {@code minecraft:plains}
     * @return whether a landing in that biome is not allowed
     */
    public boolean isAvoided(String biomeId) {
        if (biomeId == null || biomePatterns.isEmpty()) return false;
        String id = biomeId.toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        String path = colon < 0 ? id : id.substring(colon + 1);
        for (Pattern pattern : biomePatterns) {
            if (pattern.matcher(id).matches() || pattern.matcher(path).matches()) return true;
        }
        return false;
    }
}
