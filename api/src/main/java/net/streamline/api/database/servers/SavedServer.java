package net.streamline.api.database.servers;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.IStreamline;
import tv.quaint.objects.Identifiable;

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
    private IStreamline.ServerType type;

    public SavedServer(String identifier, String name, IStreamline.ServerType type) {
        this.identifier = identifier;
        this.name = name;
        this.type = type;
    }
}
