package host.plas.events;

import host.plas.StreamlineUtilities;
import singularity.events.modules.ModuleEvent;

public class UtilityEvent extends ModuleEvent {
    public UtilityEvent() {
        super(StreamlineUtilities.getInstance());
    }
}
