package host.plas.data.parties;

import host.plas.data.Party;
import singularity.data.console.CosmicSender;
import host.plas.data.events.GroupChatEvent;

public class PartyChatEvent extends GroupChatEvent<Party> {
    public PartyChatEvent(Party group, CosmicSender sender, String message) {
        super(group, sender, message);
    }
}
