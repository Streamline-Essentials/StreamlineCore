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
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.RedisConfigHandler;
import singularity.utils.MessageUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class OwnRedisClient {
    public static RedisConfigHandler getConfig() {
        return GivenConfigs.getRedisConfig();
    }

    @Getter @Setter
    private static AtomicBoolean connectedAtomic = new AtomicBoolean(false);

    @Getter @Setter
    private static TPTicketListener tpTicketListener;

    @Getter @Setter
    private static RedisClient redisClient;

    public static String getHost() {
        return getConfig().getHost();
    }

    public static int getPort() {
        return getConfig().getPort();
    }

    public static String getUsername() {
        return getConfig().getUsername();
    }

    public static String getPassword() {
        return getConfig().getPassword();
    }

    public static boolean isEnabled() {
        return getConfig().isEnabled();
    }

    public static boolean isConnected() {
        return connectedAtomic.get();
    }

    public static void setConnected(boolean connected) {
        connectedAtomic.set(connected);
    }

    public static boolean isUserValid() {
        return getUsername() != null && ! getUsername().isBlank() && ! getUsername().equals("null");
    }

    public static RedisClient getClient() {
        if (getRedisClient() == null) {
            setRedisClient(create());
        }
        return getRedisClient();
    }

    public static RedisClient create() {
        RedisURI.Builder builder = RedisURI.Builder
                .redis(getHost(), getPort());
        if (isUserValid()) {
            builder = builder.withAuthentication(getUsername(), getPassword());
        } else {
            builder = builder.withPassword(getPasswordChars());
        }

        return RedisClient.create(builder.build());
    }

    public static CharSequence getPasswordChars() {
        return getPassword();
    }

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

    public static void withConnection(Consumer<StatefulRedisConnection<String, String>> consumer) {
        try (RedisClient client = create()) {
            StatefulRedisConnection<String, String> r = client.connect();
            consumer.accept(r);
        }
    }

    public static void withPubSubConnection(Consumer<StatefulRedisPubSubConnection<String, String>> consumer) {
        try (RedisClient client = create()) {
            StatefulRedisPubSubConnection<String, String> r = client.connectPubSub();
            consumer.accept(r);
        }
    }

    public static void withRedis(Consumer<RedisCommands<String, String>> consumer) {
        withConnection(connection -> {
            RedisCommands<String, String> r = connection.sync();
            consumer.accept(r);
        });
    }

    public static void withReactive(Consumer<RedisStringReactiveCommands<String, String>> consumer) {
        withConnection(connection -> {
            RedisStringReactiveCommands<String, String> r = connection.reactive();
            consumer.accept(r);
        });
    }

    public static void withPubSub(Consumer<RedisPubSubCommands<String, String>> consumer) {
        withPubSubConnection(connection -> {
            RedisPubSubCommands<String, String> r = connection.sync();
            consumer.accept(r);
        });
    }

    public static void init() {
        if (isEnabled()) {
            testConnection();
        } else {
            MessageUtils.logInfo("&cRedis &fis not enabled in the configuration. Skipping initialization...");
        }
    }

    public static void sendMessage(RedisMessage message) {
        withRedis(commands -> {
            commands.publish(message.getChannel(), message.getMessage());
            MessageUtils.logDebug("Sent Redis message on channel: " + message.getChannel() + " with content: " + message.getMessage());
        });
    }
}
