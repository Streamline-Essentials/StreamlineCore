package singularity.redis;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.RedisConfigHandler;
import singularity.utils.MessageUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RedisClient {
    public static RedisConfigHandler getConfig() {
        return GivenConfigs.getRedisConfig();
    }

    @Getter @Setter
    private static AtomicBoolean connectedAtomic = new AtomicBoolean(false);

    @Getter @Setter
    private static TPTicketListener tpTicketListener;

    @Getter @Setter
    private static Jedis jedisClient;

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

    public static Jedis getJedis() {
        if (! isEnabled()) return null;

        if (getJedisClient() != null) {
            return getJedisClient();
        }

        Jedis jedis = new Jedis(getHost(), getPort());
        String auth = jedis.auth(getUsername(), getPassword());
        if (auth != null) {
            if (auth.equals("OK")) {
                MessageUtils.logInfo("Redis authenticated successfully.");
                setConnected(true);
                tpTicketListener = new TPTicketListener();
            } else {
                MessageUtils.logInfo("Redis authentication failed: " + auth);
            }
        }

        String pingResponse = jedis.ping();
        MessageUtils.logInfo("Redis ping response: " + pingResponse);

        setJedisClient(jedis);

        return getJedisClient();
    }

    public static void withJedis(Consumer<Jedis> consumer) {
        withJedis(consumer, true);
    }

    public static void withJedis(Consumer<Jedis> consumer, boolean silent) {
        if (isEnabled()) Optional.ofNullable(getJedis()).ifPresentOrElse(consumer, () -> {
            if (! silent) {
                MessageUtils.logWarning("Redis client is not initialized. Please check your Redis configuration.");
            }
        });
    }

    public static void sendMessage(RedisMessage message) {
        withJedis(j -> j.publish(message.getChannel(), message.getMessage()));
    }
}
