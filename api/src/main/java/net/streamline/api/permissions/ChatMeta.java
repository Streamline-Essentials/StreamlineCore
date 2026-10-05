package net.streamline.api.permissions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A resolved chat-meta value (a prefix or suffix) and the priority it was found at.
 */
@Getter
@AllArgsConstructor
public class ChatMeta {
    /** No value, at priority 0. */
    public static final ChatMeta EMPTY = new ChatMeta("", 0);

    /** The prefix or suffix text; never {@code null}. */
    private final String value;
    /** The priority the value was resolved at. */
    private final int priority;
}
