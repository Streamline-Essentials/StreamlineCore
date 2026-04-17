package singularity.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.RedisConfigHandler;
import singularity.utils.MessageUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Static facade for managing the Streamline Redis connection lifecycle and
 * providing scoped access to Lettuce clients.
 *
 * <p>This class reads Redis connection parameters from {@link RedisConfigHandler}
 * (via {@link GivenConfigs}), constructs a {@link RedisClient}, verifies
 * connectivity on startup, and exposes helper methods for executing commands
 * through short-lived, auto-closed connections.</p>
 *
 * <p>The {@link TPTicketListener} is also initialised here after a successful
 * connection test.</p>
 */
public class OwnRedisClient {

    /**
     * Returns the active Redis configuration handler.
     *
     * @return the {@link RedisConfigHandler} sourced from {@link GivenConfigs}
     */
    public static RedisConfigHandler getConfig() {
        return GivenConfigs.getRedisConfig();
    }

    /**
     * Atomic flag tracking whether a successful Redis connection has been established.
     * Managed by Lombok-generated {@code getConnectedAtomic()} / {@code setConnectedAtomic()}.
     */
    @Getter @Setter
    private static AtomicBoolean connectedAtomic = new AtomicBoolean(false);

    /**
     * The singleton {@link TPTicketListener} that handles teleport-ticket messages
     * over Redis pub/sub. Initialised by {@link #testConnection()} on success.
     */
    @Getter @Setter
    private static TPTicketListener tpTicketListener;

    /**
     * The cached Lettuce {@link RedisClient} instance. Created lazily by
     * {@link #getClient()}.
     */
    @Getter @Setter
    private static RedisClient redisClient;

    /**
     * Returns the Redis host from the configuration.
     *
     * @return the hostname or IP address of the Redis server
     */
    public static String getHost() {
        return getConfig().getHost();
    }

    /**
     * Returns the Redis port from the configuration.
     *
     * @return the port number of the Redis server
     */
    public static int getPort() {
        return getConfig().getPort();
    }

    /**
     * Returns the Redis username from the configuration.
     *
     * @return the ACL username, or {@code null} / blank if not configured
     */
    public static String getUsername() {
        return getConfig().getUsername();
    }

    /**
     * Returns the Redis password from the configuration.
     *
     * @return the password string
     */
    public static String getPassword() {
        return getConfig().getPassword();
    }

    /**
     * Returns the channel/key prefix from the configuration. All keys and channel
     * names published through this client are wrapped with this prefix via
     * {@link #wrapKey(String)}.
     *
     * @return the prefix string, which may be empty but not {@code null}
     */
    public static String getPrefix() {
        return getConfig().getPrefix();
    }

    /**
     * Returns whether Redis integration is enabled in the configuration.
     *
     * @return {@code true} if Redis is enabled
     */
    public static boolean isEnabled() {
        return getConfig().isEnabled();
    }

    /**
     * Returns whether a successful connection to Redis has been established.
     *
     * @return {@code true} if connected
     */
    public static boolean isConnected() {
        return connectedAtomic.get();
    }

    /**
     * Returns {@code true} only when Redis is both enabled in the configuration
     * and currently connected.
     *
     * @return {@code true} if Redis is enabled and connected
     */
    public static boolean isEnabledAndConnected() {
        return isEnabled() && isConnected();
    }

    /**
     * Updates the connection state flag.
     *
     * @param connected {@code true} to mark the client as connected
     */
    public static void setConnected(boolean connected) {
        connectedAtomic.set(connected);
    }

    /**
     * Returns {@code true} if a non-blank, non-{@code "null"} username is present
     * in the configuration, indicating that ACL authentication should be used.
     *
     * @return {@code true} if a valid username is configured
     */
    public static boolean isUserValid() {
        return getUsername() != null && ! getUsername().isBlank() && ! getUsername().equals("null");
    }

    /**
     * Returns the cached {@link RedisClient}, creating one via {@link #create()} if
     * it has not been initialised yet.
     *
     * @return the shared {@link RedisClient} instance
     */
    public static RedisClient getClient() {
        if (getRedisClient() == null) {
            setRedisClient(create());
        }
        return getRedisClient();
    }

    /**
     * Builds and returns a new {@link RedisClient} configured from the current
     * {@link RedisConfigHandler} settings.
     *
     * <p>Uses username + password authentication when {@link #isUserValid()} is
     * {@code true}; otherwise uses password-only authentication.</p>
     *
     * @return a freshly created {@link RedisClient}
     */
    public static RedisClient create() {
        RedisURI.Builder builder = RedisURI.Builder
                .redis(getHost(), getPort());
        if (isUserValid()) {
            builder = builder.withAuthentication(getUsername(), getPasswordChars());
        } else {
            builder = builder.withPassword(getPasswordChars());
        }
        builder = builder.withLibraryName("StreamlineCore-Lettuce").withLibraryVersion("1.0.0");

        return RedisClient.create(builder.build());
    }

    /**
     * Returns the configured password as a {@link CharSequence}, as required by
     * the Lettuce authentication API.
     *
     * @return the password character sequence
     */
    public static CharSequence getPasswordChars() {
        return getPassword();
    }

