package host.plas.data.events;

import host.plas.data.AbstractGroup;
import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;

@Getter @Setter
public class GroupEvent<T extends AbstractGroup> extends CosmicEvent {
    private T group;

    public GroupEvent(T group) {
        this.group = group;
    }
}
