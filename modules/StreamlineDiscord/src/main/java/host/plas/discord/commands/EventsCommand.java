package host.plas.discord.commands;

import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Setter
@Getter
public class EventsCommand extends DiscordCommand {
    private String replyMessageAdd;
    private String replyMessageRemove;
    private String replyMessageClear;
    private String replyMessageInfo;
    private String replyMessageNone;

    public EventsCommand() {
        super("events",
                -1L,
                "event", "ev", "e", "events"
        );

        setReplyMessageAdd(getResource().getOrSetDefault("messages.reply.add", "--file:event-add-response.json"));
        setReplyMessageRemove(getResource().getOrSetDefault("messages.reply.remove", "--file:event-remove-response.json"));
        setReplyMessageRemove(getResource().getOrSetDefault("messages.reply.clear", "--file:event-clear-response.json"));
        setReplyMessageInfo(getResource().getOrSetDefault("messages.reply.info", "--file:event-info-response.json"));
        setReplyMessageNone(getResource().getOrSetDefault("messages.reply.none", "--file:event-none-response.json"));
        loadFile("event-add-response.json");
        loadFile("event-remove-response.json");
        loadFile("event-clear-response.json");
        loadFile("event-info-response.json");
        loadFile("event-none-response.json");
    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction data) {
        OptionData action = new OptionData(OptionType.STRING, "action", "The action to perform.", true);
        action.addChoices(
                new Command.Choice("Add Event", "add"),
                new Command.Choice("Remove Event", "remove"),
                new Command.Choice("Clear All", "clear")
        );

        OptionData type = new OptionData(OptionType.STRING, "type", "The type of event to perform the action on.", false);
        Arrays.stream(EventClassifier.values()).forEach(e -> {
            type.addChoices(new Command.Choice(e.name(), e.name()));
        });
        type.addChoices(new Command.Choice("All", "all"));

        OptionData identifier = new OptionData(OptionType.STRING, "identifier", "The identifier of the channel to perform the action on.", false);

        return data.addOptions(action, type, identifier);
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        if (! messagedString.hasCommandArgs()) {
            return messageInfo(messagedString);
        }

        String action = messagedString.getCommandArgs()[0].toLowerCase();
        switch (action) {
            case "add":
                if (messagedString.getCommandArgs().length < 2) {
                    return messageInfo(messagedString);
                }

                if (messagedString.getCommandArgs().length < 3 ||
                        messagedString.getCommandArgs()[2].isBlank() || messagedString.getCommandArgs()[2].equals("?")) {
                    return messageInfo(messagedString);
                }

                String identifierAdd = messagedString.getCommandArgs()[2];

                Optional<Route> routeAdd = RouteLoader.getRoutesByDiscordChannel(messagedString.getChannel().getId()).stream().filter(r -> {
                    return Objects.equals(r.getIdentifier(), identifierAdd);
                }).findFirst();

                if (routeAdd.isEmpty()) {
                    return messageNone(messagedString);
                }

                ConcurrentSkipListSet<EventClassifier> typesAdd = new ConcurrentSkipListSet<>();
                if (! messagedString.getCommandArgs()[1].equals("all")) {
                    try {
                        typesAdd.add(EventClassifier.valueOf(messagedString.getCommandArgs()[1].toUpperCase()));
                    } catch (Exception e) {
                        return messageInfo(messagedString);
                    }
                } else {
                    typesAdd.addAll(Arrays.asList(EventClassifier.values()));
                }

                for (EventClassifier type : typesAdd) {
                    Route r = routeAdd.get();
                    r.addEnabledEvent(type);
                }

                routeAdd.ifPresent(Route::save);

                return messageAdd(messagedString, routeAdd.get(), typesAdd);
            case "remove":
                if (messagedString.getCommandArgs().length < 2) {
                    return messageInfo(messagedString);
                }

                if (messagedString.getCommandArgs().length < 3 ||
                        messagedString.getCommandArgs()[2].isBlank() || messagedString.getCommandArgs()[2].equals("?")) {
                    return messageInfo(messagedString);
                }

                String identifierRemove = messagedString.getCommandArgs()[2];

                Optional<Route> routeRemove = RouteLoader.getRoutesByDiscordChannel(messagedString.getChannel().getId()).stream().filter(r -> {
                    return Objects.equals(r.getIdentifier(), identifierRemove);
                }).findFirst();

                if (routeRemove.isEmpty()) {
                    return messageNone(messagedString);
                }

                ConcurrentSkipListSet<EventClassifier> typesRemove = new ConcurrentSkipListSet<>();
                if (! messagedString.getCommandArgs()[1].equals("all")) {
                    try {
                        typesRemove.add(EventClassifier.valueOf(messagedString.getCommandArgs()[1].toUpperCase()));
                    } catch (Exception e) {
                        return messageInfo(messagedString);
                    }
                } else {
                    typesRemove.addAll(Arrays.asList(EventClassifier.values()));
                }

                for (EventClassifier type : typesRemove) {
                    Route r = routeRemove.get();
                    r.removeEnabledEvent(type);
                }

                routeRemove.ifPresent(Route::save);

                return messageRemove(messagedString, routeRemove.get(), typesRemove);
            case "clear":
                if (messagedString.getCommandArgs().length < 3 ||
                        messagedString.getCommandArgs()[1].isBlank() || messagedString.getCommandArgs()[1].equals("?")) {
                    return messageInfo(messagedString);
                }

                String identifierClear = messagedString.getCommandArgs()[2]; // Not needed

                Optional<Route> routeClear = RouteLoader.getRoutesByDiscordChannel(messagedString.getChannel().getId()).stream().filter(r -> {
                    return Objects.equals(r.getIdentifier(), identifierClear);
                }).findFirst();

                if (routeClear.isEmpty()) {
                    return messageInfo(messagedString);
                }

                Route r = routeClear.get();
                r.clearEnabledEvents();
                r.save();

                return messageClear(messagedString, r);
            default:
                return messageInfo(messagedString);
        }
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageAdd(MessagedString messagedString, Route route, ConcurrentSkipListSet<EventClassifier> classifiers) {
        return message(messagedString, getReplyMessageAdd(), route, classifiers);
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageRemove(MessagedString messagedString, Route route, ConcurrentSkipListSet<EventClassifier> classifiers) {
        return message(messagedString, getReplyMessageRemove(), route, classifiers);
    }

    public SingleSet<MessageCreateData, BotMessageConfig> messageClear(MessagedString messagedString, Route route) {
        return message(messagedString, getReplyMessageClear(), route, new ConcurrentSkipListSet<>());
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

    public SingleSet<MessageCreateData, BotMessageConfig> message(MessagedString messagedString, String message, Route route, ConcurrentSkipListSet<EventClassifier> classifiers) {
        if (isJsonFile(message)) {
            String json = getJsonFromFile(getJsonFile(message));
            return DiscordMessenger.simpleEmbed(
                    ModuleUtils.replacePlaceholders(UserUtils.getConsole(),
                            json
                                    .replace("%this_command_label%", messagedString.getBase())
                                    .replace("%this_channel_id%", messagedString.getChannel().getId())
                                    .replace("%this_identifier%", route.getIdentifier())
                                    .replace("%this_type%", specialClassifiers(classifiers))
                                    .replace("%this_type_named%", specialClassifiersEnum(classifiers))
                    )
            );
        } else {
            return DiscordMessenger.simpleMessage(message
                    .replace("%this_command_label%", messagedString.getBase())
                    .replace("%this_channel_id%", messagedString.getChannel().getId())
                    .replace("%this_identifier%", route.getIdentifier())
                    .replace("%this_type%", specialClassifiers(classifiers))
                    .replace("%this_type_named%", specialClassifiersEnum(classifiers))
            );
        }
    }

    public boolean includeAll() {
        return false;
    }

    public String specialClassifiers(Collection<EventClassifier> classifiers) {
        if (classifiers == null || classifiers.isEmpty()) return "\nNone";

        return "\n" + (includeAll() ? (classifiers.containsAll(Arrays.asList(EventClassifier.values())) ? "All" : specialConcatClassifiers(classifiers)) : specialConcatClassifiers(classifiers));
    }

    public String specialClassifiersEnum(Collection<EventClassifier> classifiers) {
        if (classifiers == null || classifiers.isEmpty()) return "\nNone";

        return "\n" + (includeAll() ? (classifiers.containsAll(Arrays.asList(EventClassifier.values())) ? "All" : specialConcatEnum(classifiers)) : specialConcatEnum(classifiers));
    }

    public <I extends EventClassifier> String specialConcatClassifiers(Collection<I> collection) {
        String before = collection.stream().map(EventClassifier::getIdentifier).collect(Collectors.joining("`\n`"));
        if (before.isBlank()) return before;
        return "`" + before + "`";
    }

    @Override
    public void init() {

    }
}
