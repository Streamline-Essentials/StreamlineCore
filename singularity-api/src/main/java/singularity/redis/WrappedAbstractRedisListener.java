package singularity.redis;

import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

/**
 * An {@link AbstractRedisListener} extension that transparently applies the
 * configured Redis key prefix to every channel name before subscription.
 *
 * <p>All channel-related operations ({@link #getChannels()},
 * {@link #containsChannel(String)}, {@link #getChannelsSet()},
 * {@link #forEachChannel(Consumer)}, and {@link #register()}) operate on the
 * <em>wrapped</em> (prefixed) channel names returned by
 * {@link #getWrappedChannels()}. The raw channel names stored in the parent are
 * unchanged.</p>
 */
@Getter @Setter
public abstract class WrappedAbstractRedisListener extends AbstractRedisListener {

    /**
     * Constructs a prefix-aware Redis listener.
     *
     * @param identifier a unique name for this listener
     * @param channels   one or more Redis channel names to subscribe to (raw, without prefix)
     */
    public WrappedAbstractRedisListener(String identifier, String... channels) {
        super(identifier, channels);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Channel matching is performed against the <em>wrapped</em> (prefixed)
     * channel names, not the raw names stored in the parent.</p>
     *
     * @param channel the Redis channel name to check (expected to be prefixed)
     * @return {@code true} if the prefixed channel list contains {@code channel}
     *         or its {@code "~"}-prefixed alias
     */
    @Override
    public boolean containsChannel(String channel) {
        return Arrays.stream(getWrappedChannels()).anyMatch(c -> c.equals(channel) || c.equals("~" + channel));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the prefix-wrapped channel names rather than the raw names stored
     * in the parent.</p>
     *
     * @return array of prefix-wrapped channel names
     */
    @Override
    public String[] getChannels() {
        return getWrappedChannels();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a set built from the prefix-wrapped channel names.</p>
     *
     * @return a {@link ConcurrentSkipListSet} of prefix-wrapped channel names
     */
    @Override
    public ConcurrentSkipListSet<String> getChannelsSet() {
        return getWrappedChannelsSet();
    }

    /**
     * Returns the raw channel names from the parent, each transformed by
     * {@link OwnRedisClient#wrapKey(String)} to prepend the configured prefix.
     *
     * @return an array of prefix-wrapped channel names
     */
    public String[] getWrappedChannels() {
        return Arrays.stream(super.getChannels()).map(OwnRedisClient::wrapKey).toArray(String[]::new);
    }

    /**
     * Returns a thread-safe sorted set of the prefix-wrapped channel names.
     *
     * @return a {@link ConcurrentSkipListSet} containing all prefix-wrapped channel names
     */
    public ConcurrentSkipListSet<String> getWrappedChannelsSet() {
        return new ConcurrentSkipListSet<>(Arrays.asList(getWrappedChannels()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over the <em>prefix-wrapped</em> channel names.</p>
     *
     * @param consumer the action to perform for each prefix-wrapped channel name
     */
    @Override
    public void forEachChannel(Consumer<String> consumer) {
        getWrappedChannelsSet().forEach(consumer);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Subscribes to the <em>prefix-wrapped</em> channel names rather than the
     * raw names. If the connection is absent or closed, {@link #connect()} is
     * called first. Logs a success or warning message depending on the outcome.</p>
     */
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
