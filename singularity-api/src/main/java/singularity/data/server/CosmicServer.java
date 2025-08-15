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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CosmicServer) {
            CosmicServer other = (CosmicServer) obj;
            return this.getIdentifier().equals(other.getIdentifier());
        } else {
            return super.equals(obj);
        }
    }
}
