package singularity.data.server;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CosmicServer implements Identifiable {
    private String identifier;

    public CosmicServer(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return getIdentifier();
    }
}
