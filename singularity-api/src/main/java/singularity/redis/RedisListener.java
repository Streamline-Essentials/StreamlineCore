package singularity.redis;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.JedisPubSub;
import singularity.utils.MessageUtils;

@Getter @Setter
public abstract class RedisListener extends JedisPubSub implements Identifiable {
    private String identifier;

    public RedisListener(String identifier) {
        this.identifier = identifier;

        registerAndLoad();

        MessageUtils.logInfo("&cRedisListener &fregistered: &d" + identifier);
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

    public void register() {
        RedisClient.withJedis(j -> j.subscribe(this, getChannelsArray()));
    }

    public void registerAndLoad() {
        register();
        load();
    }

    abstract String[] getChannelsArray();
}
