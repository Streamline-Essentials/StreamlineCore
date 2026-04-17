package singularity.redis;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Redis pub/sub message consisting of a channel name and a payload
 * string.
 *
 * <p>Provides convenience methods for publishing the message, serialising it to a
 * simple {@code channel:message} string, and obtaining the prefix-wrapped channel
 * name used during transmission.</p>
 */
@Getter @Setter
public class RedisMessage {

    /**
     * The Redis channel this message belongs to (raw, without prefix).
     */
    private String channel;

    /**
     * The string payload of the message.
     */
    private String message;

    /**
     * Constructs a new {@code RedisMessage}.
     *
     * @param channel the Redis channel (raw, without prefix)
     * @param message the message payload
     */
    public RedisMessage(String channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    /**
     * Publishes this message via {@link OwnRedisClient#sendMessage(RedisMessage)}.
     * The channel is wrapped with the configured prefix before publishing.
     */
    public void send() {
        OwnRedisClient.sendMessage(this);
    }

    /**
     * Returns a compact string representation in the form {@code "channel:message"}.
     *
     * @return the channel and message joined by {@code ":"}
     */
    public String asString() {
        return getChannel() + ":" + getMessage();
    }

    /**
     * Returns the channel name wrapped with the configured prefix.
     *
     * @return the prefix-wrapped channel name as used when publishing
     * @see OwnRedisClient#wrapKey(String)
     */
    public String wrappedChannel() {
        return OwnRedisClient.wrapKey(getChannel());
    }

    /**
     * Returns the full {@link #asString()} representation wrapped with the
     * configured prefix.
     *
     * @return the prefix-wrapped {@code "channel:message"} string
     * @see OwnRedisClient#wrapKey(String)
     */
    public String wrap() {
        return OwnRedisClient.wrapKey(asString());
    }
}
