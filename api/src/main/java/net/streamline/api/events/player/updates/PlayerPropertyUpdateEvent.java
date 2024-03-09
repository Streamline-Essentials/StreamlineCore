package net.streamline.api.events.player.updates;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.player.PlatformedPlayerEvent;

@Getter @Setter
public class PlayerPropertyUpdateEvent<T> extends PlatformedPlayerEvent {
    private T toSet;

    public PlayerPropertyUpdateEvent(String playerUuid, T toSet) {
        super(playerUuid);
        this.toSet = toSet;
    }
}
