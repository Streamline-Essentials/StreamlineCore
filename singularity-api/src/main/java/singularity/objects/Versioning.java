package singularity.objects;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import singularity.Singularity;
import singularity.interfaces.ISingularityExtension;
import singularity.utils.MessageUtils;

/**
 * A three-part version number (major.minor.patch) with comparison helpers, plus the
 * Minecraft version of the running server ({@link #getServerVersion()}), read from
 * {@link ISingularityExtension#getMinecraftVersion()} on every platform.
 *
 * <p>Mirrors BukkitOfUtils' {@code Versioning}, so code written against one reads the same
 * against the other.</p>
 */
@Getter @Setter
public class Versioning implements Comparable<Versioning> {
    /** Cached server version, populated by {@link #introspect()}. */
    private static volatile Versioning SERVER_VERSION = null;

    /** The major version number. */
    private long first;
    /** The minor version number. */
    private long second;
    /** The patch version number. */
    private long third;

    /**
     * @param first  the major version number
     * @param second the minor version number
     * @param third  the patch version number
     */
    public Versioning(long first, long second, long third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public String toString() {
        return first + "." + second + "." + third;
    }

    /** @return whether the minor component is below {@code minorVersion} */
    public boolean isBefore(long minorVersion) {
        return this.second < minorVersion;
    }

    /** @return whether the minor component is above {@code minorVersion} */
    public boolean isAfter(long minorVersion) {
        return this.second > minorVersion;
    }

    /** @return whether the minor component equals {@code minorVersion} */
    public boolean isSame(long minorVersion) {
        return this.second == minorVersion;
    }

    /** @return whether minor.patch is before {@code minorVersion.patchVersion} */
    public boolean isBefore(long minorVersion, long patchVersion) {
        if (this.second < minorVersion) return true;
        if (this.second > minorVersion) return false;
        return this.third < patchVersion;
    }

    /** @return whether minor.patch is after {@code minorVersion.patchVersion} */
    public boolean isAfter(long minorVersion, long patchVersion) {
        if (this.second > minorVersion) return true;
        if (this.second < minorVersion) return false;
        return this.third > patchVersion;
    }

    /** @return whether minor.patch equals {@code minorVersion.patchVersion} */
    public boolean isSame(long minorVersion, long patchVersion) {
        return this.second == minorVersion && this.third == patchVersion;
    }

    /** @return whether this version is before {@code other} */
    public boolean isBefore(Versioning other) {
        if (this.first != other.first) return this.first < other.first;
        if (this.second != other.second) return this.second < other.second;
        return this.third < other.third;
    }

    /** @return whether this version is after {@code other} */
    public boolean isAfter(Versioning other) {
        if (this.first != other.first) return this.first > other.first;
        if (this.second != other.second) return this.second > other.second;
        return this.third > other.third;
    }

    /** @return whether all three components match {@code other} */
    public boolean isSame(Versioning other) {
        return this.first == other.first && this.second == other.second && this.third == other.third;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (! (obj instanceof Versioning)) return false;
        return isSame((Versioning) obj);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(first) * 961 + Long.hashCode(second) * 31 + Long.hashCode(third);
    }

    @Override
    public int compareTo(@NotNull Versioning o) {
        if (isBefore(o)) return -1;
        if (isAfter(o)) return 1;
        return 0;
    }

    /** @return whether this is the empty version {@code 0.0.0} (unknown) */
    public boolean isEmpty() {
        return first == 0 && second == 0 && third == 0;
    }

    /** @return whether this is {@link #getModern()}: a modern server whose exact version is unknown */
    public boolean isModernErrored() {
        return first == -2 && second == 0 && third == 0;
    }

    /** @return whether this version is after 1.20.4 (or {@link #isModernErrored()}) */
    public boolean isModern() {
        if (isModernErrored()) return true;
        return isAfter(new Versioning(1, 20, 4));
    }

    /**
     * Parses a version string such as {@code 1.20.4}, {@code 1.20.4-R0.1-SNAPSHOT},
     * {@code 26.2.build.3} or {@code 1.21.x}; non-numeric parts read as 0.
     *
     * @param version the version string
     * @return the parsed version, or {@link #getEmpty()} when it cannot be parsed
     */
    public static Versioning fromString(String version) {
        if (version == null || version.trim().isEmpty()) return getEmpty();
        String original = version;
        try {
            version = normalizeVersionString(version.trim());

            String[] parts = version.split("\\.");
            long first = parts.length > 0 ? parseVersionPart(parts[0]) : 0;
            long second = parts.length > 1 ? parseVersionPart(parts[1]) : 0;
            long third = parts.length > 2 ? parseVersionPart(parts[2]) : 0;
            return new Versioning(first, second, third);
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to parse version string: " + original + " (" + e.getMessage() + ")");
            return getEmpty();
        }
    }

    /**
     * Strips snapshot/build suffixes ({@code -R0.1-SNAPSHOT}) and Paper build metadata
     * ({@code 26.2.build.3}) before numeric parsing.
     */
    static String normalizeVersionString(String version) {
        version = version.split("-", 2)[0];

        int buildMetadataIndex = version.indexOf(".build.");
        if (buildMetadataIndex != -1) version = version.substring(0, buildMetadataIndex);

        return version;
    }

    private static long parseVersionPart(String part) {
        String numbers = grabOnlyNumbers(part).replace(".", "");
        if (numbers.isEmpty()) return 0;
        return Long.parseLong(numbers);
    }

    /**
     * @param from the string to process
     * @return {@code from} with everything but digits and dots removed
     */
    public static String grabOnlyNumbers(String from) {
        return from.replaceAll("[^0-9.]", "");
    }

    /** @return the empty version {@code 0.0.0} */
    public static Versioning getEmpty() {
        return new Versioning(0, 0, 0);
    }

    /** @return a version standing for a modern server whose exact version is unknown */
    public static Versioning getModern() {
        return new Versioning(-2, 0, 0);
    }

    /**
     * Reads the server's Minecraft version from the platform and caches it. An unknown
     * version (a mod server that has not started yet) is returned but not cached, so a
     * later call reads it again.
     *
     * @return the server's version
     */
    public static Versioning introspect() {
        Versioning version = fromString(getRawServerVersion());
        SERVER_VERSION = version.isEmpty() ? null : version;
        return version;
    }

    /** @return the server's Minecraft version, introspected on first use */
    public static Versioning getServerVersion() {
        Versioning cached = SERVER_VERSION;
        return cached == null ? introspect() : cached;
    }

    /**
     * The Minecraft version string the platform reports, e.g. {@code 1.21.4-R0.1-SNAPSHOT} on
     * Spigot, {@code 1.21.1} on mod loaders, the newest supported version on proxies.
     *
     * @return the raw version, or an empty string when unknown
     */
    public static String getRawServerVersion() {
        try {
            String raw = Singularity.getInstance().getPlatform().getMinecraftVersion();
            return raw == null ? "" : raw;
        } catch (Throwable e) {
            return "";
        }
    }
}
