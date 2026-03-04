package singularity.redis;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DefaultRedisListener extends AbstractRedisListener {
    public DefaultRedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }
}
