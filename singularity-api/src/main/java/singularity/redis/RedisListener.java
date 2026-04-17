package singularity.redis;

import lombok.Getter;
import lombok.Setter;

/**
 * A concrete, prefix-aware Redis pub/sub listener that applies the configured
 * channel prefix to all subscribed channels.
 *
 * <p>This is the standard listener type for most Streamline use-cases. It extends
 * {@link WrappedRedisListener}, which in turn extends
 * {@link WrappedAbstractRedisListener}, so all channel names are automatically
 * wrapped via {@link OwnRedisClient#wrapKey(String)} before subscription.</p>
 *
 * <p>Override {@link AbstractRedisListener#onMessage(RedisMessage)} in a subclass
 * to add custom message-handling logic.</p>
 */
@Getter @Setter
public class RedisListener extends WrappedRedisListener {

    /**
     * Creates a prefix-aware Redis listener subscribed to the specified channels.
     *
     * @param identifier a unique name for this listener
     * @param channels   one or more Redis channel names to subscribe to (raw, without prefix)
     */
    public RedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }
}
