package host.plas.data;

import host.plas.StreamersUnite;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import singularity.data.console.CosmicSender;
import singularity.objects.ClickableMessage;
import singularity.utils.Links;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class StreamerSetup implements Comparable<StreamerSetup> {
    public enum CommandType {
        LIVE,
        OFFLINE,
        EMPTY,
        ;
    }

    private UUID streamerUuid;

    private List<String> goLiveCommands;
    private List<String> goOfflineCommands;
    private String streamLink;

    public StreamerSetup(UUID streamerUuid, List<String> goLiveCommands, List<String> goOfflineCommands, String streamLink) {
        this.streamerUuid = streamerUuid;
        this.goLiveCommands = goLiveCommands;
        this.goOfflineCommands = goOfflineCommands;
        this.streamLink = streamLink;
    }

    public void save() {
        StreamersUnite.getStreamerConfig().saveSetup(this);
    }

    public void addCommand(CommandType type, String command) {
        if (type == CommandType.LIVE) addCommandLive(command);
        else if (type == CommandType.OFFLINE) addCommandOffline(command);
    }

    public void addCommandLive(String command) {
        goLiveCommands.add(command);

        save();
    }

    public void addCommandOffline(String command) {
        goOfflineCommands.add(command);

        save();
    }

    public void removeCommand(CommandType type, int index) {
        if (type == CommandType.LIVE) removeCommandLive(index);
        else if (type == CommandType.OFFLINE) removeCommandOffline(index);
    }

    public void removeCommandLive(int index) {
        try {
            goLiveCommands.remove(index);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void removeCommandOffline(int index) {
        try {
            goOfflineCommands.remove(index);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void insertCommand(CommandType type, int index, String command) {
        if (type == CommandType.LIVE) insertCommandLive(index, command);
        else if (type == CommandType.OFFLINE) insertCommandOffline(index, command);
    }

    public void insertCommandLive(int index, String command) {
        try {
            if (index > goLiveCommands.size()) index = goLiveCommands.size();

            goLiveCommands.add(index, command);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void insertCommandOffline(int index, String command) {
        try {
            if (index > goOfflineCommands.size()) index = goOfflineCommands.size();

            goOfflineCommands.add(index, command);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void upCommand(CommandType type, int index) {
        if (type == CommandType.LIVE) upCommandLive(index);
        else if (type == CommandType.OFFLINE) upCommandOffline(index);
    }

    public void upCommandLive(int index) {
        try {
            if (index == 0) return;

            String command = goLiveCommands.get(index);
            goLiveCommands.remove(index);
            goLiveCommands.add(index - 1, command);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void upCommandOffline(int index) {
        try {
            if (index == 0) return;

            String command = goOfflineCommands.get(index);
            goOfflineCommands.remove(index);
            goOfflineCommands.add(index - 1, command);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void downCommand(CommandType type, int index) {
        if (type == CommandType.LIVE) downCommandLive(index);
        else if (type == CommandType.OFFLINE) downCommandOffline(index);
    }

    public void downCommandLive(int index) {
        try {
            if (index == goLiveCommands.size() - 1) return;

            String command = goLiveCommands.get(index);
            goLiveCommands.remove(index);
            goLiveCommands.add(index + 1, command);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    public void downCommandOffline(int index) {
        try {
            if (index == goOfflineCommands.size() - 1) return;

            String command = goOfflineCommands.get(index);
            goOfflineCommands.remove(index);
            goOfflineCommands.add(index + 1, command);

            save();
        } catch (Exception e) {
            StreamersUnite.getInstance().logDebug(e.getStackTrace());
        }
    }

    @Override
    public int compareTo(@NotNull StreamerSetup o) {
        return streamerUuid.compareTo(o.getStreamerUuid());
    }

    /** Tells {@code to} that this streamer is live right now, with their link. */
    public void tellStreamLinkCurrentlyLive(CosmicSender... to) {
        announce(lines(StreamersUnite.getMainConfig().getLiveMessage()), LiveManager.getLink(this), to);
    }

    /** Tells {@code to} that this streamer just went live, with their link. */
    public void tellStreamLinkGoingLive(CosmicSender... to) {
        announce(lines(StreamersUnite.getMainConfig().getGoLiveMessage()), LiveManager.getLink(this), to);
    }

    /** Tells {@code to} that this streamer just went offline. */
    public void tellStreamLinkGoingOffline(CosmicSender... to) {
        announce(lines(StreamersUnite.getMainConfig().getGoOfflineMessage()), LiveManager.getLink(this), to);
    }

    /**
     * Sends each line to every recipient with {@code %display_name%}, {@code %player%} and
     * {@code %link%} filled in. A line showing the link opens it when clicked, on every
     * platform; the rest of the message is plain.
     */
    public void announce(List<String> lines, String link, CosmicSender... to) {
        CosmicSender player = UserUtils.getOrCreateSender(getStreamerUuid().toString()).orElse(null);
        if (player == null) return;

        String name = player.getCurrentName();
        String displayName = name;
        try {
            displayName = player.getDisplayName();
        } catch (Exception e) {
            // The display name needs the player's metadata; the name stands in for it.
        }

        String shown = link == null ? "" : link;
        String url = Links.toClickUrl(shown);
        String hover = StreamersUnite.getMainConfig().getLinkHover();
        for (String line : lines) {
            boolean hasLink = line.contains("%link%");
            String text = line
                    .replace("%display_name%", displayName == null ? "" : displayName)
                    .replace("%player%", name == null ? "" : name)
                    .replace("%link%", shown);
            for (CosmicSender sender : to) {
                if (sender == null) continue;
                if (hasLink && url != null) {
                    new ClickableMessage().text(text.isEmpty() ? " " : text).url(url)
                            .hover(hover.replace("%link%", url)).send(sender);
                } else {
                    sender.sendMessage(text.isEmpty() ? " " : text);
                }
            }
        }
    }

    private static List<String> lines(String message) {
        List<String> lines = new ArrayList<>();
        if (message == null) return lines;
        for (String line : message.split("\\\\n|\n", -1)) lines.add(line);
        return lines;
    }
}
