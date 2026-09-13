package host.plas.bukkit.events.streamline;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import org.bukkit.event.Event;
import host.plas.bukkit.events.EventHandler;

public class SLBListener implements BaseEventListener {
    @BaseProcessor
    public <E extends Event> void onEvent(EventingEvent<E> ev) {
        EventHandler.handle(ev.getEventName(), ev.getEv());
    }
}
