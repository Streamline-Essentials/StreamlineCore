package singularity.messages.events;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.messages.answered.ReturnableMessage;
import singularity.messages.proxied.ProxiedMessage;

/**
 * Fired by the Singularity event bus when a {@link ReturnableMessage} receives and
 * accepts a correlated reply.
 *
 * <p>Listeners may cancel this event to prevent the answer from being forwarded to
 * the registered consumer callbacks and to abort the {@link ReturnableMessage#fire}
 * call. Cancelling does not prevent the message from being unregistered.</p>
 */
@Setter
@Getter
public class AnsweredMessageEvent extends CosmicEvent {

    /**
     * The {@link ReturnableMessage} that matched the incoming reply.
     */
    private ReturnableMessage gateKeeper;

    /**
     * The {@link ProxiedMessage} that was accepted as the reply.
     */
    private ProxiedMessage answer;

    /**
     * Creates a new {@code AnsweredMessageEvent} for the given returnable message
     * and its accepted reply.
     *
     * @param gateKeeper the {@link ReturnableMessage} that matched the reply
     * @param answer     the accepted reply {@link ProxiedMessage}
     */
    public AnsweredMessageEvent(ReturnableMessage gateKeeper, ProxiedMessage answer) {
        setGateKeeper(gateKeeper);
        setAnswer(answer);
    }
}
