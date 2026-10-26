package host.plas.data.require;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Requirement implements Identifiable {
    private String identifier;
    private RequirementType type;
    private String value;
    private double amount;

    public Requirement(String identifier, RequirementType type, String value, double amount) {
        this.identifier = identifier;
        this.type = type;
        this.value = value;
        this.amount = amount;
    }
}
