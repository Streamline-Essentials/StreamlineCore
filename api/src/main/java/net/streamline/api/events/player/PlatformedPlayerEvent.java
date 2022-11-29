package net.streamline.api.events.player;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;

public class PlatformedPlayerEvent extends StreamlineEvent {
    @Getter
    final String playerUuid;

    public PlatformedPlayerEvent(String playerUuid) {
        this.playerUuid = playerUuid;
    }
}
