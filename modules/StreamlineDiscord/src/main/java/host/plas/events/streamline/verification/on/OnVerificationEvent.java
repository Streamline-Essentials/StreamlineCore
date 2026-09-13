package host.plas.events.streamline.verification.on;

import host.plas.discord.MessagedString;
import host.plas.events.streamline.verification.VerificationResultEvent;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter @Setter
public class OnVerificationEvent extends VerificationResultEvent {
    public OnVerificationEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                               @Nullable MessagedString message, @Nullable String verification, Result result) {
        super(isFromCommand, uuid, discordId, message, verification, result);
    }
}
