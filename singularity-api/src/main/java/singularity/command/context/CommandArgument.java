package singularity.command.context;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a single positional argument passed to a command. Each argument has a
 * zero-based {@link #index}, a raw {@link #content} string, and an optional
 * {@link #overrideType} that forces a specific {@link ContentType} classification.
 *
 * <p>Instances are comparable by their index so they sort naturally within a
 * {@link java.util.concurrent.ConcurrentSkipListSet}.</p>
 */
@Getter
public class CommandArgument implements Comparable<CommandArgument> {

    /**
     * Describes the usability state of a {@link CommandArgument}'s content.
     */
    public enum ContentType {
        /** The content is a non-null, non-blank string that can be used. */
        USABLE_STRING,
        /** The content is an empty or blank string. */
        EMPTY_STRING,
        /** The content reference is {@code null}. */
        NULL_STRING,
        /**
         * The argument is in a broken state (e.g. created via the no-arg constructor)
         * and should not be used.
         */
        BROKEN,
        ;
    }

    /** The zero-based position of this argument in the command invocation. */
    private final int index;

    /** The raw string content of this argument. */
    @Setter
    private String content;

    /**
     * When non-null, this type is returned by {@link #getContentType()} instead of
     * the type derived from {@link #content}.
     */
    @Setter @Nullable
    private ContentType overrideType;

    /**
     * Creates a fully specified {@code CommandArgument}.
     *
     * @param index        the zero-based position of this argument
     * @param content      the raw string content
     * @param overrideType an explicit content type, or {@code null} to derive it from {@code content}
     */
    public CommandArgument(int index, String content, @Nullable ContentType overrideType) {
        this.index = index;
        this.content = content;
        this.overrideType = overrideType;
    }

    /**
     * Creates a {@code CommandArgument} whose content type is derived automatically
     * from {@code content}.
     *
     * @param index   the zero-based position of this argument
     * @param content the raw string content
     */
    public CommandArgument(int index, String content) {
        this(index, content, null);
    }

    /**
     * Creates a broken placeholder argument with index {@code -1}, {@code null} content,
     * and {@link ContentType#BROKEN}. Intended to represent a missing argument slot.
     */
    public CommandArgument() {
        this(-1, null, ContentType.BROKEN);
    }

    /**
     * Returns the effective {@link ContentType} of this argument. If an override type is
     * set it takes priority; otherwise the type is derived from {@link #content}.
     *
     * @return the content type classification of this argument
     */
    public ContentType getContentType() {
        if (overrideType != null) return overrideType;

        if (content == null) return ContentType.NULL_STRING;
        if (content.isEmpty()) return ContentType.EMPTY_STRING;
        if (content.isBlank()) return ContentType.EMPTY_STRING;

        return ContentType.USABLE_STRING;
    }

    /**
     * Returns {@code true} if this argument contains a non-null, non-blank string.
     *
     * @return {@code true} when the content type is {@link ContentType#USABLE_STRING}
     */
    public boolean isUsable() {
        return getContentType() == ContentType.USABLE_STRING;
    }

    /**
     * Returns {@code true} if this argument's content is empty or blank.
     *
     * @return {@code true} when the content type is {@link ContentType#EMPTY_STRING}
     */
    public boolean isEmpty() {
        return getContentType() == ContentType.EMPTY_STRING;
    }

    /**
     * Returns {@code true} if this argument's content reference is {@code null}.
     *
     * @return {@code true} when the content type is {@link ContentType#NULL_STRING}
     */
    public boolean isNull() {
        return getContentType() == ContentType.NULL_STRING;
    }

    /**
     * Checks whether this argument's raw content is equal to the given string using
     * {@link java.util.Objects#equals}.
     *
     * @param string the string to compare against
     * @return {@code true} if the content equals {@code string}
     */
    public boolean equals(String string) {
        return Objects.equals(content, string);
    }

    /**
     * Returns the content of this argument, or an empty string if the argument is not
     * {@link #isUsable() usable} (null, empty, or broken).
     *
     * @return the usable content string, or {@code ""} if not usable
     */
    public String getContent() {
        if (! isUsable()) return "";

        return content;
    }

    /**
     * {@inheritDoc}
     *
     * Compares arguments by their {@link #index} so that sorted collections maintain
     * natural positional order.
     */
    @Override
    public int compareTo(@NotNull CommandArgument o) {
        return Integer.compare(index, o.index);
    }
}
