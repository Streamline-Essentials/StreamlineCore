package net.streamline.api.interfaces;

import net.streamline.api.savables.users.StreamlineLocation;
import net.streamline.api.savables.users.StreamlinePlayer;

public interface IBackendHandler {
    public void teleport(StreamlinePlayer player, StreamlineLocation location);
}
