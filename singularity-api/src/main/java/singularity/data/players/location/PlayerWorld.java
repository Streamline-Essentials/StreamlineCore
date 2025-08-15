package singularity.data.players.location;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.data.server.CosmicServer;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayerWorld) {
            PlayerWorld other = (PlayerWorld) obj;
            return this.getIdentifier().equals(other.getIdentifier());
        } else {
            return super.equals(obj);
        }
    }
}
