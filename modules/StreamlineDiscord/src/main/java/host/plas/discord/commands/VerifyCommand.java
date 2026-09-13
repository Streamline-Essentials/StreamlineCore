package host.plas.discord.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordHandler;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;

public class VerifyCommand extends DiscordCommand {
    public VerifyCommand() {
        super("verify",
                "ver", "v"
        );
    }

    @Override
    public void init() {
    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction action) {
        return action.addOption(OptionType.STRING, "code", "The code you received from the bot.", true);
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        setReplyEphemeral(StreamlineDiscord.getConfig().verificationResponsesPrivate());
        if (! messagedString.hasCommandArgs()) {
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());
        } else {
            return DiscordHandler.tryVerificationForUser(messagedString, messagedString.getCommandArgsStringed(), true);
        }
    }
}
