package net.streamline.api.middleware;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.PlayerLike;

import java.util.UUID;

@Getter @Setter
public class MPlayer implements PlayerLike {
    private final UUID uuid;
    private final String username;

    public MPlayer(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }
}