    /**
     * Opens a test connection to Redis, updates the {@link #isConnected()} flag,
     * logs the result, and — on success — initialises and registers the
     * {@link TPTicketListener}.
     *
     * <p>If the connection cannot be opened, Redis features are disabled for this
     * session.</p>
     */
    public static void testConnection() {
        RedisClient client = getClient();
        StatefulRedisConnection<String, String> connection = client.connect();

        if (connection.isOpen()) {
            MessageUtils.logInfo("&cRedis &fauthenticated &asuccessfully&f!");

            setConnected(true);
            String pingResponse = connection.sync().ping();
            MessageUtils.logInfo("&cRedis &fping response: " + (Objects.equals(pingResponse, "PONG") ? "&aCONNECTED" : "&cFAILURE"));
        } else {
            MessageUtils.logInfo("&cRedis authentication &cfailed&f: &b" + getHost() + ":" + getPort() + "&f. Please check your configuration.");
            setConnected(false);
        }

        if (isConnected()) {
            tpTicketListener = new TPTicketListener();
            tpTicketListener.registerAndLoad();
        } else {
            MessageUtils.logInfo("&cRedis &fis not connected. Disabling Redis features...");
        }
    }

    /**
     * Opens a short-lived {@link StatefulRedisConnection}, passes it to
     * {@code consumer}, then closes both the connection and the temporary client.
     *
     * @param consumer the action to perform with the connection
     */
    public static void withConnection(Consumer<StatefulRedisConnection<String, String>> consumer) {
        try (RedisClient client = create()) {
            StatefulRedisConnection<String, String> r = client.connect();
            consumer.accept(r);
        }
    }

    /**
     * Opens a short-lived pub/sub connection, passes it to {@code consumer}, then
     * closes both the connection and the temporary client.
     *
     * @param consumer the action to perform with the pub/sub connection
     */
    public static void withPubSubConnection(Consumer<StatefulRedisPubSubConnection<String, String>> consumer) {
        try (RedisClient client = create()) {
            StatefulRedisPubSubConnection<String, String> r = client.connectPubSub();
            consumer.accept(r);
        }
    }

    /**
     * Executes {@code consumer} with synchronous Redis commands via a short-lived
     * connection that is closed after the consumer returns.
     *
     * @param consumer the action to perform with the synchronous command interface
     */
    public static void withRedis(Consumer<RedisCommands<String, String>> consumer) {
        withConnection(connection -> {
            RedisCommands<String, String> r = connection.sync();
            consumer.accept(r);
        });
    }

    /**
     * Executes {@code consumer} with the reactive Redis string commands via a
     * short-lived connection that is closed after the consumer returns.
     *
     * @param consumer the action to perform with the reactive command interface
     */
    public static void withReactive(Consumer<RedisStringReactiveCommands<String, String>> consumer) {
        withConnection(connection -> {
            RedisStringReactiveCommands<String, String> r = connection.reactive();
            consumer.accept(r);
        });
    }

    /**
     * Executes {@code consumer} with synchronous pub/sub commands via a short-lived
     * connection that is closed after the consumer returns.
     *
     * @param consumer the action to perform with the synchronous pub/sub command interface
     */
    public static void withPubSub(Consumer<RedisPubSubCommands<String, String>> consumer) {
        withPubSubConnection(connection -> {
            RedisPubSubCommands<String, String> r = connection.sync();
            consumer.accept(r);
        });
    }

    /**
     * Initialises the Redis subsystem.
     *
     * <p>If Redis is enabled in the configuration, {@link #testConnection()} is
     * called; otherwise a log message is emitted and initialisation is skipped.</p>
     */
    public static void init() {
        if (isEnabled()) {
            testConnection();
        } else {
            MessageUtils.logInfo("&cRedis &fis not enabled in the configuration. Skipping initialization...");
        }
    }

    /**
     * Publishes a {@link RedisMessage} on its wrapped channel using a short-lived
     * synchronous connection.
     *
     * @param message the message to publish; its channel is wrapped with the
     *                configured prefix before publishing
     */
    public static void sendMessage(RedisMessage message) {
        withRedis(commands -> {
            commands.publish(message.wrappedChannel(), message.getMessage());
            MessageUtils.logDebug("Sent Redis message on channel: " + message.wrappedChannel() + " with content: " + message.getMessage());
        });
    }

    /**
     * Wraps the given key with the configured prefix, if a prefix is set. If no prefix is set, it returns the key unchanged.
     * @param key The key to wrap with the prefix.
     * @return The key wrapped with the prefix if a prefix is set, or the original key if no prefix is set.
     */
    public static String wrapKey(String key) {
        return getPrefix() + key;
    }

    /**
     * Checks if the given key starts with the configured prefix. If no prefix is set, it returns true for all keys.
     * @param key The key to check.
     * @return True if the key starts with the prefix or if no prefix is set, false otherwise.
     */
    public static boolean hasPrefix(String key) {
        if (getPrefix() == null) return true;

        return key.startsWith(getPrefix());
    }
}
