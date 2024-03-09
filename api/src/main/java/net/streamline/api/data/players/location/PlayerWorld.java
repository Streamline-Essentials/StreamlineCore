package net.streamline.api.data.players.location;

import lombok.Getter;
import lombok.Setter;
import tv.quaint.objects.Identifiable;

@Getter @Setter
public class PlayerWorld implements Identifiable {
    private String identifier;

    public PlayerWorld(String worldName) {
        this.identifier = worldName;
    }
}
