package host.plas.data.events;

import host.plas.data.Party;
import lombok.Getter;
import singularity.data.console.CosmicSender;

@Getter
public class CreateGroupEvent<T extends Party> extends GroupEvent<T> {
    private final CosmicSender creator;

    public CreateGroupEvent(T group, CosmicSender creator) {
        super(group);
        this.creator = creator;
    }
}
