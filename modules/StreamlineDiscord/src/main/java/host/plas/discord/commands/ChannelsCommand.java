package host.plas.discord.commands;

import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.events.EventClassifier;
import host.plas.discord.messaging.BotMessageConfig;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Setter
@Getter
public class ChannelsCommand extends DiscordCommand {
    public ChannelsCommand() {
        super("channels",
                -1L,
                "chs", "channels"
        );
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
        return message(messagedString, "list");
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageInfoHere(MessagedString messagedString) {
        return message(messagedString, "list-here");
    }

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String reply) {
        return respond("commands." + getCommandIdentifier() + "." + reply, messagedString,
                "this_list_all", getRoutesList(),
                "this_list_here", getRoutesListHere(messagedString));
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
