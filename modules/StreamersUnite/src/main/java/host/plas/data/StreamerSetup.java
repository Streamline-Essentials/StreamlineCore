package host.plas.data;

import host.plas.StreamersUnite;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;

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

    public void tellStreamLinkCurrentlyLive(CosmicSender... to) {
        CosmicSender player = UserUtils.getOrCreateSender(getStreamerUuid().toString()).orElse(null);
        if (player == null) return;

        String playerName = player.getUuid();
        try {
            playerName = player.getDisplayName();
        } catch (Exception e) {
            // do nothing
        }

        String goLiveMessage = StreamersUnite.getMainConfig().getLiveMessage();
        goLiveMessage = goLiveMessage.replace("%display_name%", playerName);
        goLiveMessage = goLiveMessage.replace("%link%", streamLink);

        for (CosmicSender sender : to) {
            sender.sendMessage(goLiveMessage);
        }
    }

    public void tellStreamLinkGoingLive(CosmicSender... to) {
        CosmicSender player = UserUtils.getOrCreateSender(getStreamerUuid().toString()).orElse(null);
        if (player == null) return;

        String playerName = player.getUuid();
        try {
            playerName = player.getDisplayName();
        } catch (Exception e) {
            // do nothing
        }

        String goLiveMessage = StreamersUnite.getMainConfig().getGoLiveMessage();
        goLiveMessage = goLiveMessage.replace("%display_name%", playerName);
        goLiveMessage = goLiveMessage.replace("%link%", streamLink);

        for (CosmicSender sender : to) {
            sender.sendMessage(goLiveMessage);
        }
    }

    public void tellStreamLinkGoingOffline(CosmicSender... to) {
        StringBuilder stringBuilder = new StringBuilder();

        CosmicSender player = UserUtils.getOrCreateSender(getStreamerUuid().toString()).orElse(null);
        if (player == null) return;

        String playerName = player.getUuid();
        try {
            playerName = player.getDisplayName();
        } catch (Exception e) {
            // do nothing
        }

        stringBuilder.append("&b").append(playerName).append(" &ehas just gone &coffline&8. &eThey were live at&8: &d&o").append(streamLink).append("&8!");

        for (CosmicSender sender : to) {
            sender.sendMessage(stringBuilder.toString());
        }
    }
}
