package host.plas.redis;

import host.plas.SimpleLogger;
import lombok.Getter;
import lombok.Setter;
import singularity.redis.RedisListener;

@Getter @Setter
public class LoggerRedisListener extends RedisListener {
    public LoggerRedisListener() {
        super(SimpleLogger.getInstance().getIdentifier());
    }
}
