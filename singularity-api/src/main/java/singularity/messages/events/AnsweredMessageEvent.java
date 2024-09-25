package singularity.messages.events;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.messages.answered.ReturnableMessage;
import singularity.messages.proxied.ProxiedMessage;

@Setter
@Getter
public class AnsweredMessageEvent extends CosmicEvent {
    private ReturnableMessage gateKeeper;
    private ProxiedMessage answer;

    public AnsweredMessageEvent(ReturnableMessage gateKeeper, ProxiedMessage answer) {
        setGateKeeper(gateKeeper);
        setAnswer(answer);
    }
}
