package host.plas.events.streamline.verification;

import host.plas.discord.MessagedString;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter @Setter
public class VerificationResultEvent extends WithVerificationEvent {
    public enum Result {
        VERIFIED_SUCCESS,
        VERIFIED_FAILURE,

        UNVERIFIED_SUCCESS,
        UNVERIFIED_FAILURE,

        ALREADY_VERIFIED,
        ALREADY_UNVERIFIED,
        ;
    }

    private Result result;

    public VerificationResultEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId,
                                   @Nullable MessagedString message, @Nullable String verification, Result result) {
        super(isFromCommand, uuid, discordId, message, verification);
        this.result = result;
    }
}
