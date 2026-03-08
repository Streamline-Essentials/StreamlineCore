package singularity.redis;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WrappedRedisListener extends WrappedAbstractRedisListener {
    public WrappedRedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }
}
