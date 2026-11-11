package host.plas.discord.data.events.relay;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.StreamlineDiscord;
import host.plas.config.Config;
import host.plas.discord.DiscordHandler;
import host.plas.discord.data.events.DiscordEventMessages;
import host.plas.discord.data.events.EventClassifier;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.database.CoreDBOperator;
import singularity.database.DatabaseType;
import singularity.interfaces.ISingularityExtension;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.redis.OwnRedisClient;
import singularity.redis.RedisListener;
import singularity.redis.RedisMessage;
import singularity.utils.UserUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Hands game events from servers without a Discord bot of their own (usually backends) to a server with one
 * (usually the proxy), which posts them through its routes.
 *
 * <p><b>Sending</b> (backends, {@code event-relay.enabled}): an event is relayed instead of posted when this
 * server's bot is turned off ({@code bot.full-disable}), has no token, or failed to log in. Join and leave
 * messages are never relayed, because the proxy posts those itself. The transport is
 * {@code event-relay.transport}; {@code auto} picks Redis (Streamline's Redis enabled and connected), then the
 * database (Streamline's database is MySQL, which every server shares), then plugin messages (Spigot and Paper
 * only, carried by an online player; queued while nobody is online and dropped after
 * {@link #PLUGIN_QUEUE_MAX_AGE_MILLIS}).
 *
 * <p><b>Receiving</b> ({@code event-relay.receive}, on a server whose own bot is set up): listens on every
 * transport it can reach at once, since each backend picks its transport by its own settings: the Redis
 * channel, the database table (polled every second, rows claimed by deleting them and pruned after
 * {@link #DATABASE_MAX_AGE_MILLIS}) and, on a proxy, the plugin-message sub-channel. Events are deduplicated by
 * id, events from this server are ignored, and relayed joins and leaves are dropped on a proxy.
 *
 * <p>Everything runs on one daemon thread owned by the module; {@link #stop()} ends it, closes the Redis
 * connection and unregisters the listener, so a reload leaves nothing running.
 */
public final class EventRelay {
    public static final String REDIS_CHANNEL = "streamline-discord:event-relay";
    public static final String PLUGIN_SUB_CHANNEL = "discord-event-relay";
    public static final String PLUGIN_PAYLOAD_KEY = "payload";
    public static final String TABLE = "discord_event_relay";

    /** How long a queued plugin message waits for a player to carry it. */
    public static final long PLUGIN_QUEUE_MAX_AGE_MILLIS = TimeUnit.MINUTES.toMillis(3);
    /** Rows older than this are deleted unread; a receiver also ignores them. */
    public static final long DATABASE_MAX_AGE_MILLIS = TimeUnit.MINUTES.toMillis(5);
    /** Longest the server stop message may hold up shutdown. */
    public static final long STOP_TIMEOUT_MILLIS = 3000L;

    private static final int SEEN_IDS = 2048;

    public enum Transport {
        REDIS("Redis"),
        DATABASE("the database"),
        PLUGIN_MESSAGES("plugin messages");

        private final String label;

        Transport(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static ScheduledExecutorService executor;
    private static RedisListener redisListener;
    private static RelayListener pluginListener;
    private static volatile boolean databaseReady;
    private static volatile boolean loginFailed;

    private static final ConcurrentLinkedQueue<RelayedEvent> pluginQueue = new ConcurrentLinkedQueue<>();

    /** Ids of events already handled, oldest first, so a copy arriving on a second transport is ignored. */
    private static final Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > SEEN_IDS;
        }
    };

    private EventRelay() {
    }

    // ---------------------------------------------------------------- lifecycle

    /** Starts sending and receiving as configured and logs which transports are in use. Runs on enable. */
    public static synchronized void start() {
        stop();

        Config config = StreamlineDiscord.getConfig();
        if (config == null) return;

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "StreamlineDiscord-Relay");
            thread.setDaemon(true);
            return thread;
        });

        if (isBackend() && config.eventRelayEnabled()) {
            Transport transport = sendTransport();
            String when = ownBotDisabled() ? "This server has no Discord bot of its own, so" : "If this server's Discord bot cannot log in,";
            if (transport != null) {
                StreamlineDiscord.getInstance().logInfo("Event relay: " + when + " game events go to the proxy's bot over " + transport.label() + ".");
            } else {
                StreamlineDiscord.getInstance().logInfo("Event relay: " + when + " game events cannot reach the proxy: " + noTransportReason() + ".");
            }
            // Started whenever plugin messages could be picked, since auto falls back to them if Redis drops.
            if (allowed(Transport.PLUGIN_MESSAGES) && isSpigot()) {
                executor.scheduleWithFixedDelay(EventRelay::flushPluginQueue, 1, 1, TimeUnit.SECONDS);
            }
        }

        if (config.eventRelayReceive() && ! ownBotDisabled()) startReceiving();
    }

    /** Stops every timer, connection and listener the relay started and forgets queued events. */
    public static synchronized void stop() {
        if (executor != null) executor.shutdownNow();
        executor = null;

        if (redisListener != null) {
            try {
                redisListener.getConnection().removeListener(redisListener);
            } catch (Throwable ignored) {
                // The connection is already gone.
            }
            try {
                redisListener.getConnection().close();
            } catch (Throwable ignored) {
                // The connection is already gone.
            }
            try {
                redisListener.unload();
            } catch (Throwable ignored) {
                // Not registered.
            }
            redisListener = null;
        }

        if (pluginListener != null) {
            try {
                BaseEventHandler.unbake(pluginListener);
            } catch (Throwable ignored) {
                // Not registered.
            }
            pluginListener = null;
        }

        databaseReady = false;
        pluginQueue.clear();
    }

    private static void startReceiving() {
        List<String> via = new ArrayList<>();

        if (allowed(Transport.REDIS) && redisAvailable()) {
            try {
                RedisListener listener = new RedisListener("streamline-discord-event-relay", REDIS_CHANNEL) {
                    @Override
                    public void onMessage(RedisMessage message) {
                        String payload = message.getMessage();
                        submit(() -> receive(RelayedEvent.fromJson(payload)));
                    }
                };
                // Registered here, not through registerAndLoad(), whose background register() would reopen
                // a connection closed by a quick disable.
                listener.register();
                listener.load();
                redisListener = listener;
                via.add(Transport.REDIS.label());
            } catch (Throwable t) {
                StreamlineDiscord.getInstance().logWarning("Event relay: could not listen on Redis: " + t);
            }
        }

        if (allowed(Transport.DATABASE) && databaseAvailable()) {
            executor.scheduleWithFixedDelay(EventRelay::pollDatabase, 2, 1, TimeUnit.SECONDS);
            executor.scheduleWithFixedDelay(EventRelay::pruneDatabase, 30, 60, TimeUnit.SECONDS);
            via.add(Transport.DATABASE.label());
        }

        if (allowed(Transport.PLUGIN_MESSAGES) && DiscordEventMessages.isProxy()) {
            pluginListener = new RelayListener();
            ModuleUtils.listen(pluginListener, StreamlineDiscord.getInstance());
            via.add(Transport.PLUGIN_MESSAGES.label());
        }

        if (! via.isEmpty()) {
            StreamlineDiscord.getInstance().logInfo("Event relay: posting game events other servers send over " + String.join(", ", via) + ".");
        }
    }

    private static void submit(Runnable task) {
        ScheduledExecutorService current = executor;
        if (current == null) return;
        try {
            current.execute(() -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    StreamlineDiscord.getInstance().logWarning("Event relay: " + t);
                }
            });
        } catch (Throwable ignored) {
            // Rejected while the module shuts down.
        }
    }

    // ---------------------------------------------------------------- bot state

    /** Called when this server's bot failed to log in, so its events go to the proxy from now on. */
    public static void onBotLoginFailed() {
        loginFailed = true;
    }

    /** Called when this server's bot is connected. */
    public static void onBotReady() {
        loginFailed = false;
    }

    /** Whether this server's bot is turned off or has no token, so it never posts anything itself. */
    public static boolean ownBotDisabled() {
        Config config = StreamlineDiscord.getConfig();
        if (config == null) return true;
        if (config.fullDisable()) return true;

        String token = config.getBotLayout().getToken();
        return token == null || token.isBlank() || token.startsWith("<");
    }

    /**
     * Whether this server hands its events to another server: it is a backend with relaying on, and its own
     * bot is turned off, has no token or failed to log in.
     */
    public static boolean isSending() {
        Config config = StreamlineDiscord.getConfig();
        if (config == null || ! config.eventRelayEnabled()) return false;
        if (! isBackend()) return false;
        return ownBotDisabled() || loginFailed;
    }

    /** Whether events of {@code classifier} are relayed at all; joins and leaves are posted by the proxy. */
    public static boolean relays(EventClassifier classifier) {
        return classifier != EventClassifier.LOGIN && classifier != EventClassifier.LOGOUT;
    }

    // ---------------------------------------------------------------- sending

    /**
     * Relays a player event when {@link #isSending()}; returns whether it was handed to a transport.
     *
     * @param extra the event's own placeholders, already escaped; may be null
     */
    public static boolean relayPlayerEvent(EventClassifier classifier, String messageKey, CosmicSender sender, Map<String, String> extra) {
        if (sender == null || ! relays(classifier) || ! isSending()) return false;

        RelayedEvent event = RelayedEvent.create(Singularity.getServerUuid(), DiscordEventMessages.ownServerName(), coreServerName(),
                classifier, messageKey, sender.getUuid(), DiscordEventMessages.clean(sender.getCurrentName()), extra);
        return send(event, false);
    }

    /**
     * Relays this server's start or stop when {@link #isSending()}. A stop is sent on the calling thread and
     * waits up to {@link #STOP_TIMEOUT_MILLIS}; plugin messages carry it only while a player is online.
     */
    public static boolean relayServerEvent(EventClassifier classifier, String messageKey) {
        if (! isSending()) return false;

        RelayedEvent event = RelayedEvent.create(Singularity.getServerUuid(), DiscordEventMessages.ownServerName(), coreServerName(),
                classifier, messageKey, null, null, null);
        return send(event, classifier == EventClassifier.SERVER_STOP);
    }

    private static boolean send(RelayedEvent event, boolean shuttingDown) {
        Transport transport = sendTransport();
        if (transport == null) {
            StreamlineDiscord.getInstance().logDebug("Event relay: dropped '" + event.getMessageKey() + "': " + noTransportReason() + ".");
            return false;
        }

        if (transport == Transport.PLUGIN_MESSAGES) {
            if (sendPluginMessage(event)) return true;
            if (shuttingDown) {
                StreamlineDiscord.getInstance().logDebug("Event relay: no player online to carry '" + event.getMessageKey() + "' to the proxy.");
                return false;
            }
            pluginQueue.add(event);
            return true;
        }

        Runnable task = transport == Transport.REDIS ? () -> sendRedis(event) : () -> sendDatabase(event);
        try {
            if (shuttingDown) {
                CompletableFuture.runAsync(task).get(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } else {
                task.run();
            }
            return true;
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Event relay: could not send '" + event.getMessageKey() + "' over " + transport.label() + ": " + t);
            return false;
        }
    }

    private static void sendRedis(RelayedEvent event) {
        new RedisMessage(REDIS_CHANNEL, event.toJson()).send();
    }

    private static void sendDatabase(RelayedEvent event) {
        CoreDBOperator database = Singularity.getMainDatabase();
        ensureTable(database);

        String sql = "INSERT INTO " + table(database) + " (Id, Origin, Payload, CreatedAt) VALUES (?, ?, ?, ?);";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getId());
            statement.setString(2, event.getOriginUuid());
            statement.setString(3, event.toJson());
            statement.setLong(4, event.getTime());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Sends {@code event} over plugin messages if a player is online to carry it. */
    private static boolean sendPluginMessage(RelayedEvent event) {
        CosmicPlayer carrier = carrier(event);
        if (carrier == null) return false;

        ProxiedMessage message = new ProxiedMessage(carrier, false);
        message.setSubChannel(PLUGIN_SUB_CHANNEL);
        message.write(PLUGIN_PAYLOAD_KEY, event.toBase64());
        message.send();
        return true;
    }

    /** The event's own player when online, else any online player; null when nobody is online. */
    private static CosmicPlayer carrier(RelayedEvent event) {
        Map<String, CosmicPlayer> online = UserUtils.getOnlinePlayers();
        if (online.isEmpty()) return null;

        if (event.isPlayerEvent()) {
            CosmicPlayer player = online.get(event.getPlayerUuid());
            if (player != null && player.isOnline()) return player;
        }
        for (CosmicPlayer player : online.values()) {
            if (player != null && player.isOnline()) return player;
        }
        return null;
    }

    private static void flushPluginQueue() {
        long now = System.currentTimeMillis();
        Iterator<RelayedEvent> iterator = pluginQueue.iterator();
        while (iterator.hasNext()) {
            RelayedEvent event = iterator.next();
            if (now - event.getTime() > PLUGIN_QUEUE_MAX_AGE_MILLIS) {
                iterator.remove();
                StreamlineDiscord.getInstance().logDebug("Event relay: dropped '" + event.getMessageKey() + "', no player came online to carry it.");
                continue;
            }
            try {
                if (! sendPluginMessage(event)) return;
                iterator.remove();
            } catch (Throwable t) {
                iterator.remove();
                StreamlineDiscord.getInstance().logWarning("Event relay: could not send '" + event.getMessageKey() + "' over plugin messages: " + t);
            }
        }
    }

    // ---------------------------------------------------------------- transports

    /** The transport this server sends with now, or null when none is available. */
    public static Transport sendTransport() {
        for (Transport transport : Transport.values()) {
            if (! allowed(transport)) continue;
            if (available(transport)) return transport;
        }
        return null;
    }

    private static boolean available(Transport transport) {
        switch (transport) {
            case REDIS:
                return redisAvailable();
            case DATABASE:
                return databaseAvailable();
            case PLUGIN_MESSAGES:
                return isSpigot();
            default:
                return false;
        }
    }

    /** Whether {@code event-relay.transport} lets {@code transport} be used. */
    private static boolean allowed(Transport transport) {
        String configured = configuredTransport();
        if (configured.equals("auto")) return true;
        return configured.equals(transport.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    private static String configuredTransport() {
        Config config = StreamlineDiscord.getConfig();
        String value = config == null ? "auto" : config.eventRelayTransport();
        if (value == null || value.isBlank()) return "auto";
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (normalized.equals("plugin-message") || normalized.equals("plugin") || normalized.equals("messages")) return "plugin-messages";
        if (normalized.equals("mysql") || normalized.equals("db")) return "database";
        return normalized;
    }

    private static String noTransportReason() {
        String configured = configuredTransport();
        switch (configured) {
            case "redis":
                return "Streamline's Redis is not enabled or not connected";
            case "database":
                return "Streamline's database is not MySQL";
            case "plugin-messages":
                return "plugin messages only work on Spigot and Paper servers";
            case "auto":
                return "Streamline's Redis is off, its database is not MySQL, and this is not a Spigot or Paper server";
            default:
                return "event-relay.transport '" + configured + "' is not auto, redis, database or plugin-messages";
        }
    }

    private static boolean redisAvailable() {
        try {
            return OwnRedisClient.isEnabledAndConnected();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean databaseAvailable() {
        try {
            CoreDBOperator database = Singularity.getMainDatabase();
            return database != null && database.getType() == DatabaseType.MYSQL;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String table(CoreDBOperator database) {
        return database.getConnectorSet().getTablePrefix() + TABLE;
    }

    private static void ensureTable(CoreDBOperator database) {
        if (databaseReady) return;

        String sql = "CREATE TABLE IF NOT EXISTS " + table(database) + " (" +
                "Id VARCHAR(36) NOT NULL," +
                "Origin VARCHAR(64) NOT NULL," +
                "Payload MEDIUMTEXT NOT NULL," +
                "CreatedAt BIGINT NOT NULL," +
                "PRIMARY KEY (Id)," +
                "INDEX (CreatedAt)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
            databaseReady = true;
        } catch (Exception e) {
            throw new IllegalStateException("could not create " + table(database) + ": " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------- receiving

    private static void pollDatabase() {
        try {
            CoreDBOperator database = Singularity.getMainDatabase();
            ensureTable(database);

            // Rows are read first and claimed after, never while the query's connection is held.
            List<String[]> rows = new ArrayList<>();
            String select = "SELECT Id, Payload FROM " + table(database) + " WHERE Origin <> ? AND CreatedAt >= ? ORDER BY CreatedAt LIMIT 100;";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setString(1, Singularity.getServerUuid());
                statement.setLong(2, System.currentTimeMillis() - DATABASE_MAX_AGE_MILLIS);
                try (ResultSet set = statement.executeQuery()) {
                    while (set.next()) rows.add(new String[] { set.getString(1), set.getString(2) });
                }
            }

            for (String[] row : rows) {
                // Deleting the row claims it: when two servers poll the table, only one posts it.
                if (! claim(database, row[0])) continue;
                receive(RelayedEvent.fromJson(row[1]));
            }
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logDebug("Event relay: could not read relayed events from the database: " + t);
        }
    }

    private static boolean claim(CoreDBOperator database, String id) throws Exception {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table(database) + " WHERE Id = ?;")) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void pruneDatabase() {
        try {
            CoreDBOperator database = Singularity.getMainDatabase();
            ensureTable(database);
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table(database) + " WHERE CreatedAt < ?;")) {
                statement.setLong(1, System.currentTimeMillis() - DATABASE_MAX_AGE_MILLIS);
                statement.executeUpdate();
            }
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logDebug("Event relay: could not prune old relayed events: " + t);
        }
    }

    /** Posts a relayed event unless it is this server's own, already handled, too old, or a join or leave on a proxy. */
    static void receive(RelayedEvent event) {
        if (event == null) return;

        Config config = StreamlineDiscord.getConfig();
        if (config == null || ! config.eventRelayReceive()) return;
        if (event.getOriginUuid().equals(Singularity.getServerUuid())) return;
        if (event.getTime() > 0 && System.currentTimeMillis() - event.getTime() > DATABASE_MAX_AGE_MILLIS) return;
        if (! relays(event.getClassifier()) && DiscordEventMessages.isProxy()) return;

        synchronized (seen) {
            if (seen.containsKey(event.getId())) return;
            seen.put(event.getId(), Boolean.TRUE);
        }

        if (! DiscordHandler.isConnected()) return;
        DiscordEventMessages.postRelayed(event);
    }

    // ---------------------------------------------------------------- helpers

    private static String coreServerName() {
        try {
            return Singularity.getServerName();
        } catch (Throwable t) {
            return "";
        }
    }

    private static boolean isBackend() {
        try {
            return ModuleUtils.getServerType() == ISingularityExtension.ServerType.BACKEND;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isSpigot() {
        try {
            return ModuleUtils.getPlatformType() == ISingularityExtension.PlatformType.SPIGOT;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Takes relayed events arriving at a proxy as plugin messages. */
    public static class RelayListener implements BaseEventListener {
        @BaseProcessor
        public void onPluginMessage(ProxyMessageInEvent event) {
            ProxiedMessage message = event.getMessage();
            if (message == null) return;
            if (! PLUGIN_SUB_CHANNEL.equals(message.getSubChannel())) return;

            String payload = message.getString(PLUGIN_PAYLOAD_KEY);
            submit(() -> receive(RelayedEvent.fromBase64(payload)));
        }
    }
}
