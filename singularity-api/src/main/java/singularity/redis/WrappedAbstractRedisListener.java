package singularity.redis;

import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

@Getter @Setter
public abstract class WrappedAbstractRedisListener extends AbstractRedisListener {
    public WrappedAbstractRedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }

    @Override
    public boolean containsChannel(String channel) {
        return Arrays.stream(getWrappedChannels()).anyMatch(c -> c.equals(channel) || c.equals("~" + channel));
    }

    @Override
    public String[] getChannels() {
        return getWrappedChannels();
    }

    @Override
    public ConcurrentSkipListSet<String> getChannelsSet() {
        return getWrappedChannelsSet();
    }

    public String[] getWrappedChannels() {
        return Arrays.stream(super.getChannels()).map(OwnRedisClient::wrapKey).toArray(String[]::new);
    }

    public ConcurrentSkipListSet<String> getWrappedChannelsSet() {
        return new ConcurrentSkipListSet<>(Arrays.asList(getWrappedChannels()));
    }

    @Override
    public void forEachChannel(Consumer<String> consumer) {
        getWrappedChannelsSet().forEach(consumer);
    }

    @Override
    public void register() {
        try {
            if (getConnection() == null || ! isConnected()) connect();

            RedisPubSubAsyncCommands<String, String> r = getConnection().async();

            r.getStatefulConnection().addListener(this);
            r.subscribe(getWrappedChannels());

            MessageUtils.logInfo("&cRedisListener &fregistered: &d" + getIdentifier());
        } catch (Throwable e) {
            MessageUtils.logWarning("&cRedisListener &ferror registering: &d" + getIdentifier() + " &7-> &f" + e.getMessage());
            e.printStackTrace();
        }
    }
}
