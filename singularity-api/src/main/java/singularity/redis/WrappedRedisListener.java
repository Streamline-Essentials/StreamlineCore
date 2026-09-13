package singularity.redis;

import lombok.Getter;
import lombok.Setter;

/**
 * A concrete, prefix-aware Redis pub/sub listener that adds no additional behaviour
 * beyond what {@link WrappedAbstractRedisListener} provides.
 *
 * <p>This class exists as an instantiable middle layer in the listener hierarchy:
 * {@link RedisListener} extends this class, while custom listeners that need
 * prefix-wrapping but prefer a shallower hierarchy can extend this directly.</p>
 *
 * <p>Override {@link AbstractRedisListener#onMessage(RedisMessage)} to handle
 * incoming messages.</p>
 */
@Getter @Setter
public class WrappedRedisListener extends WrappedAbstractRedisListener {

    /**
     * Creates a prefix-aware Redis listener subscribed to the specified channels.
     *
     * @param identifier a unique name for this listener
     * @param channels   one or more Redis channel names to subscribe to (raw, without prefix)
     */
    public WrappedRedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }
}
