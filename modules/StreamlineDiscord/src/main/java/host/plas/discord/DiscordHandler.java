package host.plas.discord;

import gg.drak.thebase.async.AsyncUtils;
import host.plas.config.VerifiedUsers;
import host.plas.discord.data.BotLayout;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.verified.VerifiedUser;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.streamline.api.SLAPI;
import host.plas.StreamlineDiscord;
import host.plas.discord.commands.*;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import host.plas.discord.voice.StreamlineVoiceInterceptor;
import host.plas.events.streamline.bot.BotStoppedEvent;
import host.plas.events.streamline.verification.on.VerificationAlreadyVerifiedEvent;
import host.plas.events.streamline.verification.on.VerificationFailureEvent;
import singularity.data.console.CosmicSender;
import singularity.interfaces.ISingularityExtension;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordHandler {

    @Getter
    private static final File forwardedJsonsFolder = new File(StreamlineDiscord.getInstance().getDataFolder(), "forwarded-jsons" + File.separator);

    @Getter @Setter
    private static AtomicReference<JDA> concurrentDiscordAPI;

    public static JDA getDiscordAPI() {
        if (getConcurrentDiscordAPI() == null) return null;
        return getConcurrentDiscordAPI().get();
    }

    public static void setDiscordAPI(JDA value) {
        if (getConcurrentDiscordAPI() == null) {
            setConcurrentDiscordAPI(new AtomicReference<>(value));
            return;
        }
        getConcurrentDiscordAPI().set(value);
    }

    @NonNull
    public static JDA safeDiscordAPI() {
        JDA api = getDiscordAPI();
        if (api == null) {
            return Objects.requireNonNull(null, "The Concurrent DiscordAPI is 'null'!");
        } else {
            try {
                return Objects.requireNonNull(api.awaitReady(), "The Concurrent DiscordAPI is 'null'!");
            } catch (Exception e) {
                StreamlineDiscord.getInstance().logWarning("Error while waiting for the Discord API to be ready: " + e.getMessage());
                StreamlineDiscord.getInstance().logWarning(e.getStackTrace());
                return api;
            }
        }
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<String, DiscordCommand> registeredCommands = new ConcurrentSkipListMap<>();

    public static long getBotId() {
        return getBotUser().getIdLong();
    }

    public static User getBotUser() {
        return safeDiscordAPI().getSelfUser();
    }

    public static long getUserId(User user) {
        return user.getIdLong();
    }

    public static User getUser(long userId) {
        return safeDiscordAPI().getUserById(userId);
    }

    public static void updateBotAvatar(String url) {
//        try {
//            if (safeDiscordAPI().getStatus() == JDA.Status.CONNECTED) {
//                safeDiscordAPI().getSelfUser().getManager().setAvatar(Icon.from(new URL(url).openStream()));
//            } else {
//                DiscordModule.getInstance().logWarning("Couldn't change the bot's avatar due to the bot not being connected.");
//            }
//        } catch (Exception e) {
//            DiscordModule.getInstance().logWarning("Couldn't change the bot's avatar due to...");
//            DiscordModule.getInstance().logWarning(e.getStackTrace());
//        }
    }

    public static ConcurrentSkipListMap<Long, Guild> getJoinedServers() {
        ConcurrentSkipListMap<Long, Guild> r = new ConcurrentSkipListMap<>();

        for (Guild server : DiscordHandler.safeDiscordAPI().getSelfUser().getMutualGuilds()) {
            r.put(server.getIdLong(), server);
        }

        return r;
    }

    public static Guild getServerById(long id) {
        return getJoinedServers().get(id);
    }

    public static TextChannel getTextChannelById(long id) {
        return safeDiscordAPI().getTextChannelById(id);
    }

    public static VoiceChannel getVoiceChannelById(long id) {
        return safeDiscordAPI().getVoiceChannelById(id);
    }

    public static void registerCommands() {
        new ChannelCommand();
        new ChannelRemoveCommand();
        new ChannelSetCommand();
        new ChannelsCommand();
        new EventsCommand();
        new HelpCommand();
        new PingCommand();
        new ReloadCommand();
        new RestartCommand();
        new UnVerifyCommand();
        new VerifyCommand();
    }

    public static CompletableFuture<Boolean> init() {
        getForwardedJsonsFolder().mkdirs();

        return CompletableFuture.supplyAsync(() -> {
            kill().join();

            if (! StreamlineDiscord.getConfig().fullDisable()) {
                StreamlineDiscord.getInstance().logInfo("Bot is initializing...!");

                BotLayout layout = StreamlineDiscord.getConfig().getBotLayout();
                try {
                    JDA jda = JDABuilder.createDefault(layout.getToken(), List.of(GatewayIntent.values()))
                            .setMemberCachePolicy(MemberCachePolicy.ALL)
                            .setVoiceDispatchInterceptor(new StreamlineVoiceInterceptor())
                            .setActivity(Activity.of(layout.getActivityType(), layout.getActivityValue()))
                            .addEventListeners(new DiscordListener())
                            .build();
                    jda = jda.awaitReady();
                    setDiscordAPI(jda);

                    StreamlineDiscord.getInstance().logInfo("Bot is ready!");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    updateBotAvatar(layout.getAvatarUrl());

                    StreamlineDiscord.getInstance().logInfo("Registering Discord commands...");
                    registerCommands();
                    StreamlineDiscord.getInstance().logInfo("Registered Discord commands!");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (getDiscordAPI() != null) StreamlineDiscord.getInstance().logInfo("Bot Initialized!");
            }

            return true;
        });
    }

    public static CompletableFuture<Boolean> kill() {
        return CompletableFuture.supplyAsync(() -> {
            if (getDiscordAPI() == null) return false;

            getRegisteredCommands().forEach((s, command) -> {
                command.unregister();
            });

            if (! StreamlineDiscord.getConfig().moduleForwardsEventsToProxy()) {
                safeDiscordAPI().shutdownNow();
            }

            setConcurrentDiscordAPI(null);

            new BotStoppedEvent().fire();

            return true;
        });
    }

    public static void awaitReady() {
        awaitReady(20000);
    }

    public static void awaitReady(long timeoutMillis) {
        AsyncUtils.supplyAsync(() -> {
            JDA api = getDiscordAPI();
            while (api == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                api = getDiscordAPI();
            }

            while (! api.getStatus().equals(JDA.Status.CONNECTED)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Wait an extra 10 ms to make sure it is fully ready.
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }).completeOnTimeout(null, timeoutMillis, TimeUnit.MILLISECONDS)
                .join();
    }

    public static void registerCommand(DiscordCommand command) {
        getRegisteredCommands().put(command.getCommandIdentifier(), command);
        StreamlineDiscord.getInstance().logDebug("Registered DiscordCommand '" + command.getCommandIdentifier() + "'.");
    }

    public static void unregisterCommand(String identifier) {
        getRegisteredCommands().remove(identifier);
        StreamlineDiscord.getInstance().logDebug("Unregistered DiscordCommand '" + identifier + "'.");
    }

    public static boolean isRegistered(String identifier) {
        return getRegisteredCommands().containsKey(identifier);
    }

    public static boolean isRegisteredByAlias(String alias) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        getRegisteredCommands().forEach((s, command) -> {
            if (command.getAliases().contains(alias)) atomicBoolean.set(true);
        });

        return atomicBoolean.get();
    }

    public static DiscordCommand getCommandByAlias(String alias) {
        AtomicReference<DiscordCommand> atomicCommand = new AtomicReference<>(null);

        getRegisteredCommands().forEach((s, command) -> {
            if (command.getAliases().contains(alias)) atomicCommand.set(command);
        });

        return atomicCommand.get();
    }

    @Getter
    private static final File discordCommandMainFolder = new File(StreamlineDiscord.getInstance().getDataFolder(), "discord-commands" + File.separator);

    public static File getDiscordCommandFolder(String commandIdentifier) {
        return new File(getDiscordCommandMainFolder(), commandIdentifier + File.separator);
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<String, String> pendingVerifications = new ConcurrentSkipListMap<>();

    public static String getOrGetVerification(CosmicSender user) {
        return getOrGetVerification(user.getUuid());
    }

    public static String getOrGetVerification(String uuid) {
        String r = getPendingVerifications().get(uuid);
        if (r != null) return r;
        r = createVerification();
        getPendingVerifications().put(uuid, r);
        return r;
    }

    public static String createVerification() {
        String uuid = UUID.randomUUID().toString();
        String r = uuid.substring(uuid.lastIndexOf("-") + 8);
        if (hasVerification(r)) r = createVerification();
        return r;
    }

    public static boolean hasVerification(String verification) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        getPendingVerifications().forEach((uuid, s) -> {
            if (s.equals(verification)) atomicBoolean.set(true);
        });

        return atomicBoolean.get();
    }

    public static Optional<CosmicSender> getPendingVerificationUser(String verification) {
        AtomicReference<Optional<CosmicSender>> atomicUser = new AtomicReference<>(Optional.empty());

        getPendingVerifications().forEach((uuid, s) -> {
            if (s.equals(verification)) atomicUser.set(UserUtils.getOrGetSender(uuid));
        });

        return atomicUser.get();
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> tryVerificationForUser(MessagedString messagedString, String verification, boolean fromCommand) {
        Optional<VerifiedUser> optional = VerifiedUsers.getById(messagedString.getAuthor().getIdLong());

        if (optional.isPresent()) {
            new VerificationAlreadyVerifiedEvent(fromCommand, null, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().verifiedFailureAlreadyVerifiedDiscord());
        }
        if (! hasVerification(verification)) {
            new VerificationFailureEvent(fromCommand, null, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());
        }
        CosmicSender user = getPendingVerificationUser(verification).orElse(null);
        if (user == null) {
            new VerificationFailureEvent(fromCommand, null, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());
        }
        SingleSet<MessageCreateData, BotMessageConfig> data = VerifiedUsers.verifyUser(user.getUuid(), messagedString, verification, fromCommand);
        getPendingVerifications().remove(user.getUuid());
        return data;
    }

    public static boolean isBackEnd() {
        return SLAPI.getInstance().getPlatform().getServerType().equals(ISingularityExtension.ServerType.BACKEND);
    }

    public static ConcurrentSkipListSet<Route> getAllCurrentRoutes(CosmicSender player) {
        ConcurrentSkipListSet<Route> routes = new ConcurrentSkipListSet<>();

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (route.getInput().getType() == EndPointType.GLOBAL_NATIVE) routes.add(route);
            if (route.getInput().getType() == EndPointType.SPECIFIC_NATIVE) {
                if (route.getInput().getEndPointIdentifier().equals(player.getServerName())) routes.add(route);
            }
            if (route.getInput().getType() == EndPointType.GUILD) {
//                if (DiscordModule.getGroupsDependency().isPresent()) {
//                    if (DiscordModule.getGroupsDependency().getGuildMembersOf(route.getInput().getIdentifier()).containsKey(player.getUuid()))
//                        routes.add(route);
//                }
            }
            if (route.getInput().getType() == EndPointType.PARTY) {
//                if (DiscordModule.getGroupsDependency().isPresent()) {
//                    if (DiscordModule.getGroupsDependency().getPartyMembersOf(route.getInput().getIdentifier()).containsKey(player.getUuid()))
//                        routes.add(route);
//                }
            }
        });

        return routes;
    }

    public static ConcurrentSkipListSet<String> allEndPointTypesAsStrings() {
        ConcurrentSkipListSet<String> strings = new ConcurrentSkipListSet<>();
        for (EndPointType type : EndPointType.values()) strings.add(type.toString());
        return strings;
    }

    public static ConcurrentSkipListMap<Long, Command> retrieveCommands() {
        ConcurrentSkipListMap<Long, Command> commands = new ConcurrentSkipListMap<>();

        JDA jda = getDiscordAPI();
        if (jda == null) return commands;

        jda.retrieveCommands().complete().forEach(command -> {
            commands.put(command.getIdLong(), command);
        });

        return commands;
    }

    public static boolean hasCommand(String identifier) {
        return retrieveCommands().values().stream().anyMatch(command -> command.getName().equals(identifier));
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<DiscordCommand, Long> registeredSlashCommands = new ConcurrentSkipListMap<>();

    public static CompletableFuture<Command> registerSlashCommand(DiscordCommand discordCommand) {
        if (getRegisteredSlashCommands().containsValue(discordCommand.getSlashCommandSnowflake())) {
            unregisterSlashCommand(discordCommand);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Command command = discordCommand.setupOptionData(safeDiscordAPI().upsertCommand(discordCommand.getCommandIdentifier(), discordCommand.getDescription())).complete();

                getRegisteredSlashCommands().put(discordCommand, command.getIdLong());
                discordCommand.setSlashCommandSnowflake(command.getIdLong());

                StreamlineDiscord.getInstance().logInfo("Registered &cDiscordCommand &rwith identifier '&d" + discordCommand.getCommandIdentifier() + "&r' and snowflake '&d" + command.getIdLong() + "&r'.");

                return command;
            } catch (Exception e) {
                StreamlineDiscord.getInstance().logWarning("Error registering slash command: " + discordCommand.getCommandIdentifier() + " - " + e.getMessage());
                StreamlineDiscord.getInstance().logWarning(e.getStackTrace());

                return null;
            }
        });
    }

    public static void unregisterSlashCommand(DiscordCommand discordCommand) {
        if (! getRegisteredSlashCommands().containsValue(discordCommand.getSlashCommandSnowflake())) return;

        CompletableFuture.runAsync(() -> {
            safeDiscordAPI().deleteCommandById(discordCommand.getSlashCommandSnowflake()).submit().join();
            discordCommand.setSlashCommandSnowflake(-1);

            getRegisteredSlashCommands().remove(discordCommand);
            StreamlineDiscord.getInstance().logInfo("Unregistered &cDiscordCommand &rwith identifier '&d" + discordCommand.getCommandIdentifier() + "&r'.");
        });
    }

    public static DiscordCommand getSlashCommand(long identifier) {
        AtomicReference<DiscordCommand> commandAtomicReference = new AtomicReference<>(null);

        getRegisteredSlashCommands().forEach((command, aLong) -> {
            if (aLong == identifier) commandAtomicReference.set(command);
        });

        return commandAtomicReference.get();
    }
}
