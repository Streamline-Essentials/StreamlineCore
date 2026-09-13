package host.plas.events.streamline.verification.off;

import host.plas.discord.MessagedString;
import org.jetbrains.annotations.Nullable;

public class UnVerificationAlreadyUnVerifiedEvent extends UnVerificationEvent {
    public UnVerificationAlreadyUnVerifiedEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                                                @Nullable MessagedString message, @Nullable String verification) {
        super(isFromCommand, uuid, discordId, message, verification, Result.ALREADY_UNVERIFIED);
    }
}
