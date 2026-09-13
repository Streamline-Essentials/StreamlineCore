package host.plas.events.streamline.verification.off;

import host.plas.discord.MessagedString;
import lombok.Getter;
import lombok.Setter;
import host.plas.events.streamline.verification.VerificationResultEvent;
import org.jetbrains.annotations.Nullable;

@Setter
@Getter
public class UnVerificationEvent extends VerificationResultEvent {
    public UnVerificationEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                                   @Nullable MessagedString message, @Nullable String verification, Result result) {
        super(isFromCommand, uuid, discordId, message, verification, result);
    }
}
