package singularity.database.servers;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.interfaces.ISingularityExtension;

@Getter @Setter
public class SavedServer implements Identifiable {
    private String identifier;

    public String getUuid() {
        return identifier;
    }

    public void setUuid(String uuid) {
        this.identifier = uuid;
    }

    private String name;
    private ISingularityExtension.ServerType type;

    public SavedServer(String identifier, String name, ISingularityExtension.ServerType type) {
        this.identifier = identifier;
        this.name = name;
        this.type = type;
    }

    public void push() {
        // Push to database.
        Singularity.getMainDatabase().putServerAsync(this);
    }
}
