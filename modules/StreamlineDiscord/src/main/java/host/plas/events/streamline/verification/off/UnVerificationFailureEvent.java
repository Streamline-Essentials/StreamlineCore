package host.plas.events.streamline.verification.off;

import host.plas.discord.MessagedString;
import org.jetbrains.annotations.Nullable;

public class UnVerificationFailureEvent extends UnVerificationEvent {
    public UnVerificationFailureEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                                      @Nullable MessagedString message, @Nullable String verification) {
        super(isFromCommand, uuid, discordId, message, verification, Result.UNVERIFIED_FAILURE);
    }
}
