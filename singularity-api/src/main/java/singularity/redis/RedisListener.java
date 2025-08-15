package singularity.redis;

import gg.drak.thebase.async.AsyncUtils;
import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RedisListener extends AbstractRedisListener implements Identifiable {

    public RedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }

    public void load() {
        RedisHandler.load(this);
    }

    public void unload() {
        RedisHandler.unload(this);
    }

    public boolean isLoaded() {
        return RedisHandler.isLoaded(this);
    }

    public void registerAndLoad() {
        AsyncUtils.executeAsync(this::register);
        load();
    }
}
