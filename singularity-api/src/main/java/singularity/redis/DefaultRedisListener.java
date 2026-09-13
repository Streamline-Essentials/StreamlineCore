package singularity.redis;

import lombok.Getter;
import lombok.Setter;

/**
 * A concrete, no-op {@link AbstractRedisListener} provided as a convenience base
 * for callers that prefer inheritance over anonymous subclasses.
 *
 * <p>All behaviour is inherited from {@link AbstractRedisListener}. Override
 * {@link AbstractRedisListener#onMessage(RedisMessage)} to add custom handling.</p>
 */
@Getter @Setter
public class DefaultRedisListener extends AbstractRedisListener {

    /**
     * Creates a default Redis listener subscribed to the specified channels.
     *
     * @param identifier a unique name for this listener
     * @param channels   one or more Redis channel names to subscribe to
     */
    public DefaultRedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }
}
