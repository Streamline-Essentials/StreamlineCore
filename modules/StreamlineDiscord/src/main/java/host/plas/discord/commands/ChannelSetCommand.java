package host.plas.discord.commands;

import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.events.EventClassifier;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@Setter
@Getter
public class ChannelSetCommand extends DiscordCommand {
    private String replyMessageSet;
    private String replyMessageInfo;
    private String replyMessageNone;

    public ChannelSetCommand() {
        super("channelset",
                -1L,
                "chanset", "chset", "channelset"
        );

        setReplyMessageSet(getResource().getOrSetDefault("messages.reply.set", "--file:channel-set-response.json"));
        setReplyMessageInfo(getResource().getOrSetDefault("messages.reply.info", "--file:channelset-info-response.json"));
        setReplyMessageNone(getResource().getOrSetDefault("messages.reply.none", "--file:channel-none-response.json"));
        loadFile("channel-set-response.json");
        loadFile("channelset-info-response.json");
        loadFile("channel-none-response.json");
    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction data) {
        OptionData type = new OptionData(OptionType.STRING, "type", "The type of channel to perform the action on.", false);
//        type.addChoices(
//                new Command.Choice("GLOBAL_NATIVE", "GLOBAL_NATIVE")
//                , new Command.Choice("SPECIFIC_NATIVE", "SPECIFIC_NATIVE")
//                , new Command.Choice("PERMISSION", "PERMISSION")
//                , new Command.Choice("DISCORD_TEXT", "DISCORD_TEXT")
//                , new Command.Choice("GUILD", "GUILD")
//                , new Command.Choice("PARTY", "PARTY")
//                , new Command.Choice("SPECIFIC_HANDLED", "SPECIFIC_HANDLED")
//        );
        Arrays.stream(EndPointType.values()).forEach(c -> {
            type.addChoices(new Command.Choice(c.name(), c.name()));
        });

        OptionData identifier = new OptionData(OptionType.STRING, "identifier", "The identifier of the channel to perform the action on.", false);
//        // Proxy (Global)
//        identifier.addChoices(new Command.Choice("Proxy (Global)", "proxy"));
//        // Server
//        ModuleUtils.getServerNames().forEach(s -> {
//            identifier.addChoices(new Command.Choice(s, s));
//        });
////        identifier.addChoices() // Make an other option...

        return data.addOptions(type, identifier);
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        if (! messagedString.hasCommandArgs()) {
            return messageInfo(messagedString);
        }

        if (messagedString.getCommandArgs().length < 1) {
            return messageInfo(messagedString);
        }

        EndPointType type;

        try {
            type = EndPointType.valueOf(messagedString.getCommandArgs()[0].toUpperCase());
        } catch (Exception e) {
            return messageInfo(messagedString);
        }

        if (type.equals(EndPointType.GLOBAL_NATIVE) && messagedString.getCommandArgs().length == 1) {
            EndPoint discord = new EndPoint();

            discord.setType(EndPointType.DISCORD_TEXT);
            discord.setEndPointIdentifier(messagedString.getChannel().getId());
            discord.setToFormat(StreamlineDiscord.getConfig().getDefaultFormatFromMinecraft());

            EndPoint other = new EndPoint();

            other.setType(EndPointType.GLOBAL_NATIVE);
            other.setEndPointIdentifier("");
            other.setToFormat(StreamlineDiscord.getConfig().getDefaultFormatFromDiscord());

            Route toDiscord = new Route();

            toDiscord.setInput(other);
            toDiscord.setOutput(discord);

            Route toOther = new Route();

            toOther.setInput(discord);
            toOther.setOutput(other);

            RouteLoader.registerRoute(toDiscord);
            RouteLoader.registerRoute(toOther);

            toDiscord.save();
            toOther.save();

            return messageSet(messagedString, other);
        }

        if (messagedString.getCommandArgs().length < 2) {
            return messageInfo(messagedString);
        }

        String otherFormat = type.equals(EndPointType.DISCORD_TEXT) ? StreamlineDiscord.getConfig().getDefaultFormatFromDiscord()
                : StreamlineDiscord.getConfig().getDefaultFormatFromMinecraft();
        String discordFormat = StreamlineDiscord.getConfig().getDefaultFormatFromDiscord();

        if (messagedString.getCommandArgs().length >= 3) {
            otherFormat = ModuleUtils.argsToStringMinus(messagedString.getCommandArgs(), 0, 1);
        }

        EndPoint discord = new EndPoint();

        discord.setType(EndPointType.DISCORD_TEXT);
        discord.setEndPointIdentifier(messagedString.getChannel().getId());
        discord.setToFormat(otherFormat);

        EndPoint other = new EndPoint();

        other.setType(EndPointType.GLOBAL_NATIVE);
        other.setEndPointIdentifier(messagedString.getCommandArgs()[1]);
        other.setToFormat(discordFormat);

        Route toDiscord = new Route();

        toDiscord.setInput(other);
        toDiscord.setOutput(discord);

        Route toOther = new Route();

        toOther.setInput(discord);
        toOther.setOutput(other);

        RouteLoader.registerRoute(toDiscord);
        RouteLoader.registerRoute(toOther);

        if (StreamlineDiscord.getConfig().serverEventAllEventsOnDiscordRoute()) {
            toDiscord.addEnabledEvents(EventClassifier.values());
//                    toOther.addEnabledEvents(EventClassifier.values()); // Do do this as it is not -> (to) Discord.
        }

        toDiscord.save();
        toOther.save();

        return messageSet(messagedString, other);
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageSet(MessagedString messagedString, EndPoint endPoint) {
        return message(messagedString, getReplyMessageSet(), endPoint);
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageInfo(MessagedString messagedString) {
        return message(messagedString, getReplyMessageInfo());
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageNone(MessagedString messagedString) {
        return message(messagedString, getReplyMessageNone());
    }

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String message) {
        if (isJsonFile(message)) {
            String json = getJsonFromFile(getJsonFile(message));
            return DiscordMessenger.simpleEmbed(ModuleUtils.replacePlaceholders(UserUtils.getConsole(), json
                    .replace("%this_command_label%", messagedString.getBase())
                    .replace("%this_channel_id%", messagedString.getChannel().getId())
            ));
        } else {
            return DiscordMessenger.simpleMessage(message
                    .replace("%this_command_label%", messagedString.getBase())
                    .replace("%this_channel_id%", messagedString.getChannel().getId())
            );
        }
    }

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String message, EndPoint endPoint) {
        if (isJsonFile(message)) {
            String json = getJsonFromFile(getJsonFile(message));
            return DiscordMessenger.simpleEmbed(
                    ModuleUtils.replacePlaceholders(UserUtils.getConsole(),
                            json
                                    .replace("%this_command_label%", messagedString.getBase())
                                    .replace("%this_channel_id%", messagedString.getChannel().getId())
                                    .replace("%this_type%", endPoint.getType().toString())
                                    .replace("%this_identifier%", endPoint.getEndPointIdentifier())
                                    .replace("%this_format%", endPoint.getToFormat())
                    )
            );
        } else {
            return DiscordMessenger.simpleMessage(message
                    .replace("%this_command_label%", messagedString.getBase())
                    .replace("%this_channel_id%", messagedString.getChannel().getId())
                    .replace("%this_type%", endPoint.getType().toString())
                    .replace("%this_identifier%", endPoint.getEndPointIdentifier())
                    .replace("%this_format%", endPoint.getToFormat())
            );
        }
    }

    @Override
    public void init() {

    }
}
