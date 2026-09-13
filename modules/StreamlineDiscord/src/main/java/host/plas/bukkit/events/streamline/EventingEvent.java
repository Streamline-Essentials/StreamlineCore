package host.plas.bukkit.events.streamline;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import host.plas.StreamlineDiscord;
import singularity.events.modules.ModuleEvent;

@Setter
@Getter
public class EventingEvent<T extends Event> extends ModuleEvent {
    String identifier;
    T ev;

    public EventingEvent(String identifier, T ev) {
        super(StreamlineDiscord.getInstance());
        setIdentifier(identifier);
        setEv(ev);
    }
}
