package net.streamline.api.events.player;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;

@Getter
public class PlatformedPlayerEvent extends StreamlineEvent {
    final String playerUuid;

    public PlatformedPlayerEvent(String playerUuid) {
        this.playerUuid = playerUuid;
    }
}
