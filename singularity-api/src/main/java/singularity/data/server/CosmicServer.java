package singularity.data.server;

import lombok.Getter;
import lombok.Setter;
import tv.quaint.objects.Identifiable;

@Getter @Setter
public class CosmicServer implements Identifiable {
    private String identifier;

    public CosmicServer(String identifier) {
        this.identifier = identifier;
    }
}
