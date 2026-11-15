package host.plas.discord.data.events;

import lombok.Getter;

import java.util.Locale;

/**
 * The game events a route can post to Discord. Routes store the constant names, so existing
 * names never change; {@link #getIdentifier()} is the short name shown to owners.
 */
@Getter
public enum EventClassifier {
    // Streamline
    LOGIN("join"),
    LOGOUT("leave"),

    // Bukkit
    ADVANCEMENT("advancement", EventType.BUKKIT),
    DEATH("death", EventType.BUKKIT),

    // Server lifecycle
    SERVER_START("server-start"),
    SERVER_STOP("server-stop"),
    ;

    private final String identifier;
    private final EventType type;

    EventClassifier(String identifier, EventType type) {
        this.identifier = identifier;
        this.type = type;
    }

    EventClassifier(String identifier, boolean isStreamline) {
        this(identifier, isStreamline ? EventType.STREAMLINE : EventType.BUKKIT);
    }

    EventClassifier(String identifier) {
        this(identifier, true);
    }

    /**
     * The classifier named {@code name}, by constant name or identifier, ignoring case and treating
     * {@code -} as {@code _} ({@code server-start}, {@code SERVER_START}, {@code join}), or null.
     */
    public static EventClassifier parse(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        for (EventClassifier classifier : values()) {
            if (classifier.getIdentifier().equalsIgnoreCase(trimmed)) return classifier;
        }
        try {
            return valueOf(trimmed.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
