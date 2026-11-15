package host.plas.discord.commands;

import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.events.EventClassifier;
import host.plas.discord.messaging.BotMessageConfig;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns on every game event for the links that post into Discord, so owners need no link ids.
 * Without arguments it covers the links posting into the channel it is used in; {@code everywhere}
 * covers every link that posts into any Discord channel.
 *
 * <p>Only links whose output is a Discord channel are touched: those are the ones events are posted
 * through, while links carrying Discord chat into the game never post events.</p>
 */
public class AddAllEventsCommand extends DiscordCommand {
    public static final String SCOPE_HERE = "here";
    public static final String SCOPE_EVERYWHERE = "everywhere";

    public AddAllEventsCommand() {
        super("add_all_events",
                -1L,
                "add_all_events", "addallevents", "allevents"
        );

        setDescription("Turns on every game event for the channel links that post into Discord.");
    }

    @Override
    public void init() {

    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction action) {
        OptionData scope = new OptionData(OptionType.STRING, "scope", "Which links to change. Defaults to this channel.", false);
        scope.addChoices(
                new Command.Choice("This channel", SCOPE_HERE),
                new Command.Choice("Every channel", SCOPE_EVERYWHERE)
        );

        return action.addOptions(scope);
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        boolean everywhere = messagedString.hasCommandArgs()
                && messagedString.getCommandArgs()[0].equalsIgnoreCase(SCOPE_EVERYWHERE);

        Collection<Route> candidates = everywhere
                ? RouteLoader.getLoadedRoutes()
                : RouteLoader.getRoutesByDiscordChannel(messagedString.getChannel().getId());

        String channelId = messagedString.getChannel().getId();
        List<Route> changed = new ArrayList<>();
        for (Route route : candidates) {
            if (route.getOutput() == null || route.getOutput().getType() != EndPointType.DISCORD_TEXT) continue;
            if (! everywhere && ! channelId.equals(route.getOutput().getEndPointIdentifier())) continue;

            for (EventClassifier classifier : EventClassifier.values()) {
                route.addEnabledEvent(classifier);
            }
            route.save();
            changed.add(route);
        }

        String scopeKey = everywhere ? SCOPE_EVERYWHERE : SCOPE_HERE;
        if (changed.isEmpty()) {
            return respond("commands.add_all_events.none-" + scopeKey, messagedString);
        }

        String events = "`" + java.util.Arrays.stream(EventClassifier.values())
                .map(EventClassifier::getIdentifier)
                .collect(Collectors.joining("`, `")) + "`";
        String routes = "`" + changed.stream()
                .map(Route::getIdentifier)
                .collect(Collectors.joining("`\n`")) + "`";

        return respond("commands.add_all_events." + scopeKey, messagedString,
                "this_count", String.valueOf(changed.size()),
                "this_events", events,
                "this_routes", routes);
    }
}
