package host.plas.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.SimpleLogger;
import net.streamline.api.SLAPI;

public class RedisInfoConfig extends SimpleConfiguration {
    public RedisInfoConfig() {
        super("redis-info.yml", SimpleLogger.getInstance(), true);
    }

    @Override
    public void init() {
        isUseRedis();
    }

    public boolean isUseRedis() {
        reloadResource();

        return getOrSetDefault("use-redis", true);
    }
}
