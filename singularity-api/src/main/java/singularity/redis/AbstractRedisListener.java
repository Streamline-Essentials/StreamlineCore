package singularity.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

@Getter @Setter
public abstract class AbstractRedisListener implements RedisPubSubListener<String, String> {
    private String identifier;
    private String[] channels;

    private StatefulRedisPubSubConnection<String, String> connection;

    public AbstractRedisListener(String identifier, String... channels) {
        this.identifier = identifier;
        this.channels = channels;

        connect();
    }

    public void connect() {
        this.connection = OwnRedisClient.getClient().connectPubSub();
    }

    public boolean containsChannel(String channel) {
        return Arrays.stream(channels).anyMatch(c -> c.equals(channel) || c.equals("~" + channel));
    }

    @Override
    public void message(String channel, String message) {
        if (! containsChannel(channel)) return;
        onMessage(channel, message);
    }

    public void onMessage(String channel, String message) {
        onMessage(new RedisMessage(channel, message));
    }

    @Override
    public void message(String pattern, String channel, String message) {
        if (! containsChannel(channel)) return;
        onMessage(channel, message);
    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }

    public void onMessage(RedisMessage redisMessage) {
        // do nothing by default
    }

    public ConcurrentSkipListSet<String> getChannelsSet() {
        return new ConcurrentSkipListSet<>(Arrays.asList(channels));
    }

    public void forEachChannel(Consumer<String> consumer) {
        getChannelsSet().forEach(consumer);
    }

    public void register() {
        try {
            if (getConnection() == null || ! isConnected()) connect();

            RedisPubSubAsyncCommands<String, String> r = getConnection().async();

            r.getStatefulConnection().addListener(this);
            r.subscribe(getChannels());

            MessageUtils.logInfo("&cRedisListener &fregistered: &d" + getIdentifier());
        } catch (Throwable e) {
            MessageUtils.logWarning("&cRedisListener &ferror registering: &d" + getIdentifier() + " &7-> &f" + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return getConnection().isOpen();
    }
}
