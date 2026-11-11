package host.plas.discord.data.channeling;

import host.plas.config.VerifiedUsers;
import host.plas.database.DiscordMiddleware;
import host.plas.discord.data.events.EventClassifier;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordHandler;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.BotMessages;
import host.plas.discord.messaging.DiscordMessenger;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;
import host.plas.events.streamline.channels.ChanneledMessageEvent;
import singularity.data.console.CosmicSender;
import singularity.loading.Loadable;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter @Setter
public class Route implements Loadable<Route> {
    @Getter
    private static final File oldDataFolder = new File(StreamlineDiscord.getInstance().getDataFolder(), "routes" + File.separator);

    private String identifier;

    private EndPoint input;
    private EndPoint output;

    private ConcurrentSkipListSet<EventClassifier> enabledEvents;

    private boolean fullyLoaded = false;

    public Route(String identifier) {
        this.identifier = identifier;

        this.enabledEvents = new ConcurrentSkipListSet<>();
    }

    public Route() {
        this(UUID.randomUUID().toString());
    }

    public String getEnabledEventsAsString() {
        return getEnabledEvents().stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public void setEnabledEventsFromString(String s) {
        ConcurrentSkipListSet<EventClassifier> set = new ConcurrentSkipListSet<>();
        if (s == null || s.isBlank()) {
            setEnabledEvents(set);
            return;
        }
        String[] split = s.split(",");
        for (String str : split) {
            try {
                EventClassifier classifier = EventClassifier.valueOf(str);
                set.add(classifier);
            } catch (IllegalArgumentException e) {
                StreamlineDiscord.getInstance().logWarning("Invalid event classifier in route " + getIdentifier() + ": " + str);
            }
        }
        setEnabledEvents(set);
    }

    public void addEnabledEvent(EventClassifier classifier) {
        enabledEvents.add(classifier);
    }

    public void removeEnabledEvent(EventClassifier classifier) {
        enabledEvents.remove(classifier);
    }

    public void addEnabledEvents(EventClassifier... classifiers) {
        enabledEvents.addAll(Arrays.asList(classifiers));
    }

    public void removeEnabledEvents(EventClassifier... classifiers) {
        Arrays.asList(classifiers).forEach(enabledEvents::remove);
    }

    public boolean hasEnabledEvent(EventClassifier classifier) {
        return enabledEvents.contains(classifier);
    }

    public void clearEnabledEvents() {
        enabledEvents.clear();
    }

    @Override
    public void save(boolean async) {
        DiscordMiddleware.saveRoute(this);
    }

    @Override
    public void load() {
        RouteLoader.registerRoute(this);
    }

    @Override
    public void unload() {
        RouteLoader.unregisterRoute(this);
    }

    @Override
    public boolean isLoaded() {
        return RouteLoader.getRoute(this.getIdentifier()).isPresent();
    }

    @Override
    public void saveAndUnload(boolean b) {
        save(b);
        unload();
    }

    public void bounceMessage(RoutedUser routedUser, String message) {
        ModuleUtils.fireEvent(new ChanneledMessageEvent(message, getInput(), getOutput()));

        switch (getOutput().getType()) {
            case GLOBAL_NATIVE:
                UserUtils.getOnlineSenders().forEach((s, user) -> {
                    ModuleUtils.sendMessage(user, getMutatedMessage(routedUser, message, getOutput()));
                });
                break;
            case SPECIFIC_NATIVE:
                ModuleUtils.getOnlineUsers().forEach((s, user) -> {
                    if (user.getServer() != null) {
                        if (user.getServerName().equals(getOutput().getEndPointIdentifier())) {
                            ModuleUtils.sendMessage(user, getMutatedMessage(routedUser, message, getOutput()));
                        }
                    }
                });
                break;
            case PERMISSION:
                UserUtils.getOnlineSenders().forEach((s, streamlineUser) -> {
                    if (ModuleUtils.hasPermission(streamlineUser, getOutput().getEndPointIdentifier())) ModuleUtils.sendMessage(streamlineUser,
                            getMutatedMessage(routedUser, message, getOutput()));
                });
                break;

//            case GUILD:
//                if (! StreamlineDiscord.getConfig().allowDiscordToStreamlineGuilds()) return;
//                if (StreamlineDiscord.getGroupsDependency().isPresent()) {
//                    StreamlineDiscord.getGroupsDependency().getGuildMembersOf(getOutput().getIdentifier()).forEach((s, user) -> {
//                        ModuleUtils.sendMessage(user, getMutatedMessage(routedUser, message, getOutput()));
//                    });
//                }
//                break;
            case PARTY:
                if (! StreamlineDiscord.getConfig().allowDiscordToStreamlineParties()) return;
                if (StreamlineDiscord.getGroupsDependency().isPresent()) {
                    StreamlineDiscord.getGroupsDependency().getPartyMembersOf(getOutput().getEndPointIdentifier()).forEach((s, user) -> {
                        ModuleUtils.sendMessage(user, getMutatedMessage(routedUser, message, getOutput()));
                    });
                }
                break;

            case SPECIFIC_HANDLED:
                if (! StreamlineDiscord.getConfig().allowDiscordToStreamlineChannels()) return;
                if (StreamlineDiscord.getMessagingDependency().isPresent()) {
                    StreamlineDiscord.getMessagingDependency().getUsersInChannel(getOutput().getIdentifier()).forEach((s, user) -> {
                        ModuleUtils.sendMessage(user, getMutatedMessage(routedUser, message, getOutput()));
                    });
                }
                break;

            case DISCORD_TEXT:
                TextChannel channel = getOutput().asServerTextChannel();
                if (channel == null) return;

                // A format naming an embed ('--file:<name>.json') sends that file from embeds/.
                if (getOutput().getToFormat() != null && getOutput().getToFormat().trim().startsWith("--file:")) {
                    SingleSet<MessageCreateData, BotMessageConfig> embed = BotMessages.buildEmbed(getOutput().getToFormat(), routedUser.getUser(),
                            BotMessages.placeholders("this_message", message));
                    if (embed != null) DiscordMessenger.message(channel.getIdLong(), embed.getKey(), embed.getValue());
                } else {
                    DiscordMessenger.sendSimpleMessage(channel.getIdLong(), ModuleUtils.stripColor(
                            ModuleUtils.replaceAllPlayerBungee(routedUser.getUser(), getOutput().getToFormat().replace("%this_message%", message))));
                }
                break;
        }
    }


    /**
     * Sends the bot message {@code messageKey} (see {@link BotMessages}) about {@code routedUser} to this route's
     * output when it is a Discord channel; other outputs ignore game events. Nothing is sent when the owner
     * disabled the message in {@code messaging.yml}.
     *
     * @param extra {@code %name%} placeholders for the message, keyed by {@code name}; may be null
     */
    public void bounceEvent(RoutedUser routedUser, String messageKey, Map<String, String> extra) {
        submitEvent(routedUser, messageKey, extra);
    }

    /**
     * {@link #bounceEvent} that hands back the Discord request, completing once Discord accepted the message,
     * or at once when nothing is sent (output not a Discord channel, message disabled, bot not connected).
     */
    public CompletableFuture<?> submitEvent(RoutedUser routedUser, String messageKey, Map<String, String> extra) {
        CompletableFuture<?> none = CompletableFuture.completedFuture(null);

        ModuleUtils.fireEvent(new ChanneledMessageEvent(BotMessages.getText(messageKey), getInput(), getOutput()));

        if (getOutput() == null || getOutput().getType() != EndPointType.DISCORD_TEXT) return none;
        // Channel lookups wait for the bot to be ready; a game event must never wait on Discord.
        if (! DiscordHandler.isConnected()) return none;

        TextChannel channel = getOutput().asServerTextChannel();
        if (channel == null) return none;

        SingleSet<MessageCreateData, BotMessageConfig> data = BotMessages.build(messageKey, routedUser.getUser(), extra);
        if (data == null) return none;

        CompletableFuture<?> sent = DiscordMessenger.submit(channel.getIdLong(), data.getKey());
        if (data.getValue().isAvatarChange()) {
            sent = sent.whenComplete((ignored, error) -> DiscordHandler.updateBotAvatar(data.getValue().getChangeAfterMessage()));
        }
        return sent;
    }

    /** Sends {@code message} on, or, with {@code forceEmbed}, sends it as embed JSON to this route's Discord channel. */
    public void bounceMessage(RoutedUser routedUser, String message, boolean forceEmbed) {
        if (! forceEmbed) {
            bounceMessage(routedUser, message);
            return;
        }

        TextChannel channel = getOutput().asServerTextChannel();
        if (channel == null) return;

        try {
            DiscordMessenger.sendSimpleEmbed(channel.getIdLong(), ModuleUtils.stripColor(
                    ModuleUtils.replaceAllPlayerBungee(routedUser.getUser(), message)));
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Could not send a forwarded embed to route " + getIdentifier() + ": " + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route o = (Route) obj;
            return o.getInput().equals(getInput()) && o.getOutput().equals(getOutput());
        } else {
            return super.equals(obj);
        }
    }

    public String getMutatedMessage(RoutedUser user, String message, EndPoint endPoint) {
        if (! user.isDiscord()) return endPoint.getToFormat().replace("%this_message%", message);
        if (! VerifiedUsers.isVerified(user.getDiscordId())) return endPoint.getToFormat()
                .replace("%streamline_user_absolute%",
                        DiscordHandler.getTag(DiscordHandler.getUser(user.getDiscordId())))
                .replace("%streamline_user_formatted%", DiscordHandler.getUser(user.getDiscordId()).getName())
                .replace("%this_message%", message);
        String s = VerifiedUsers.getUUIDfromDiscordID(user.getDiscordId()).orElse(null);
        if (s == null) return endPoint.getToFormat()
                .replace("%streamline_user_absolute%",
                        DiscordHandler.getTag(DiscordHandler.getUser(user.getDiscordId())))
                .replace("%streamline_user_formatted%", DiscordHandler.getUser(user.getDiscordId()).getName())
                .replace("%this_message%", message);
        CosmicSender u = ModuleUtils.getOrCreateSender(s).orElse(null);
        if (u == null) return endPoint.getToFormat()
                .replace("%streamline_user_absolute%",
                        DiscordHandler.getTag(DiscordHandler.getUser(user.getDiscordId())))
                .replace("%streamline_user_formatted%", DiscordHandler.getUser(user.getDiscordId()).getName())
                .replace("%this_message%", message);

        return ModuleUtils.replaceAllPlayerBungee(u, endPoint.getToFormat()).replace("%this_message%", message);
    }

    public void drop() {
        // Taken out of the loader directly: Loader.unload saves on the way out, which
        // would queue the route straight back into the database.
        RouteLoader.getLoadedRoutes().remove(this);
        DiscordMiddleware.dropRoute(this);

        // A channel link is two routes over the same pair of endpoints, so an endpoint
        // goes only once no remaining route uses it.
        dropIfUnused(this.input);
        dropIfUnused(this.output);

        this.input = null;
        this.output = null;
        this.enabledEvents.clear();
    }

    private static void dropIfUnused(EndPoint endPoint) {
        if (endPoint == null) return;

        boolean used = RouteLoader.getLoadedRoutes().stream().anyMatch(route -> uses(route.getInput(), endPoint) || uses(route.getOutput(), endPoint));
        if (! used) endPoint.drop();
    }

    private static boolean uses(EndPoint candidate, EndPoint endPoint) {
        return candidate != null && candidate.getIdentifier().equals(endPoint.getIdentifier());
    }


    @Override
    public Route augment(CompletableFuture<Optional<Route>> completableFuture, boolean isGet) {
        this.fullyLoaded = false;

        completableFuture.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                StreamlineDiscord.getInstance().logWarning("An error occurred while loading the route: " + this.getIdentifier());
                StreamlineDiscord.getInstance().logWarning(throwable.getStackTrace());
                return;
            }

            if (optional.isPresent()) {
                Route route = optional.get();

                this.setIdentifier(route.getIdentifier());
                this.setInput(route.getInput());
                this.setOutput(route.getOutput());
                this.setEnabledEvents(route.getEnabledEvents());
            } else {
                if (! isGet) {
                    save();
                }
            }

            this.fullyLoaded = true;
        });

        return this;
    }
}
