package net.streamline.api.interfaces.audiences;

import net.streamline.api.interfaces.audiences.getters.PlayerGetter;
import net.streamline.api.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

public interface IPlayerInterface<P> {
    PlayerGetter<P> getPlayerGetter(UUID uuid);

    PlayerGetter<P> getPlayerGetter(String playerName);

    default RealPlayer<P> getPlayer(UUID uuid) {
        return getPlayer(getPlayerGetter(uuid));
    }

    default RealPlayer<P> getPlayer(String playerName) {
        return getPlayer(getPlayerGetter(playerName));
    }

    RealPlayer<P> getPlayer(PlayerGetter<P> playerGetter);
}
