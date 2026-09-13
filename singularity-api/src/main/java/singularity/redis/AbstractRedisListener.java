package singularity.redis;

import gg.drak.thebase.async.AsyncUtils;
import gg.drak.thebase.objects.Identifiable;
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

/**
 * Base implementation of a Lettuce {@link RedisPubSubListener} that handles channel
 * subscription and message dispatching for the Streamline Redis subsystem.
 *
 * <p>Subclasses override {@link #onMessage(RedisMessage)} to react to incoming
 * pub/sub messages. After construction, call {@link #registerAndLoad()} (or
 * {@link #register()} then {@link #load()} separately) to activate the listener.</p>
 */
@Getter @Setter
public abstract class AbstractRedisListener implements RedisPubSubListener<String, String>, Identifiable {

    /**
     * The unique identifier for this listener, used by {@link RedisHandler} to
     * track and de-duplicate registrations.
     */
    private String identifier;

    /**
     * The Redis pub/sub channels this listener is subscribed to.
     * A channel prefixed with {@code "~"} is treated as an alias match.
     */
    private String[] channels;

    /**
     * The stateful Lettuce pub/sub connection used to subscribe and receive messages.
     */
    private StatefulRedisPubSubConnection<String, String> connection;

    /**
     * Constructs a new listener and immediately opens a pub/sub connection.
     *
     * @param identifier a unique name for this listener (used for logging and registry lookups)
     * @param channels   one or more Redis channel names to subscribe to
     */
    public AbstractRedisListener(String identifier, String... channels) {
        this.identifier = identifier;
        this.channels = channels;

        connect();
    }

    /**
     * Opens a new pub/sub connection from {@link OwnRedisClient} and stores it in
     * {@link #connection}.
     */
    public void connect() {
        this.connection = OwnRedisClient.getClient().connectPubSub();
    }

    /**
     * Returns {@code true} if {@code channel} is one of the channels this listener
     * is registered for (including alias variants prefixed with {@code "~"}).
     *
     * @param channel the Redis channel name to check
     * @return {@code true} if the channel is handled by this listener
     */
    public boolean containsChannel(String channel) {
        return Arrays.stream(channels).anyMatch(c -> c.equals(channel) || c.equals("~" + channel));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #onMessage(String, String)} after filtering out
     * channels that this listener does not handle.</p>
     */
    @Override
    public void message(String channel, String message) {
        if (! containsChannel(channel)) return;
        onMessage(channel, message);
    }

    /**
     * Wraps {@code channel} and {@code message} into a {@link RedisMessage} and
     * delegates to {@link #onMessage(RedisMessage)}.
     *
     * @param channel the Redis channel the message was published on
     * @param message the raw message payload
     */
    public void onMessage(String channel, String message) {
        onMessage(new RedisMessage(channel, message));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #onMessage(String, String)} after filtering out
     * channels that this listener does not handle. The {@code pattern} argument
     * is ignored; only {@code channel} is checked.</p>
     */
    @Override
    public void message(String pattern, String channel, String message) {
        if (! containsChannel(channel)) return;
        onMessage(channel, message);
    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op by default.</p>
     */
    @Override
    public void subscribed(String channel, long count) {

    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op by default.</p>
     */
    @Override
    public void psubscribed(String pattern, long count) {

    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op by default.</p>
     */
    @Override
    public void unsubscribed(String channel, long count) {

    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op by default.</p>
     */
    @Override
    public void punsubscribed(String pattern, long count) {

    }

    /**
     * Called when a message arrives on one of the subscribed channels.
     * Override this method to implement message-handling logic.
     *
     * @param redisMessage the received message, including channel and payload
     */
    public void onMessage(RedisMessage redisMessage) {
        // do nothing by default
    }

    /**
     * Returns a snapshot of the subscribed channels as a thread-safe sorted set.
     *
     * @return a {@link ConcurrentSkipListSet} containing all channel names
     */
    public ConcurrentSkipListSet<String> getChannelsSet() {
        return new ConcurrentSkipListSet<>(Arrays.asList(channels));
    }

    /**
     * Iterates over all subscribed channels and applies the given {@code consumer}.
     *
     * @param consumer the action to perform for each channel name
     */
    public void forEachChannel(Consumer<String> consumer) {
        getChannelsSet().forEach(consumer);
    }

    /**
     * Registers this listener with the Lettuce pub/sub connection.
     *
     * <p>If the connection is absent or closed, {@link #connect()} is called first.
     * Logs a success or warning message depending on the outcome.</p>
     */
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

    /**
     * Returns {@code true} if the underlying pub/sub connection is currently open.
     *
     * @return {@code true} if connected, {@code false} otherwise
     */
    public boolean isConnected() {
        return getConnection().isOpen();
    }

    /**
     * Registers this listener in the {@link RedisHandler} registry.
     * Any previously loaded listener with the same identifier is replaced.
     */
    public void load() {
        RedisHandler.load(this);
    }

    /**
     * Removes this listener from the {@link RedisHandler} registry.
     */
    public void unload() {
        RedisHandler.unload(this);
    }

    /**
     * Returns {@code true} if this listener is currently tracked by the
     * {@link RedisHandler} registry.
     *
     * @return {@code true} if loaded, {@code false} otherwise
     */
    public boolean isLoaded() {
        return RedisHandler.isLoaded(this);
    }

    /**
     * Registers this listener with Redis asynchronously and adds it to the
     * {@link RedisHandler} registry.
     *
     * <p>{@link #register()} is executed on a background thread; {@link #load()}
     * is called immediately on the calling thread.</p>
     */
    public void registerAndLoad() {
        AsyncUtils.executeAsync(this::register);
        load();
    }
}
