package host.plas.discord.commands;

import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.events.EventClassifier;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Setter
@Getter
public class ChannelsCommand extends DiscordCommand {
    private String replyMessageList;
    private String replyMessageListHere;

    public ChannelsCommand() {
        super("channels",
                -1L,
                "chs", "channels"
        );

        setReplyMessageList(getResource().getOrSetDefault("messages.reply.list", "--file:channel-list-response.json"));
        setReplyMessageListHere(getResource().getOrSetDefault("messages.reply.list-here", "--file:channel-list-here-response.json"));
        loadFile("channel-list-response.json");
        loadFile("channel-list-here-response.json");
    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction data) {
        OptionData here = new OptionData(OptionType.BOOLEAN, "here", "Just show here?", false);

        return data.addOptions(here);
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        boolean here = true;
        
        if (messagedString.getCommandArgs().length == 0) {
            here = false;
        } else {
            String arg = messagedString.getCommandArgs()[0].toLowerCase();
            here = Boolean.parseBoolean(arg);
        }
        
        if (here) {
            return messageInfoHere(messagedString);
        } else {
            return messageInfo(messagedString);
        }
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageInfo(MessagedString messagedString) {
        return message(messagedString, getReplyMessageList());
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageInfoHere(MessagedString messagedString) {
        return message(messagedString, getReplyMessageListHere());
    }

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String message) {
        if (isJsonFile(message)) {
            String json = getJsonFromFile(getJsonFile(message));
            return DiscordMessenger.simpleEmbed(ModuleUtils.replacePlaceholders(UserUtils.getConsole(), json
                    .replace("%this_command_label%", messagedString.getBase())
                    .replace("%this_channel_id%", messagedString.getChannel().getId())
                    .replace("%this_list_all%", getRoutesList())
                    .replace("%this_list_here%", getRoutesListHere(messagedString))
            ));
        } else {
            return DiscordMessenger.simpleMessage(message
                    .replace("%this_command_label%", messagedString.getBase())
                    .replace("%this_channel_id%", messagedString.getChannel().getId())
                    .replace("%this_list_all%", getRoutesList())
                    .replace("%this_list_here%", getRoutesListHere(messagedString))
            );
        }
    }

    public static ConcurrentSkipListSet<Route> getAllRoutes() {
        return RouteLoader.getLoadedRoutes();
    }

    public static ConcurrentSkipListSet<Route> getRoutesForChannel(String channelId) {
        return RouteLoader.getRoutesByDiscordChannel(channelId);
    }

    public String getRoutesList() {
        return specialConcatRoutes(getAllRoutes());
    }

    public String getRoutesListHere(MessagedString ctx) {
        String channelId = ctx.getChannel().getId();

        return specialConcatRoutes(getRoutesForChannel(channelId));
    }

    public <I extends Route> String specialConcatRoutes(Collection<I> collection) {
        String before = collection.stream().map(r -> {
            String identifier = r.getIdentifier();

            EndPoint input = r.getInput();
            EndPoint output = r.getOutput();

            StringBuilder sb = new StringBuilder();

            sb.append(identifier).append("`: ").append("`").append(input.getType().name()).append("` -> `").append(output.getType().name())/*.append("`")*/;

            return sb.toString();
        }).collect(Collectors.joining("`\n`"));
        if (before.isBlank()) return before;
        return "`" + before + "`";
    }

    @Override
    public void init() {

    }
}
