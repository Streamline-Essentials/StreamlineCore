package host.plas.data.events;

import host.plas.data.AbstractGroup;
import lombok.Getter;
import singularity.data.console.CosmicSender;

@Getter
public class InviteGroupEvent<T extends AbstractGroup> extends GroupEvent<T> {
    private final CosmicSender invited;

    public InviteGroupEvent(T group, CosmicSender invited) {
        super(group);
        this.invited = invited;
    }
}
