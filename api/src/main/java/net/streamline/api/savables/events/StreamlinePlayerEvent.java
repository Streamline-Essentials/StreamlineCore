package net.streamline.api.savables.events;

import net.streamline.api.savables.users.StreamlinePlayer;

public class StreamlinePlayerEvent extends StreamlineUserEvent<StreamlinePlayer> {
    public StreamlinePlayerEvent(StreamlinePlayer player) {
        super(player);
    }
}
