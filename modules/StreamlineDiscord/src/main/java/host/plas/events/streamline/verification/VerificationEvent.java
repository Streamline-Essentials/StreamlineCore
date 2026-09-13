package host.plas.events.streamline.verification;

import host.plas.events.streamline.CosmicDiscordEvent;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerificationEvent extends CosmicDiscordEvent {
    private boolean fromCommand;

    public VerificationEvent(boolean isFromCommand) {
        super();
        this.fromCommand = isFromCommand;
    }
}
