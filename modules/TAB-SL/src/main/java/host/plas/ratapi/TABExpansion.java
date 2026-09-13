package host.plas.ratapi;

import singularity.placeholders.expansions.RATExpansion;

public class TABExpansion extends RATExpansion {
    public TABExpansion() {
        super(
                new RATExpansionBuilder(
                        "tab"
                )
        );
    }

    @Override
    public void init() {

    }
}
