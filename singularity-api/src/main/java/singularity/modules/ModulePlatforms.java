package singularity.modules;

import singularity.Singularity;
import singularity.interfaces.ISingularityExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Decides whether a module jar may load on the running platform, from the
 * {@link SupportedPlatformTag}s in its manifest.
 *
 * <p>Jars built before platform tags existed carry none. They keep loading on server
 * platforms, as they always did, with a warning; a mod-loader client skips them, since no
 * module written before then was meant to run in a game client.</p>
 */
public final class ModulePlatforms {

    private ModulePlatforms() {}

    /**
     * @return the tags the jar declares, or {@code null} if it declares none or cannot be read
     */
    public static Set<SupportedPlatformTag> readTags(Path jar) {
        try (JarFile file = new JarFile(jar.toFile())) {
            Manifest manifest = file.getManifest();
            if (manifest == null) return null;
            String raw = manifest.getMainAttributes().getValue(SupportedPlatformTag.MANIFEST_ATTRIBUTE);
            if (raw == null || raw.trim().isEmpty()) return null;
            return SupportedPlatformTag.parseList(raw);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @return the tags the running platform accepts; empty before Streamline has started
     */
    public static Set<SupportedPlatformTag> accepted() {
        Singularity<?, ?, ?, ?, ?> instance = Singularity.getInstance();
        if (instance == null || instance.getPlatform() == null) return Collections.emptySet();
        ISingularityExtension platform = instance.getPlatform();
        Set<SupportedPlatformTag> tags = platform.getSupportedPlatformTags();
        return tags == null ? Collections.emptySet() : tags;
    }

    /**
     * @return why the jar may not load here, or {@code null} if it may
     */
    public static String refusal(Path jar) {
        Set<SupportedPlatformTag> accepted = accepted();
        Set<SupportedPlatformTag> tags = readTags(jar);
        String name = jar.getFileName().toString();

        if (tags == null) {
            if (accepted.contains(SupportedPlatformTag.MODDED_CLIENT) && ! acceptsServerTags(accepted)) {
                return "'" + name + "' declares no supported platforms, so it is not loaded on a game client.";
            }
            return null;
        }

        for (SupportedPlatformTag tag : tags) {
            if (accepted.contains(tag)) return null;
        }
        return "'" + name + "' supports " + (tags.isEmpty() ? "no known platform" : SupportedPlatformTag.join(tags))
                + ", but this platform runs " + SupportedPlatformTag.join(accepted) + ".";
    }

    /**
     * @return whether the jar declares no platform tags at all
     */
    public static boolean isUntagged(Path jar) {
        return readTags(jar) == null;
    }

    private static boolean acceptsServerTags(Set<SupportedPlatformTag> accepted) {
        Set<SupportedPlatformTag> servers = EnumSet.of(SupportedPlatformTag.PROXY, SupportedPlatformTag.SPIGOT,
                SupportedPlatformTag.PAPER, SupportedPlatformTag.MODDED_SERVER);
        for (SupportedPlatformTag tag : accepted) {
            if (servers.contains(tag)) return true;
        }
        return false;
    }
}
