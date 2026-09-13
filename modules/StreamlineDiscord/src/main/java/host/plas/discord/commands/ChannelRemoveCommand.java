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
public class ChannelRemoveCommand extends DiscordCommand {
    private String replyMessageRemove;
    private String replyMessageInfo;
    private String replyMessageNone;

    public ChannelRemoveCommand() {
        super("channelremove",
                -1L,
                "chanrem", "chrem", "channelremove"
        );

        setReplyMessageRemove(getResource().getOrSetDefault("messages.reply.remove", "--file:channel-remove-response.json"));
        setReplyMessageInfo(getResource().getOrSetDefault("messages.reply.info", "--file:channelremove-info-response.json"));
        setReplyMessageNone(getResource().getOrSetDefault("messages.reply.none", "--file:channel-none-response.json"));
        loadFile("channel-remove-response.json");
        loadFile("channelremove-info-response.json");
        loadFile("channel-none-response.json");
    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction data) {
        OptionData identifier = new OptionData(OptionType.STRING, "identifier", "The identifier of the channel to perform the action on.", false);
//        // Proxy (Global)
//        identifier.addChoices(new Command.Choice("Proxy (Global)", "proxy"));
//        // Server
//        ModuleUtils.getServerNames().forEach(s -> {
//            identifier.addChoices(new Command.Choice(s, s));
//        });
////        identifier.addChoices() // Make an other option...

        return data.addOptions(identifier);
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        if (! messagedString.hasCommandArgs()) {
            return messageInfo(messagedString);
        }

        if (messagedString.getCommandArgs().length == 0) {
            AtomicReference<SingleSet<MessageCreateData, BotMessageConfig>> data = new AtomicReference<>(DiscordMessenger.simpleMessage("No channel found to remove!"));

            RouteLoader.getLoadedRoutes().forEach(route -> {
                if (route.getInput().getType().equals(EndPointType.DISCORD_TEXT) && route.getInput().getEndPointIdentifier().equals(messagedString.getChannel().getId())) {
                    data.set(messageRemove(messagedString, route.getOutput()));
                    route.drop();
                }
            });

            return data.get();
        }

        if (messagedString.getCommandArgs().length < 1) {
            return messageInfo(messagedString);
        }

        String identifier = messagedString.getCommandArgs()[0];

        ConcurrentSkipListSet<Route> routes = new ConcurrentSkipListSet<>();
        Optional<Route> optional = RouteLoader.getRoute(identifier);
        if (optional.isEmpty()) {
            return messageNone(messagedString);
        }
        Route route = optional.get();

        routes.addAll(RouteLoader.getBackAndForthRoute(route.getInput().getType(), identifier, route.getOutput().getType(), messagedString.getChannel().getId()));

        EndPoint point = route.getOutput();

        SingleSet<MessageCreateData, BotMessageConfig> data = messageRemove(messagedString, point);

        routes.forEach(Route::drop);

        return data;
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageRemove(MessagedString messagedString, EndPoint endPoint) {
        return message(messagedString, getReplyMessageRemove(), endPoint);
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
