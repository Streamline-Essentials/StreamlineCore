package net.streamline.api.events.player.updates;

import lombok.Getter;
import net.streamline.api.events.player.PlatformedPlayerEvent;

@Getter
public class PlayerPropertyUpdateEvent<T> extends PlatformedPlayerEvent {
    T toSet;

    public PlayerPropertyUpdateEvent(String playerUuid, T toSet) {
        super(playerUuid);
        this.toSet = toSet;
    }
}
