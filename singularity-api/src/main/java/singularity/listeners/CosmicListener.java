package singularity.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import singularity.Singularity;

public class CosmicListener implements BaseEventListener {
    public CosmicListener() {
        BaseEventHandler.bake(this, Singularity.getInstance());
    }
}
