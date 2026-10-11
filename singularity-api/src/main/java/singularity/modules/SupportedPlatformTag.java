package singularity.modules;

import lombok.Getter;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A kind of platform a module can run on. Every module jar lists the tags it supports in its
 * {@value #MANIFEST_ATTRIBUTE} manifest attribute (from {@code pf.platforms} in the module's
 * {@code gradle.properties}), and a platform loads a module only if they share a tag.
 *
 * <p>A platform may accept several tags: Paper also runs modules written for Spigot, and a
 * mod-loader client that hosts a world runs server modules on its integrated server.</p>
 */
public enum SupportedPlatformTag {
    /** Velocity or BungeeCord. */
    PROXY("proxy"),
    /** Spigot, and every server built on it. */
    SPIGOT("spigot"),
    /** Paper and its forks (Folia, Purpur, ...): servers with the Paper API. */
    PAPER("paper"),
    /** A Fabric, Forge or NeoForge server, dedicated or integrated. */
    MODDED_SERVER("modded-server"),
    /**
     * A Fabric, Forge or NeoForge game client. Alone, it marks a client module: one that extends
     * {@code ClientModule} and runs in the client's own module core, which servers never load.
     * Together with {@link #MODDED_SERVER}, it marks a world module: a server module that game
     * clients also offer from eCloud and manage, and run on the integrated server of the worlds
     * they host (singleplayer or LAN). Servers load such a module by its other tags and ignore
     * this one.
     */
    MODDED_CLIENT("modded-client"),
    ;

    /** The manifest attribute holding a module's comma-separated tags. */
    public static final String MANIFEST_ATTRIBUTE = "Plugin-Platforms";

    @Getter
    private final String id;

    SupportedPlatformTag(String id) {
        this.id = id;
    }

    /**
     * @return the tag with this id, ignoring case and surrounding space, or {@code null}
     */
    public static SupportedPlatformTag fromId(String id) {
        if (id == null) return null;
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (SupportedPlatformTag tag : values()) {
            if (tag.id.equals(wanted)) return tag;
        }
        return null;
    }

    /**
     * Parses a comma-separated tag list, skipping unknown ids.
     */
    public static Set<SupportedPlatformTag> parseList(String list) {
        Set<SupportedPlatformTag> tags = EnumSet.noneOf(SupportedPlatformTag.class);
        if (list == null) return tags;
        for (String part : list.split(",")) {
            SupportedPlatformTag tag = fromId(part);
            if (tag != null) tags.add(tag);
        }
        return tags;
    }

    public static String join(Collection<SupportedPlatformTag> tags) {
        return tags.stream().map(SupportedPlatformTag::getId).collect(Collectors.joining(", "));
    }
}
