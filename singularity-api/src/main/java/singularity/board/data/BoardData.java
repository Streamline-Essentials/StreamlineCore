package singularity.board.data;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.board.data.constructor.BoardConstructable;
import singularity.board.data.constructor.StringConstructable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Represents a named data payload destined for (or received from) a specific
 * {@link BoardSender} under a given {@link BoardHeader}.
 *
 * <p>The data map holds arbitrary {@link singularity.board.data.constructor.BoardConstructable}
 * values keyed by string names. Instances can be serialised to and parsed from
 * a compact string form via {@link #buildAsString()} and {@link #parseFrom(String)}.
 *
 * <p>Natural ordering is defined by {@link #getIdentifier()}, which combines the
 * sender's class simple name and the header string.
 */
@Setter
@Getter
public class BoardData implements Comparable<BoardData> {

    /** The sender that originated or is associated with this board data entry. */
    private BoardSender<?> sender;

    /** The header categorising this board data entry. */
    private BoardHeader header;

    /** The key-value data payload carried by this entry. */
    private ConcurrentHashMap<String, BoardConstructable<?>> data;

    /**
     * Returns a composite identifier of the form
     * {@code <SenderClass>-><headerString>} used for natural ordering and display.
     *
     * @return the composite identifier string
     */
    public String getIdentifier() {
        return this.sender.getOf().getClass().getSimpleName() + "->" + this.header.getHeader();
    }

    /**
     * Constructs a {@code BoardData} with all three fields supplied explicitly.
     *
     * @param sender the originating or target sender
     * @param header the category header for this data
     * @param data   the initial data payload
     */
    public BoardData(BoardSender<?> sender, BoardHeader header, ConcurrentHashMap<String, BoardConstructable<?>> data) {
        this.sender = sender;
        this.header = header;
        this.data = data;
    }

    /**
     * Constructs a {@code BoardData} with an empty data map.
     *
     * @param sender the originating or target sender
     * @param header the category header for this data
     */
    public BoardData(BoardSender<?> sender, BoardHeader header) {
        this(sender, header, new ConcurrentHashMap<>());
    }

    /**
     * Serialises this {@code BoardData} to a compact string representation.
     *
     * <p>The format is a sequence of {@code !key=value;} tokens, with
     * {@code !--sender} and {@code !--header} reserved keys written first,
     * followed by one token per data entry.
     *
     * @return the serialised string; never {@code null}
     */
    public String buildAsString() {
        StringBuilder builder = new StringBuilder();

        builder.append("!--sender=").append(this.sender.getIdentifier()).append(";");
        builder.append("!--header=").append(this.header.getHeader()).append(";");

        this.data.forEach((key, value) -> {
            builder.append("!").append(key).append("=").append(value.construct()).append(";");
        });

        return builder.toString();
    }

    /**
     * Convenience constructor that wraps raw {@code String} arguments in
     * {@link BoardSender} and {@link BoardHeader} objects.
     *
     * @param sender the sender identifier string
     * @param header the header string
     */
    public BoardData(String sender, String header) {
        this(new BoardSender<>(sender), new BoardHeader(header));
    }

    /**
     * Parses a {@code BoardData} from a string produced by {@link #buildAsString()}.
     *
     * <p>Recognised token keys are {@code --sender} and {@code --header}; all
     * other keys are stored as {@link singularity.board.data.constructor.StringConstructable}
     * values in the data map.
     *
     * @param from the serialised string to parse
     * @return the reconstructed {@code BoardData}; fields may be {@code null}
     *         if the corresponding tokens were absent in {@code from}
     */
    public static BoardData parseFrom(String from) {
        Matcher matcher = MatcherUtils.matcherBuilder("([!](.?*)[=](.?*)[;])", from);
        List<String[]> matches = MatcherUtils.getGroups(matcher, 3);

        BoardSender<?> sender = null;
        BoardHeader header = null;
        ConcurrentSkipListMap<String, BoardConstructable<?>> data = new ConcurrentSkipListMap<>();

        for (String[] match : matches) {
            if (match[1].equals("--sender")) {
                sender = new BoardSender<>(match[2]);
            } else if (match[1].equals("--header")) {
                header = new BoardHeader(match[2]);
            } else {
                data.put(match[1], new StringConstructable(match[2]));
            }
        }

        return new BoardData(sender, header, new ConcurrentHashMap<>(data));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares by {@link #getIdentifier()} lexicographically.
     */
    @Override
    public int compareTo(@NotNull BoardData o) {
        return this.getIdentifier().compareTo(o.getIdentifier());
    }
}
