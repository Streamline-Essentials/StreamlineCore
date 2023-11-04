package net.streamline.api.messages.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.proxied.ProxiedMessage;

@Getter
public class AnsweredMessageEvent extends StreamlineEvent {
    @Setter
    private ReturnableMessage gateKeeper;
    @Setter
    private ProxiedMessage answer;

    public AnsweredMessageEvent(ReturnableMessage gateKeeper, ProxiedMessage answer) {
        setGateKeeper(gateKeeper);
        setAnswer(answer);
    }
}
