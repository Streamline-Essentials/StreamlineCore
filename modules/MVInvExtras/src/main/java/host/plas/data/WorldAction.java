package host.plas.data;

import lombok.Getter;
import lombok.Setter;
import gg.drak.thebase.objects.Identifiable;

@Getter @Setter
public class WorldAction implements Identifiable {
    private String identifier;
    private String regex;
    private boolean createOwn;

    public WorldAction(String identifier, String regex, boolean createOwn) {
        this.identifier = identifier;
        this.regex = regex;
        this.createOwn = createOwn;
    }
}
