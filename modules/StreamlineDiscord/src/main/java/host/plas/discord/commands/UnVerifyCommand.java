package host.plas.discord.commands;

import host.plas.config.VerifiedUsers;
import host.plas.discord.data.verified.VerifiedUser;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import host.plas.events.streamline.verification.off.UnVerificationAlreadyUnVerifiedEvent;
import host.plas.events.streamline.verification.off.UnVerificationFailureEvent;
import host.plas.events.streamline.verification.off.UnVerificationSuccessEvent;

import java.util.Optional;

public class UnVerifyCommand extends DiscordCommand {
    public UnVerifyCommand() {
        super("unverify",
                "unver", "uv"
        );
    }

    @Override
    public void init() {
    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction action) {
        return action;
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        setReplyEphemeral(StreamlineDiscord.getConfig().verificationResponsesPrivate());
        if (! messagedString.hasCommandArgs()) {
            new UnVerificationFailureEvent(true, null, messagedString.getAuthor().getIdLong(), messagedString, null).fire();
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().unVerifiedFailureGenericDiscord());
        } else {
            Optional<VerifiedUser> optional = VerifiedUsers.getById(messagedString.getAuthor().getIdLong());
            if (optional.isPresent()) {
                VerifiedUser verified = optional.get();
                verified.unverify();

                new UnVerificationSuccessEvent(true, verified.getUuid(), messagedString.getAuthor().getIdLong(), messagedString, null).fire();
                return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().unVerifiedSuccessDiscord());
            } else {
                new UnVerificationAlreadyUnVerifiedEvent(true, null, null, messagedString, null).fire();
                return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().unVerifiedFailureAlreadyUnVerifiedDiscord());
            }
        }
    }
}
