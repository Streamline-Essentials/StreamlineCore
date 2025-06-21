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

@Setter
@Getter
public class BoardData implements Comparable<BoardData> {
    private BoardSender<?> sender;
    private BoardHeader header;
    private ConcurrentHashMap<String, BoardConstructable<?>> data;

    public String getIdentifier() {
        return this.sender.getOf().getClass().getSimpleName() + "->" + this.header.getHeader();
    }

    public BoardData(BoardSender<?> sender, BoardHeader header, ConcurrentHashMap<String, BoardConstructable<?>> data) {
        this.sender = sender;
        this.header = header;
        this.data = data;
    }

    public BoardData(BoardSender<?> sender, BoardHeader header) {
        this(sender, header, new ConcurrentHashMap<>());
    }

    public String buildAsString() {
        StringBuilder builder = new StringBuilder();

        builder.append("!--sender=").append(this.sender.getIdentifier()).append(";");
        builder.append("!--header=").append(this.header.getHeader()).append(";");

        this.data.forEach((key, value) -> {
            builder.append("!").append(key).append("=").append(value.construct()).append(";");
        });

        return builder.toString();
    }

    public BoardData(String sender, String header) {
        this(new BoardSender<>(sender), new BoardHeader(header));
    }

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

    @Override
    public int compareTo(@NotNull BoardData o) {
        return this.getIdentifier().compareTo(o.getIdentifier());
    }
}
