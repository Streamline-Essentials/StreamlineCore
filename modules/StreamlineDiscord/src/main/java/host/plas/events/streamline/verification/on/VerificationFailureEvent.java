package host.plas.events.streamline.verification.on;

import host.plas.discord.MessagedString;
import org.jetbrains.annotations.Nullable;

public class VerificationFailureEvent extends OnVerificationEvent {
    public VerificationFailureEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                                    @Nullable MessagedString message, @Nullable String verification) {
        super(isFromCommand, uuid, discordId, message, verification, Result.VERIFIED_FAILURE);
    }
}
