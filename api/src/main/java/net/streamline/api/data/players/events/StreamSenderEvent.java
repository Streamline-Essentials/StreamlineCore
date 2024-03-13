package net.streamline.api.data.players.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.events.StreamlineEvent;

@Getter @Setter
public class StreamSenderEvent extends StreamlineEvent {
    private StreamSender sender;

    public StreamSenderEvent(StreamSender sender) {
        this.sender = sender;
    }

    public boolean isPlayer() {
        return sender instanceof StreamPlayer;
    }

    public StreamPlayer getPlayer() {
        if (! isPlayer()) return null;

        return (StreamPlayer) sender;
    }
}
