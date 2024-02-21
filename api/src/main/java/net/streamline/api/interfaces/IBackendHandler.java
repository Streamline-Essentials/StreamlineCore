package net.streamline.api.interfaces;

import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.location.PlayerLocation;

public interface IBackendHandler {
    public void teleport(StreamPlayer player, PlayerLocation location);
}
