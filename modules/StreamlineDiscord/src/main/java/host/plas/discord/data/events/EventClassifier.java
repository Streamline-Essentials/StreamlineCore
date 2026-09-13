package host.plas.discord.data.events;

import lombok.Getter;

@Getter
public enum EventClassifier {
    // Streamline
    LOGIN("join"),
    LOGOUT("leave"),

    // Bukkit
    ADVANCEMENT("advancement", EventType.BUKKIT),
    DEATH("death", EventType.BUKKIT),
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
}
