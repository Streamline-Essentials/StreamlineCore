package singularity.redis;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RedisListener extends WrappedRedisListener {
    public RedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }
}
