package host.plas.collections.progression;

/**
 * Derives a star level from total weighted progression XP. Stars are never stored, only computed
 * from total experience. The cost of star {@code n → n + 1} is
 * {@code min(max-needed, n * formula-multiplier + formula-base)}.
 */
public final class StarsFormula {
    private StarsFormula() {
    }

    /** XP cost to go from {@code stars} to {@code stars + 1}. */
    public static double costForNextStar(long stars) {
        ProgressionConfig config = ProgressionManager.getConfig();
        return costForNextStar(stars, config.getFormulaMultiplier(), config.getFormulaBase(), config.getMaxNeeded());
    }

    static double costForNextStar(long stars, double multiplier, double base, double max) {
        long safe = Math.max(0L, stars);
        return Math.min(max, safe * multiplier + base);
    }

    /** Total weighted XP required to reach exactly {@code stars} (the sum of every earlier star's cost). */
    public static double xpRequiredForStars(long stars) {
        ProgressionConfig config = ProgressionManager.getConfig();
        return xpRequiredForStars(stars, config.getFormulaMultiplier(), config.getFormulaBase(), config.getMaxNeeded());
    }

    static double xpRequiredForStars(long stars, double multiplier, double base, double max) {
        long safe = Math.max(0L, stars);
        if (safe == 0L) return 0.0D;
        double total = 0.0D;
        // cost(i) = min(max, i*mult + base). Once capped, remaining levels cost `max` each.
        long capStart = 0L;
        if (multiplier > 0.0D) capStart = (long) Math.ceil(Math.max(0.0D, (max - base) / multiplier));
        long linearEnd = Math.min(safe, capStart);
        for (long i = 0L; i < linearEnd; i++) total += i * multiplier + base;
        if (safe > capStart) total += (safe - capStart) * max;
        return total;
    }

    public static long starsFromTotalXp(double totalXp) {
        ProgressionConfig config = ProgressionManager.getConfig();
        return starsFromTotalXp(totalXp, config.getFormulaMultiplier(), config.getFormulaBase(), config.getMaxNeeded());
    }

    static long starsFromTotalXp(double totalXp, double multiplier, double base, double max) {
        double total = Math.max(0.0D, totalXp);
        if (total <= 0.0D) return 0L;
        long lo = 0L;
        // No fixed star ceiling: expand the range until it brackets the XP, without overflowing
        // for configurations where stars cost nothing.
        long hi = 1L;
        while (hi < Long.MAX_VALUE && xpRequiredForStars(hi, multiplier, base, max) <= total) {
            hi = hi > Long.MAX_VALUE / 2L ? Long.MAX_VALUE : hi * 2L;
        }
        while (lo < hi) {
            long mid = (lo + hi + 1L) >>> 1;
            if (xpRequiredForStars(mid, multiplier, base, max) <= total) lo = mid;
            else hi = mid - 1L;
        }
        return lo;
    }

    public static double xpIntoCurrentStar(double totalXp) {
        long stars = starsFromTotalXp(totalXp);
        return Math.max(0.0D, Math.max(0.0D, totalXp) - xpRequiredForStars(stars));
    }

    /** Progress towards the next star, 0 to 1, rounded to two decimals. */
    public static double progressToNextStar(double totalXp) {
        long stars = starsFromTotalXp(totalXp);
        double needed = costForNextStar(stars);
        if (needed <= 0.0D) return 0.0D;
        double raw = Math.max(0.0D, Math.min(1.0D, xpIntoCurrentStar(totalXp) / needed));
        return Math.round(raw * 100.0D) / 100.0D;
    }
}
