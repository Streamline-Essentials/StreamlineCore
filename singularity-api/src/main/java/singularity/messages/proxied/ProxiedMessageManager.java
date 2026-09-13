package singularity.messages.proxied;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gg.drak.thebase.utils.MathUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.command.CommandMessageBuilder;
import singularity.messages.builders.*;
import singularity.messages.answered.ReturnableMessage;
import singularity.objects.SingleSet;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.BaseRunnable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Central coordinator for inbound and outbound {@link ProxiedMessage} traffic.
 *
 * <p>Responsibilities include:
 * <ul>
 *   <li>Maintaining a short-lived Caffeine cache of in-flight
 *       {@link ReturnableMessage} instances awaiting a response.</li>
 *   <li>Dispatching inbound messages to the correct sub-channel handler.</li>
 *   <li>Holding a pending-message queue for messages that could not be
 *       delivered immediately and must be retried.</li>
 *   <li>Running a periodic {@link PendingTicker} that retries pending
 *       messages and evicts stale ones.</li>
 * </ul>
 */
public class ProxiedMessageManager {

    /**
     * The periodic task responsible for retrying messages in the pending
     * queue and removing those that have exceeded the maximum retry window.
     */
    @Getter @Setter
    private static PendingTicker pendingTicker;

    /**
     * Initialises the manager by starting the {@link PendingTicker}.
     * Must be called once during platform startup.
     */
    public static void init() {
        setPendingTicker(new PendingTicker());
    }

    /**
     * Short-lived cache of {@link ReturnableMessage} instances keyed by the
     * timestamp of their payload.  Entries expire two seconds after insertion,
     * acting as the round-trip timeout window.
     */
    @Getter @Setter
    private static Cache<Date, ReturnableMessage> loadedReturnableMessaged = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(2))
            .build();

    /**
     * Registers a {@link ReturnableMessage} in the in-flight cache so that
     * an incoming reply can be matched against it.
     *
     * @param returnableMessage the outbound returnable message to track
     */
    public static void loadReturnableMessage(ReturnableMessage returnableMessage) {
        getLoadedReturnableMessaged().put(returnableMessage.getPayload().getGottenAt(), returnableMessage);
    }

    /**
     * Cancels the timeout timer of the given {@link ReturnableMessage} (if
     * still active) and removes it from the in-flight cache.
     *
     * @param returnableMessage the message to remove from tracking
     */
    public static void unloadReturnableMessage(ReturnableMessage returnableMessage) {
        if (returnableMessage.getTimeoutTimer() != null) {
            returnableMessage.getTimeoutTimer().cancel();
        }
        getLoadedReturnableMessaged().invalidate(returnableMessage.getPayload().getGottenAt());
    }

    /**
     * Registers and immediately sends a {@link ReturnableMessage}, placing it
     * in the in-flight cache before dispatching its payload.
     *
     * @param returnableMessage the message to send
     */
    public static void sendReturnable(ReturnableMessage returnableMessage) {
        loadReturnableMessage(returnableMessage);
        returnableMessage.getPayload().send();
    }

    /**
     * Removes a {@link ReturnableMessage} from the in-flight cache if it has
     * been answered or if its timeout window has elapsed.
     *
     * @param returnableMessage the message to evaluate for removal
     * @return {@code true} if the message was removed; {@code false} if it is
     *         still within the timeout window and should be kept
     */
    public static boolean killReturnable(ReturnableMessage returnableMessage) {
        if (! returnableMessage.isAnswered()) {
            if (! MathUtils.isDateOlderThan(returnableMessage.getPayload().getGottenAt(), ReturnableMessage.getTimeoutTicks() / 20, ChronoUnit.SECONDS)) return false;
        }
        unloadReturnableMessage(returnableMessage);
        return true;
    }

    /**
     * Entry point for all inbound proxied messages.  If the message looks like
     * a returnable reply it is matched against the in-flight cache
     * asynchronously; then the message is forwarded to {@link #handle} for
     * sub-channel routing.
     *
     * @param proxiedMessage the received message
     */
    public static void onProxiedMessageReceived(ProxiedMessage proxiedMessage) {
        if (proxiedMessage.isReturnableLike()) {
            CompletableFuture.runAsync(() -> {
                getLoadedReturnableMessaged().asMap().forEach((date, returnableMessage) -> {
                    returnableMessage.tryAnswer(proxiedMessage);
                });
            });
        }

        CompletableFuture.runAsync(() -> {
            handle(proxiedMessage);
        });
    }

    /**
     * Routes an inbound message to the appropriate sub-channel handler based
     * on the message's main channel and sub-channel fields.
     *
     * <p>Handled sub-channels (on the Singularity API channel) include
     * resource-pack delivery, command execution, proxy-side placeholder parsing,
     * player location queries, server connection requests, and server name
     * lookups.  Unrecognised sub-channels are silently ignored.
     *
     * @param proxiedMessage the message to route and handle
     */
    public static void handle(ProxiedMessage proxiedMessage) {
        if (proxiedMessage.getMainChannel().equals(Singularity.getApiChannel())) {
            if (proxiedMessage.getSubChannel().equals(ResourcePackMessageBuilder.getSubChannel())) {
                SingleSet<String, CosmicResourcePack> set = ResourcePackMessageBuilder.unbuild(proxiedMessage);
                CosmicResourcePack resourcePack = set.getValue();

                Singularity.getInstance().getPlatform().sendResourcePack(resourcePack, set.getKey());
                return;
            }
            if (proxiedMessage.getSubChannel().equals(CommandMessageBuilder.getSubChannel())) {
                CommandMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(ProxyParseMessageBuilder.getSubChannel())) {
                ProxyParseMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(PlayerLocationMessageBuilder.getSubChannel())) {
                PlayerLocationMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
                ServerConnectMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(ServerNameMessageBuilder.getSubChannel())) {
                ServerNameMessageBuilder.handle(proxiedMessage);
                return;
            }
        }
    }

    /**
     * Periodic task that iterates the pending-message queue on every tick,
     * attempting to resend each queued message and evicting entries older than
     * ten minutes.
     */
    public static class PendingTicker extends BaseRunnable {

        /**
         * Constructs a new ticker with an initial delay and period of 40 ticks
         * (two seconds).
         */
        public PendingTicker() {
            super(40, 40);
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            tickPendingMessages();
        }
    }

    /**
     * Queue of messages that could not be delivered immediately, keyed by
     * the time they were originally received so they are processed in order.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<Date, ProxiedMessage> pendingMessages = new ConcurrentSkipListMap<>();

    /**
     * Adds a message to the pending queue so that it will be retried on the
     * next {@link PendingTicker} cycle.
     *
     * @param proxiedMessage the message to enqueue
     */
    public static void pendMessage(ProxiedMessage proxiedMessage) {
        getPendingMessages().put(proxiedMessage.getGottenAt(), proxiedMessage);
    }

    /**
     * Removes a message from the pending queue.
     *
     * @param proxiedMessage the message to dequeue
     */
    public static void unpendMessage(ProxiedMessage proxiedMessage) {
        getPendingMessages().remove(proxiedMessage.getGottenAt());
    }

    /**
     * Iterates the entire pending queue, resending each message via the
     * platform's proxy messenger and evicting any entry that is older than
     * ten minutes.
     */
    public static void tickPendingMessages() {
        getPendingMessages().forEach((date, proxiedMessage) -> {
            Singularity.getInstance().getProxyMessenger().sendMessage(proxiedMessage);
            if (MathUtils.isDateOlderThan(date, 10, ChronoUnit.MINUTES)) unpendMessage(proxiedMessage);
        });
    }
}
