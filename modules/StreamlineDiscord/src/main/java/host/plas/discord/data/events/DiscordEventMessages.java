package host.plas.discord.data.events;

import host.plas.StreamlineDiscord;
import host.plas.config.Config;
import host.plas.discord.DiscordHandler;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.channeling.RoutedUser;
import host.plas.discord.data.events.relay.EventRelay;
import host.plas.discord.data.events.relay.RelayedEvent;
import host.plas.discord.messaging.BotMessages;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Posts game events (joins, quits, advancements, deaths, server start and stop) to the Discord channels of
 * every route that has the event enabled.
 *
 * <p>Every message is a bot message (see {@link BotMessages}) and gets these placeholders besides the
 * event's own: {@code %this_player%} (the player's name, Discord markdown escaped), {@code %this_server%}
 * ({@code `name`}, or {@code the server} when this server has no name), {@code %this_server_name%} (the
 * bare name, or {@code The server}) and {@code %this_server_tag%} ({@code  (`name`)}, or nothing).
 *
 * <p>Messages are built and sent on one background thread, so a game thread never waits on files or
 * Discord, and nothing is sent while the bot is not connected. A backend without a usable bot of its own
 * relays its events to the proxy's bot instead ({@link EventRelay}), which posts them with
 * {@link #postRelayed(RelayedEvent)}.
 */
public final class DiscordEventMessages {
    public static final String KEY_JOIN = "events.join";
    public static final String KEY_LEAVE = "events.leave";
    public static final String KEY_ADVANCEMENT = "events.advancement";
    public static final String KEY_DEATH = "events.death";
    public static final String KEY_SERVER_START = "events.server-start";
    public static final String KEY_SERVER_STOP = "events.server-stop";

    /**
     * Set once this JVM's server has finished starting. A system property outlives the module's class
     * loader, so a reloaded module still knows the server is up and never waits for a start event again.
     */
    private static final String PROPERTY_SERVER_STARTED = "streamline.discord.server-started";
    /** Set while the last server message this JVM sent was "online", so each one is sent once. */
    private static final String PROPERTY_ONLINE_ANNOUNCED = "streamline.discord.online-announced";

    /** Longest {@link #announceStop()} waits for Discord to accept the message. */
    private static final long STOP_TIMEOUT_MILLIS = 5000L;

    /** Minecraft colour codes with {@code §} or {@code &}, including {@code §x§r§r§g§g§b§b} hex colours. */
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)[§&]x([§&][0-9a-f]){6}|[§&][0-9a-fk-or]");

    private static ScheduledExecutorService executor;

    private DiscordEventMessages() {
    }

    private static synchronized ScheduledExecutorService executor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "StreamlineDiscord-Events");
                thread.setDaemon(true);
                return thread;
            });
        }
        return executor;
    }

    /** Stops the background thread, dropping messages not yet sent. Runs when the module disables. */
    public static synchronized void shutdown() {
        if (executor != null) executor.shutdownNow();
        executor = null;
    }

    /** Runs {@code task} on the background thread, after {@code delayTicks} ticks (50 ms each). */
    private static void later(Runnable task, long delayTicks) {
        Runnable safe = () -> {
            try {
                task.run();
            } catch (Throwable t) {
                StreamlineDiscord.getInstance().logWarning("Could not send a game event to Discord: " + t);
            }
        };
        try {
            if (delayTicks <= 0) executor().execute(safe);
            else executor().schedule(safe, delayTicks * 50L, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            // Rejected while the module shuts down.
        }
    }

    // ---------------------------------------------------------------- player events

    /**
     * Posts {@code messageKey} about {@code sender} to every route with {@code classifier} enabled whose input
     * takes this player: global routes, routes of the player's server, and permission routes the player passes.
     *
     * @param extra the event's own placeholders, captured on the calling thread; may be null
     */
    public static void sendPlayerEvent(EventClassifier classifier, String messageKey, CosmicSender sender, Map<String, String> extra) {
        sendPlayerEvent(classifier, messageKey, sender, extra, 0L);
    }

    /** {@link #sendPlayerEvent(EventClassifier, String, CosmicSender, Map)} after {@code delayTicks} ticks. */
    public static void sendPlayerEvent(EventClassifier classifier, String messageKey, CosmicSender sender, Map<String, String> extra, long delayTicks) {
        if (sender == null) return;

        later(() -> {
            if (! DiscordHandler.isConnected()) {
                // A backend without a bot of its own hands the event to the proxy's bot.
                EventRelay.relayPlayerEvent(classifier, messageKey, sender, extra);
                return;
            }

            // The player's server is read when sending, so a delayed proxy join names the server reached.
            Map<String, String> placeholders = playerPlaceholders(sender);
            if (extra != null) placeholders.putAll(extra);

            String serverName = sender.getServerName();
            for (Route route : RouteLoader.getLoadedRoutes()) {
                try {
                    if (! route.hasEnabledEvent(classifier)) continue;
                    if (! takesPlayer(route.getInput(), sender, serverName)) continue;

                    route.bounceEvent(new RoutedUser(sender), messageKey, placeholders);
                } catch (Throwable t) {
                    StreamlineDiscord.getInstance().logWarning("Could not send '" + messageKey + "' to route " + route.getIdentifier() + ": " + t);
                }
            }
        }, delayTicks);
    }

    private static boolean takesPlayer(EndPoint input, CosmicSender sender, String serverName) {
        if (input == null || input.getType() == null) return false;

        switch (input.getType()) {
            case GLOBAL_NATIVE:
                return true;
            case SPECIFIC_NATIVE:
            case SPECIFIC_HANDLED:
                return serverName != null && serverName.equals(input.getEndPointIdentifier());
            case PERMISSION:
                return sender.hasPermission(input.getEndPointIdentifier());
            default:
                return false;
        }
    }

    /** The placeholders every player event has; see the class description. */
    public static Map<String, String> playerPlaceholders(CosmicSender sender) {
        Map<String, String> map = new HashMap<>();
        map.put("this_player", escape(sender.getCurrentName()));
        putServer(map, serverNameOf(sender));
        return map;
    }

    /**
     * The name of the server {@code sender} plays on: their backend on a proxy, else this server's name.
     * Empty when unknown.
     */
    public static String serverNameOf(CosmicSender sender) {
        if (isProxy()) {
            String name = sender.getServerName();
            return isKnownServerName(name) ? name : "";
        }
        return ownServerName();
    }

    /**
     * This server's name for event messages: {@code server-events.server-name} in config.yml, else
     * Streamline's server name, unless that is still its default (the server's UUID). Empty when neither is set.
     */
    public static String ownServerName() {
        Config config = StreamlineDiscord.getConfig();
        String configured = config == null ? null : config.serverEventServerName();
        if (configured != null && ! configured.isBlank()) return clean(configured).trim();

        try {
            String name = Singularity.getServerName();
            if (isKnownServerName(name) && ! name.equals(Singularity.getServerUuid())) return name;
        } catch (Throwable ignored) {
            // The core's server config is not loaded.
        }
        return "";
    }

    private static boolean isKnownServerName(String name) {
        if (name == null || name.isBlank()) return false;
        // Placeholder names such as "--null" mean the player has no server yet.
        return ! name.startsWith("--") && ! name.equalsIgnoreCase("null");
    }

    private static void putServer(Map<String, String> map, String serverName) {
        boolean known = serverName != null && ! serverName.isBlank();
        String escaped = known ? escape(serverName) : "";
        map.put("this_server", known ? "`" + serverName.replace("`", "'") + "`" : "the server");
        map.put("this_server_name", known ? escaped : "The server");
        map.put("this_server_tag", known ? " (`" + serverName.replace("`", "'") + "`)" : "");
    }

    // ---------------------------------------------------------------- server start / stop

    /** Marks this server as started. Called on Streamline's server start event and on enable on a proxy. */
    public static void onServerStarted() {
        System.setProperty(PROPERTY_SERVER_STARTED, "true");
        announceStart();
    }

    /** Called whenever the bot finishes connecting. */
    public static void onBotReady() {
        EventRelay.onBotReady();
        announceStart();
    }

    /** Called when the bot could not log in, so a backend announces its start through the proxy instead. */
    public static void onBotLoginFailed() {
        EventRelay.onBotLoginFailed();
        announceStart();
    }

    /**
     * Sends {@code events.server-start} once the server has started and the bot is connected, unless this
     * JVM already announced the server online since its last stop message, so a bot restart or reconnect
     * sends nothing. A backend whose bot is off, has no token or failed to log in relays it to the proxy
     * instead (see {@link EventRelay}).
     */
    public static synchronized void announceStart() {
        if (! Boolean.getBoolean(PROPERTY_SERVER_STARTED)) return;
        if (Boolean.getBoolean(PROPERTY_ONLINE_ANNOUNCED)) return;

        boolean relay = ! DiscordHandler.isConnected();
        if (relay && ! EventRelay.isSending()) return;

        System.setProperty(PROPERTY_ONLINE_ANNOUNCED, "true");

        Config config = StreamlineDiscord.getConfig();
        if (config == null || ! config.serverEventServerStart()) return;

        if (relay) {
            later(() -> EventRelay.relayServerEvent(EventClassifier.SERVER_START, KEY_SERVER_START), 0L);
            return;
        }

        later(() -> {
            for (CompletableFuture<?> sent : sendServerEvent(EventClassifier.SERVER_START, KEY_SERVER_START)) {
                sent.exceptionally(error -> {
                    StreamlineDiscord.getInstance().logWarning("Could not send '" + KEY_SERVER_START + "': " + error.getMessage());
                    return null;
                });
            }
        }, 0L);
    }

    /**
     * Sends {@code events.server-stop} when the server was announced online, and waits up to five seconds for
     * Discord to accept it. Runs on the calling thread, before the bot shuts down, since shutting the bot
     * down cancels requests still queued. A backend that relays its events sends it to the proxy instead,
     * waiting up to three seconds.
     */
    public static synchronized void announceStop() {
        try {
            if (! Boolean.getBoolean(PROPERTY_ONLINE_ANNOUNCED)) return;

            boolean relay = ! DiscordHandler.isConnected();
            if (relay && ! EventRelay.isSending()) return;

            System.clearProperty(PROPERTY_ONLINE_ANNOUNCED);

            Config config = StreamlineDiscord.getConfig();
            if (config == null || ! config.serverEventServerStop()) return;

            if (relay) {
                EventRelay.relayServerEvent(EventClassifier.SERVER_STOP, KEY_SERVER_STOP);
                return;
            }

            List<CompletableFuture<?>> sent = sendServerEvent(EventClassifier.SERVER_STOP, KEY_SERVER_STOP);
            if (sent.isEmpty()) return;

            CompletableFuture.allOf(sent.toArray(new CompletableFuture[0])).get(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send '" + KEY_SERVER_STOP + "' before shutting down: " + t);
        }
    }

    /**
     * Sends a server message to every route with {@code classifier} enabled whose input is global or this
     * server, and hands back each Discord request.
     */
    private static List<CompletableFuture<?>> sendServerEvent(EventClassifier classifier, String messageKey) {
        List<CompletableFuture<?>> sent = new ArrayList<>();

        String name = ownServerName();
        String coreName = null;
        try {
            coreName = Singularity.getServerName();
        } catch (Throwable ignored) {
            // The core's server config is not loaded.
        }

        Map<String, String> placeholders = new HashMap<>();
        putServer(placeholders, name);
        CosmicSender console = UserUtils.getConsole();

        for (Route route : RouteLoader.getLoadedRoutes()) {
            try {
                if (! route.hasEnabledEvent(classifier)) continue;
                if (! takesServer(route.getInput(), name, coreName)) continue;

                sent.add(route.submitEvent(new RoutedUser(console), messageKey, placeholders));
            } catch (Throwable t) {
                StreamlineDiscord.getInstance().logWarning("Could not send '" + messageKey + "' to route " + route.getIdentifier() + ": " + t);
            }
        }
        return sent;
    }

    private static boolean takesServer(EndPoint input, String name, String coreName) {
        if (input == null || input.getType() == null) return false;

        if (input.getType() == EndPointType.GLOBAL_NATIVE) return true;
        if (input.getType() == EndPointType.SPECIFIC_NATIVE || input.getType() == EndPointType.SPECIFIC_HANDLED) {
            String identifier = input.getEndPointIdentifier();
            return identifier != null && (identifier.equals(name) || identifier.equals(coreName));
        }
        return false;
    }

    /** Whether this server is a proxy, whose start event cannot be relied on to reach modules. */
    public static boolean isProxy() {
        try {
            return ModuleUtils.getServerType() == ISingularityExtension.ServerType.PROXY;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------- relayed events

    /**
     * Posts an event another server relayed (see {@link EventRelay}) to every route with its classifier
     * enabled whose input takes it, as if it had happened here: global routes; routes of the origin server
     * (its event name or Streamline name, or for a player the server this proxy sees them on); and, for
     * player events, permission routes the player passes. {@code %this_server*%} name the origin server,
     * and Streamline's player placeholders resolve against the player when this server knows them.
     */
    public static void postRelayed(RelayedEvent event) {
        if (event == null) return;

        later(() -> {
            if (! DiscordHandler.isConnected()) return;

            CosmicSender player = null;
            if (event.isPlayerEvent()) {
                try {
                    player = UserUtils.getOrGetSender(event.getPlayerUuid()).orElse(null);
                } catch (Throwable ignored) {
                    // Unknown here; the payload's name stands in.
                }
            }

            String serverName = event.getOriginName();
            String seenOn = null;
            if (player != null && isProxy()) {
                String name = player.getServerName();
                if (isKnownServerName(name)) seenOn = name;
            }
            if (serverName.isBlank() && seenOn != null) serverName = seenOn;

            Map<String, String> placeholders = new HashMap<>();
            if (event.isPlayerEvent()) {
                String name = event.getPlayerName();
                if ((name == null || name.isBlank()) && player != null) name = player.getCurrentName();
                placeholders.put("this_player", escape(name));
            }
            putServer(placeholders, serverName);
            placeholders.putAll(event.getPlaceholders());

            CosmicSender subject = player != null ? player : UserUtils.getConsole();
            for (Route route : RouteLoader.getLoadedRoutes()) {
                try {
                    if (! route.hasEnabledEvent(event.getClassifier())) continue;
                    if (! takesRelayed(route.getInput(), event, seenOn, player)) continue;

                    route.bounceEvent(new RoutedUser(subject), event.getMessageKey(), placeholders);
                } catch (Throwable t) {
                    StreamlineDiscord.getInstance().logWarning("Could not send relayed '" + event.getMessageKey() + "' to route " + route.getIdentifier() + ": " + t);
                }
            }
        }, 0L);
    }

    private static boolean takesRelayed(EndPoint input, RelayedEvent event, String seenOn, CosmicSender player) {
        if (input == null || input.getType() == null) return false;

        switch (input.getType()) {
            case GLOBAL_NATIVE:
                return true;
            case SPECIFIC_NATIVE:
            case SPECIFIC_HANDLED:
                String identifier = input.getEndPointIdentifier();
                if (identifier == null || identifier.isBlank()) return false;
                return identifier.equals(event.getOriginName()) || identifier.equals(event.getOriginCoreName())
                        || identifier.equals(seenOn);
            case PERMISSION:
                return player != null && event.isPlayerEvent() && player.hasPermission(input.getEndPointIdentifier());
            default:
                return false;
        }
    }

    // ---------------------------------------------------------------- text

    /** {@code text} without Minecraft colour codes; empty for null. */
    public static String clean(String text) {
        if (text == null) return "";
        return COLOR_CODES.matcher(text).replaceAll("");
    }

    /** {@code text} without colour codes and with Discord markdown escaped, so names show as typed. */
    public static String escape(String text) {
        return MarkdownSanitizer.escape(clean(text));
    }
}
