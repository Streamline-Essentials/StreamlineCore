package host.plas.events.streamline.verification.off;

import host.plas.discord.MessagedString;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Setter
@Getter
public class UnVerificationSuccessEvent extends UnVerificationEvent {
    public UnVerificationSuccessEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                                      @Nullable MessagedString message, @Nullable String verification) {
        super(isFromCommand, uuid, discordId, message, verification, Result.UNVERIFIED_SUCCESS);
    }
}
