package host.plas.data.events;

import host.plas.data.Party;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;

@Setter @Getter
public class GroupChatEvent<T extends Party> extends GroupEvent<T> {
    private String message;
    private CosmicSender sender;

    public GroupChatEvent(T group, CosmicSender sender, String message) {
        super(group);
        setSender(sender);
        setMessage(message);
    }
}
