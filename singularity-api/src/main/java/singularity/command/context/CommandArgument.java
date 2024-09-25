package singularity.command.context;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class CommandArgument implements Comparable<CommandArgument> {
    public enum ContentType {
        USABLE_STRING,
        EMPTY_STRING,
        NULL_STRING,
        BROKEN,
        ;
    }

    private final int index;
    @Setter
    private String content;
    @Setter @Nullable
    private ContentType overrideType;

    public CommandArgument(int index, String content, @Nullable ContentType overrideType) {
        this.index = index;
        this.content = content;
        this.overrideType = overrideType;
    }

    public CommandArgument(int index, String content) {
        this(index, content, null);
    }

    public CommandArgument() {
        this(-1, null, ContentType.BROKEN);
    }

    public ContentType getContentType() {
        if (overrideType != null) return overrideType;

        if (content == null) return ContentType.NULL_STRING;
        if (content.isEmpty()) return ContentType.EMPTY_STRING;
        if (content.isBlank()) return ContentType.EMPTY_STRING;

        return ContentType.USABLE_STRING;
    }

    public boolean isUsable() {
        return getContentType() == ContentType.USABLE_STRING;
    }

    public boolean isEmpty() {
        return getContentType() == ContentType.EMPTY_STRING;
    }

    public boolean isNull() {
        return getContentType() == ContentType.NULL_STRING;
    }

    public boolean equals(String string) {
        return Objects.equals(content, string);
    }

    public String getContent() {
        if (! isUsable()) return "";

        return content;
    }

    @Override
    public int compareTo(@NotNull CommandArgument o) {
        return Integer.compare(index, o.index);
    }
}
