package host.plas.events;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.ProxProtect;
import host.plas.data.cause.HazardCause;
import host.plas.data.cause.HazardCauseType;
import singularity.events.server.CosmicChatEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProxListener implements BaseEventListener {
    @BaseProcessor
    public void onChatEvent(CosmicChatEvent event) {
        HazardCause cause = new HazardCause(event.getPlayer(), HazardCauseType.CHAT, event.getMessage());

        AtomicBoolean cancel = new AtomicBoolean(false);
        ProxProtect.getHazardsConfig().getHazards().forEach(hazard -> {
            if (cancel.get()) return;

            boolean bool = hazard.checkAndConclude(cause);
            cancel.set(bool);
        });

        if (cancel.get()) {
            event.setCanceled(true); // Chat Event.
            event.setCancelled(true); // Base Event.
        }
    }
}
