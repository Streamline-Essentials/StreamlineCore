package host.plas.ratapi;


import singularity.placeholders.expansions.RATExpansion;

public class LoggerExpansion extends RATExpansion {
    public LoggerExpansion() {
        super(
                new RATExpansionBuilder(
                        "logger"
                )
        );
    }

    @Override
    public void init() {
    }
}
