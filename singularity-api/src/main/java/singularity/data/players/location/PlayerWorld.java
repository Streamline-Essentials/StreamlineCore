package singularity.data.players.location;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlayerWorld implements Identifiable {
    private String identifier;

    public PlayerWorld(String worldName) {
        this.identifier = worldName;
    }

    @Override
    public String toString() {
        return getIdentifier();
    }
}
