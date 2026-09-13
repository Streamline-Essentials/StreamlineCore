package host.plas.data.parties;

import host.plas.data.Party;
import singularity.data.console.CosmicSender;
import host.plas.data.events.CreateGroupEvent;

public class CreatePartyEvent extends CreateGroupEvent<Party> {
    public CreatePartyEvent(Party group, CosmicSender creator) {
        super(group, creator);
    }
}
