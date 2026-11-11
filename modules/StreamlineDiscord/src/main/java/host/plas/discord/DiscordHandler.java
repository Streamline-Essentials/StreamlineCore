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
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.streamline.api.SLAPI;
import host.plas.StreamlineDiscord;
import host.plas.discord.commands.*;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.BotMessages;
import host.plas.discord.voice.StreamlineVoiceInterceptor;
import host.plas.discord.data.events.DiscordEventMessages;
import host.plas.events.streamline.bot.BotStoppedEvent;
import host.plas.events.streamline.verification.on.VerificationAlreadyVerifiedEvent;
import host.plas.events.streamline.verification.on.VerificationFailureEvent;
import singularity.data.console.CosmicSender;
import singularity.interfaces.ISingularityExtension;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DiscordHandler {

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

    /**
     * Whether the bot is logged in and connected to the gateway right now, so a message sent now
     * goes out instead of waiting (or failing) on a bot that is starting, reconnecting or off.
     */
    public static boolean isConnected() {
        JDA api = getDiscordAPI();
        return api != null && api.getStatus() == JDA.Status.CONNECTED;
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

    /**
     * A user's display tag: {@code name#1234} for legacy accounts, just {@code name} for
     * accounts on Discord's unique-username system, whose discriminator is {@code 0000}.
     */
    public static String getTag(User user) {
        String discriminator = user.getDiscriminator();
        if (discriminator.isEmpty() || discriminator.equals("0000") || discriminator.equals("0")) return user.getName();
        return user.getName() + "#" + discriminator;
    }

    /**
     * Every command built for this login, by identifier, whether or not it is registered,
     * so a disabled command can still be found and turned back on.
     */
    @Getter
    private static final ConcurrentSkipListMap<String, DiscordCommand> knownCommands = new ConcurrentSkipListMap<>();

    private static void track(DiscordCommand command) {
        getKnownCommands().put(command.getCommandIdentifier(), command);
    }

    public static DiscordCommand getKnownCommand(String identifier) {
        if (identifier == null) return null;
        return getKnownCommands().get(identifier.toLowerCase());
    }

    public static void registerCommands() {
        track(new AddAllEventsCommand());
        track(new ChannelCommand());
        track(new ChannelRemoveCommand());
        track(new ChannelSetCommand());
        track(new ChannelsCommand());
        track(new EventsCommand());
        track(new HelpCommand());
        track(new PingCommand());
        track(new ReloadCommand());
        track(new RestartCommand());
        track(new UnVerifyCommand());
        track(new VerifyCommand());
    }

    /**
     * Logs the bot in and waits until it is ready.
     *
     * @param layout the bot configuration
     * @param privileged whether to request the privileged GUILD_MEMBERS and MESSAGE_CONTENT intents.
     *                   Discord closes the gateway with 4014 when a bot asks for a privileged intent
     *                   not enabled in its Developer Portal page; in that case the login is retried
     *                   without them, so the bot still runs with reduced features.
     * @return the ready JDA instance, or {@code null} if login failed
     */
    private static JDA connect(BotLayout layout, boolean privileged) {
        EnumSet<GatewayIntent> intents = GatewayIntent.getIntents(GatewayIntent.DEFAULT);
        if (privileged) {
            intents.add(GatewayIntent.GUILD_MEMBERS);
            intents.add(GatewayIntent.MESSAGE_CONTENT);
        }

        JDA jda = null;
        try {
            jda = JDABuilder.createDefault(layout.getToken(), intents)
                    .setMemberCachePolicy(privileged ? MemberCachePolicy.ALL : MemberCachePolicy.DEFAULT)
                    .setVoiceDispatchInterceptor(new StreamlineVoiceInterceptor())
                    .setActivity(Activity.of(layout.getActivityType(), layout.getActivityValue()))
                    .addEventListeners(new DiscordListener())
                    .build();
            return jda.awaitReady();
        } catch (Exception e) {
            if (jda != null) jda.shutdownNow();

            String message = String.valueOf(e.getMessage());
            if (privileged && message.contains("intent")) {
                StreamlineDiscord.getInstance().logWarning(
                        "&cThe Discord bot is not allowed the privileged intents it needs.&r%newline%" +
                        "&eEnable '&bServer Members Intent&e' and '&bMessage Content Intent&e' for the bot at " +
                        "&bhttps://discord.com/developers/applications&e (Bot tab).%newline%" +
                        "&eRetrying without them; Discord messages will arrive without their text until they are enabled.");
                return connect(layout, false);
            }

            StreamlineDiscord.getInstance().logWarning("Discord login failed: " + message);
            return null;
        }
    }

    public static CompletableFuture<Boolean> init() {
        return CompletableFuture.supplyAsync(() -> {
            kill().join();

            if (! StreamlineDiscord.getConfig().fullDisable()) {
                StreamlineDiscord.getInstance().logInfo("Bot is initializing...!");

                BotLayout layout = StreamlineDiscord.getConfig().getBotLayout();
                JDA jda = connect(layout, true);
                if (jda == null) {
                    StreamlineDiscord.getInstance().logWarning("&cThe Discord bot could not log in; Discord features stay off until the bot is reloaded.");
                    return false;
                }
                setDiscordAPI(jda);
                StreamlineDiscord.getInstance().logInfo("Bot is ready!");

                try {
                    updateBotAvatar(layout.getAvatarUrl());

                    StreamlineDiscord.getInstance().logInfo("Registering Discord commands...");
                    registerCommands();
                    if (layout.isSlashCommandsEnabled()) syncSlashCommands();
                    StreamlineDiscord.getInstance().logInfo("Registered Discord commands!");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (getDiscordAPI() != null) {
                    StreamlineDiscord.getInstance().logInfo("Bot Initialized!");
                    DiscordEventMessages.onBotReady();
                }
            }

            return true;
        });
    }

    public static CompletableFuture<Boolean> kill() {
        return CompletableFuture.supplyAsync(() -> {
            JDA api = getDiscordAPI();
            if (api == null) return false;

            // Slash commands stay registered on Discord across restarts; the next login
            // reuses them as recorded in slash-commands.yml, or compares and sends what changed.
            getRegisteredCommands().clear();
            getRegisteredSlashCommands().clear();
            getPendingSlashCommands().clear();
            getKnownCommands().clear();

            // JDA's worker threads must be gone before the module's class loader closes;
            // a survivor fails with NoClassDefFoundError on its next lazily loaded class.
            api.shutdownNow();
            try {
                api.awaitShutdown(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
            return BotMessages.build("verification.failure.already-verified", UserUtils.getConsole());
        }
        if (! hasVerification(verification)) {
            new VerificationFailureEvent(fromCommand, null, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return BotMessages.build("verification.failure.generic", UserUtils.getConsole());
        }
        CosmicSender user = getPendingVerificationUser(verification).orElse(null);
        if (user == null) {
            new VerificationFailureEvent(fromCommand, null, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return BotMessages.build("verification.failure.generic", UserUtils.getConsole());
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

    /**
     * Commands waiting to be published, by name. A {@link DiscordCommand} lands here as it
     * registers; {@link #syncSlashCommands()} then publishes the whole set at once.
     */
    @Getter
    private static final ConcurrentSkipListMap<String, DiscordCommand> pendingSlashCommands = new ConcurrentSkipListMap<>();

    /**
     * Whether Discord's copy of a command already matches what would be sent. Both sides go
     * through JDA's own serialization of each part, so equal maps mean an update would
     * change nothing.
     */
    private static boolean matches(Command existing, SlashCommandData wanted) {
        if (existing.getType() != Command.Type.SLASH) return false;

        SlashCommandData current = SlashCommandData.fromCommand(existing);

        return current.getDescription().equals(wanted.getDescription())
                && current.isGuildOnly() == wanted.isGuildOnly()
                && current.isNSFW() == wanted.isNSFW()
                && Objects.equals(current.getDefaultPermissions().getPermissionsRaw(), wanted.getDefaultPermissions().getPermissionsRaw())
                && toMaps(current.getOptions()).equals(toMaps(wanted.getOptions()))
                && toMaps(current.getSubcommands()).equals(toMaps(wanted.getSubcommands()))
                && toMaps(current.getSubcommandGroups()).equals(toMaps(wanted.getSubcommandGroups()));
    }

    private static List<Map<String, Object>> toMaps(List<? extends SerializableData> data) {
        return data.stream().map(d -> d.toData().toMap()).collect(Collectors.toList());
    }

    public static void registerSlashCommand(DiscordCommand discordCommand) {
        getPendingSlashCommands().put(discordCommand.getCommandIdentifier(), discordCommand);
    }

    /** The outcome of a slash command sync, worded for whoever asked for it. */
    public static final class SyncResult {
        public final boolean success;
        public final String message;

        private SyncResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /** Guards every slash command sync, so a login and an admin command never overlap. */
    private static final Object SLASH_SYNC_LOCK = new Object();

    @Getter
    private static final SlashCommandState slashCommandState = new SlashCommandState();

    public static SyncResult syncSlashCommands() {
        return syncSlashCommands(false);
    }

    /**
     * Makes the bot's global slash commands match {@link #getPendingSlashCommands()}.
     *
     * <p>Unless {@code force} is set, {@code slash-commands.yml} is checked first: when it
     * records this module version, this bot application and the same command set (by
     * hash), Discord already has these commands and their ids are taken from the file
     * without a request. Otherwise Discord's current set is fetched in one request, and
     * only when a command was added, removed or changed is the full set sent back, as one
     * bulk overwrite, which keeps the ids of commands that did not change. {@code force}
     * skips both checks and always sends the full set. Every successful sync rewrites the
     * file.</p>
     *
     * <p>Blocks on Discord from a single thread. JDA completes requests on
     * {@link java.util.concurrent.ForkJoinPool#commonPool()}, so many common-pool tasks
     * blocking on Discord at once can leave no thread to complete any of them.</p>
     */
    public static SyncResult syncSlashCommands(boolean force) {
        synchronized (SLASH_SYNC_LOCK) {
            JDA jda = getDiscordAPI();
            if (jda == null) return new SyncResult(false, "The Discord bot is not connected.");

            try {
                Map<String, DiscordCommand> pending = new java.util.TreeMap<>(getPendingSlashCommands());

                List<SlashCommandData> wanted = new java.util.ArrayList<>();
                List<String> jsons = new java.util.ArrayList<>();
                pending.forEach((name, discordCommand) -> {
                    SlashCommandData data = discordCommand.setupOptionData(jda.upsertCommand(name, discordCommand.getDescription()));
                    wanted.add(data);
                    jsons.add(new String(data.toData().toJson(), java.nio.charset.StandardCharsets.UTF_8));
                });

                String version = SlashCommandState.moduleVersion();
                String hash = SlashCommandState.hash(jsons);
                long applicationId = jda.getSelfUser().getApplicationIdLong();

                if (! force) {
                    Map<String, Long> stored = getSlashCommandState().idsIfCurrent(version, hash, applicationId, pending.keySet());
                    if (stored != null) {
                        getRegisteredSlashCommands().clear();
                        stored.forEach((name, id) -> mapSlashCommand(pending.get(name), id));
                        String message = "Slash commands unchanged since the last sync (" + pending.size() + " commands); nothing sent to Discord.";
                        StreamlineDiscord.getInstance().logInfo(message);
                        return new SyncResult(true, message);
                    }
                }

                java.util.Collection<Command> live;
                String message;
                if (force) {
                    live = jda.updateCommands().addCommands(wanted).complete();
                    message = "Re-registered all " + wanted.size() + " slash commands with Discord.";
                } else {
                    Map<String, Command> existing = new java.util.HashMap<>();
                    jda.retrieveCommands().complete().forEach(command -> existing.put(command.getName(), command));

                    List<String> changes = new java.util.ArrayList<>();
                    for (SlashCommandData data : wanted) {
                        Command current = existing.get(data.getName());
                        if (current == null) changes.add("+" + data.getName());
                        else if (! matches(current, data)) changes.add("~" + data.getName());
                    }
                    existing.keySet().forEach(name -> {
                        if (! pending.containsKey(name)) changes.add("-" + name);
                    });

                    if (changes.isEmpty()) {
                        live = existing.values();
                        message = "Slash commands are up to date on Discord; nothing sent.";
                    } else {
                        live = jda.updateCommands().addCommands(wanted).complete();
                        message = "Published slash commands to Discord (" + String.join(" ", changes) + ").";
                    }
                }
                StreamlineDiscord.getInstance().logInfo(message);

                getRegisteredSlashCommands().clear();
                Map<String, Long> ids = new java.util.TreeMap<>();
                live.forEach(command -> {
                    DiscordCommand discordCommand = pending.get(command.getName());
                    if (discordCommand == null) return;

                    mapSlashCommand(discordCommand, command.getIdLong());
                    ids.put(command.getName(), command.getIdLong());
                });

                getSlashCommandState().save(version, hash, applicationId, ids);
                return new SyncResult(true, message);
            } catch (Exception e) {
                StreamlineDiscord.getInstance().logWarning("Could not sync slash commands with Discord: " + e.getMessage());
                StreamlineDiscord.getInstance().logWarning(e.getStackTrace());
                return new SyncResult(false, "Could not sync slash commands with Discord: " + e.getMessage());
            }
        }
    }

    /**
     * Runs {@link #syncSlashCommands(boolean)} on its own short-lived thread, never the
     * common pool (see there), and completes with its result.
     */
    public static CompletableFuture<SyncResult> syncSlashCommandsAsync(boolean force) {
        CompletableFuture<SyncResult> future = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            try {
                future.complete(syncSlashCommands(force));
            } catch (Throwable t) {
                future.complete(new SyncResult(false, "Could not sync slash commands with Discord: " + t.getMessage()));
            }
        }, "StreamlineDiscord-SlashSync");
        thread.setDaemon(true);
        thread.start();
        return future;
    }

    private static void mapSlashCommand(DiscordCommand discordCommand, long id) {
        if (discordCommand == null) return;
        getRegisteredSlashCommands().put(discordCommand, id);
        discordCommand.setSlashCommandSnowflake(id);
    }

    /**
     * Drops a command from the slash command set kept here. Discord's list follows on the
     * next {@link #syncSlashCommands(boolean)}, which sends the whole set at once.
     */
    public static void unregisterSlashCommand(DiscordCommand discordCommand) {
        getPendingSlashCommands().remove(discordCommand.getCommandIdentifier());
        getRegisteredSlashCommands().remove(discordCommand);
        discordCommand.setSlashCommandSnowflake(-1);
    }

    public static DiscordCommand getSlashCommand(long identifier) {
        AtomicReference<DiscordCommand> commandAtomicReference = new AtomicReference<>(null);

        getRegisteredSlashCommands().forEach((command, aLong) -> {
            if (aLong == identifier) commandAtomicReference.set(command);
        });

        return commandAtomicReference.get();
    }

    /**
     * The command a slash interaction is for. Interactions name their command id; when
     * that id is not one this bot mapped (ids reused from a {@code slash-commands.yml} that
     * no longer matches Discord), the command is found by name instead, mapped to the id
     * Discord used, and the file is marked stale so the next start syncs with Discord.
     */
    public static DiscordCommand resolveSlashCommand(long id, String name) {
        DiscordCommand command = getSlashCommand(id);
        if (command != null) return command;

        command = getPendingSlashCommands().get(name);
        if (command == null) return null;

        mapSlashCommand(command, id);
        StreamlineDiscord.getInstance().logWarning("Slash command '/" + name + "' arrived with an unknown id; "
                + "slash commands will be synced with Discord on the next start.");
        try {
            getSlashCommandState().markStale();
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Could not mark " + SlashCommandState.FILE_NAME + " stale: " + e.getMessage());
        }
        return command;
    }
}
