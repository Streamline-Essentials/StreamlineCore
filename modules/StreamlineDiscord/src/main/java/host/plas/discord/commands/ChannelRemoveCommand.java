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
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@Setter
@Getter
public class ChannelRemoveCommand extends DiscordCommand {
    public ChannelRemoveCommand() {
        super("channelremove",
                -1L,
                "chanrem", "chrem", "channelremove"
        );
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
        // getCommandArgs() is [""] when no argument was given.
        if (messagedString.getCommandArgs()[0].isEmpty()) {
            AtomicReference<SingleSet<MessageCreateData, BotMessageConfig>> data = new AtomicReference<>(respond("commands." + getCommandIdentifier() + ".nothing-to-remove", messagedString));

            // Both directions of the link: Discord -> game and game -> Discord.
            String channelId = messagedString.getChannel().getId();
            RouteLoader.getRoutesByDiscordChannel(channelId).forEach(route -> {
                boolean fromHere = route.getInput().getType() == EndPointType.DISCORD_TEXT && route.getInput().getEndPointIdentifier().equals(channelId);
                data.set(messageRemove(messagedString, fromHere ? route.getOutput() : route.getInput()));
                route.drop();
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
        return message(messagedString, "remove", endPoint);
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageInfo(MessagedString messagedString) {
        return message(messagedString, "info");
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageNone(MessagedString messagedString) {
        return message(messagedString, "none");
    }

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String reply) {
        return respond("commands." + getCommandIdentifier() + "." + reply, messagedString);
    }

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String reply, EndPoint endPoint) {
        return respond("commands." + getCommandIdentifier() + "." + reply, messagedString,
                "this_type", endPoint.getType().toString(),
                "this_identifier", endPoint.getEndPointIdentifier(),
                "this_format", endPoint.getToFormat());
    }

    @Override
    public void init() {

    }
}
