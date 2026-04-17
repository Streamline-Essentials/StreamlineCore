package singularity.messages.answered;

import lombok.Getter;
import lombok.Setter;
import singularity.messages.events.AnsweredMessageEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.messages.proxied.ProxiedMessageManager;
import singularity.scheduler.BaseRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * Represents a proxied message that expects a correlated reply from the receiving end.
 *
 * <p>A {@code ReturnableMessage} wraps a {@link ProxiedMessage} payload and embeds a
 * randomly generated {@link #answerKey} into it (under the {@link #key} field name).
 * The remote side echoes this key back in its reply, allowing {@link #tryAnswer} to
 * correlate incoming messages without a shared state channel.</p>
 *
 * <p>Once an answer is accepted the message fires the registered {@link Consumer}
 * callbacks, raises an {@link AnsweredMessageEvent}, and then removes itself from
 * {@link ProxiedMessageManager}. A {@link TimeoutTimer} monitors the message and
 * forcibly cleans it up via {@link #kill()} if no reply arrives in time.</p>
 *
 * <p>Subclasses may override {@link #fire(ProxiedMessage)} to react to the answer
 * within the same object rather than registering a separate consumer.</p>
 */
@Setter
public class ReturnableMessage implements Comparable<ReturnableMessage> {

    /**
     * The payload field name under which the unique answer key is written.
     * The remote end must echo this field back in its reply message.
     */
    @Getter
    private static final String key = "{{key}}";

    /**
     * Maximum number of ticks to wait before the message is considered timed out.
     */
    @Getter
    private static final int timeoutTicks = 60;

    /**
     * Interval in ticks at which the {@link TimeoutTimer} checks whether the
     * returnable message has been answered or should be killed.
     */
    @Getter
    private static final int testingTicks = 1;

    /**
     * The randomly generated UUID string that uniquely identifies this returnable
     * message and is used to correlate the incoming reply.
     */
    @Getter
    private String answerKey;

    /**
     * The outgoing {@link ProxiedMessage} that carries the request data and the
     * embedded {@link #answerKey}.
     */
    @Getter
    private ProxiedMessage payload;

    /**
     * Whether {@link #fire(ProxiedMessage)} has been called after an answer was accepted.
     */
    @Getter
    private boolean fired = false;

    /**
     * Whether all registered {@link Consumer} callbacks have been invoked after
     * an answer was accepted.
     */
    @Getter
    private boolean called = false;

    /**
     * The timer that monitors this message for a timeout and cleans it up if
     * no answer arrives within {@link #timeoutTicks} ticks.
     */
    @Getter
    private TimeoutTimer timeoutTimer;

    /**
     * Ordered map of callbacks to invoke when an answer is received. Keys are
     * 1-based sequential integers assigned at registration time.
     */
    @Getter
    private ConcurrentSkipListMap<Integer, Consumer<ProxiedMessage>> registeredEvents = new ConcurrentSkipListMap<>();

    /**
     * The reply message received from the remote side, or empty if not yet answered.
     */
    @Getter
    private Optional<ProxiedMessage> answer = Optional.empty();

    /**
     * Creates a new {@code ReturnableMessage} wrapping the given payload, optionally
     * sending it immediately via {@link ProxiedMessageManager}.
     *
     * @param payload the message to send and await a reply for
     * @param send    {@code true} to send the message immediately; {@code false} to
     *                defer sending to a later call to {@link #send()}
     */
    public ReturnableMessage(ProxiedMessage payload, boolean send) {
        this.payload = payload;
        this.answerKey = UUID.randomUUID().toString();
        getPayload().write(getKey(), getAnswerKey());
        setTimeoutTimer(new TimeoutTimer(this));
        if (send) send();
    }

    /**
     * Creates a new {@code ReturnableMessage} wrapping the given payload and sends
     * it immediately.
     *
     * @param payload the message to send and await a reply for
     */
    public ReturnableMessage(ProxiedMessage payload) {
        this(payload, true);
    }

    /**
     * Registers this message with {@link ProxiedMessageManager} and dispatches
     * the payload to the remote side.
     */
    public void send() {
        ProxiedMessageManager.sendReturnable(this);
    }

    /**
     * Attempts to match an incoming message to this returnable by comparing the
     * embedded answer key. If matched, {@link #acceptAnswer(ProxiedMessage)} is called.
     *
     * @param answer the candidate reply message
     * @return {@code true} if the message matched and was accepted; {@code false} otherwise
     */
    public boolean tryAnswer(ProxiedMessage answer) {
        if (answer.getString(getKey()).equals(getAnswerKey())) {
            acceptAnswer(answer);
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if a reply has been received and accepted.
     *
     * @return {@code true} when an answer is present
     */
    public boolean isAnswered() {
        return answer.isPresent();
    }

    /**
     * Stores the reply, fires an {@link AnsweredMessageEvent}, invokes
     * {@link #fire(ProxiedMessage)}, calls all registered consumers, and then
     * disposes of this returnable message.
     *
     * <p>If the {@link AnsweredMessageEvent} is cancelled no further processing
     * occurs and the message remains registered.</p>
     *
     * @param message the reply message from the remote side
     */
    public void acceptAnswer(ProxiedMessage message) {
        answer = Optional.of(message);

        AnsweredMessageEvent event = new AnsweredMessageEvent(this, message).fire();
        if (event.isCancelled()) {
            return;
        }

        fire(message);
        setFired(true);

        callEvents(message);
        setCalled(true);

        killNow();
    }

    /**
     * Extension point called with the accepted reply message before consumer
     * callbacks are invoked. The default implementation does nothing; subclasses
     * may override to perform inline processing.
     *
     * @param message the accepted reply message
     */
    public void fire(ProxiedMessage message) {
        // do nothing.
    }

    /**
     * Requests that {@link ProxiedMessageManager} remove this message from the
     * active returnable registry (subject to timeout logic).
     *
     * @return {@code true} if the message was successfully removed
     */
    public boolean kill() {
        return ProxiedMessageManager.killReturnable(this);
    }

    /**
     * Cancels the {@link TimeoutTimer} and immediately unloads this message from
     * {@link ProxiedMessageManager} without going through timeout logic.
     */
    public void killNow() {
        getTimeoutTimer().cancel();
        ProxiedMessageManager.unloadReturnableMessage(this);
    }

    /**
     * Registers a callback to be invoked when an answer is received and accepted.
     *
     * @param consumer a {@link Consumer} that accepts the reply {@link ProxiedMessage}
     */
    public void registerEventCall(Consumer<ProxiedMessage> consumer) {
        getRegisteredEvents().put(getRegisteredEvents().size() + 1, consumer);
    }

    /**
     * Invokes all registered {@link Consumer} callbacks in insertion order,
     * passing the accepted reply message to each one.
     *
     * <p>Exceptions thrown by individual consumers are printed to stderr but do
     * not prevent subsequent consumers from being called.</p>
     *
     * @param accepted the accepted reply message
     */
    public void callEvents(ProxiedMessage accepted) {
        getRegisteredEvents().forEach((integer, consumer) -> {
            try {
                consumer.accept(accepted);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Compares this message to another based on when the underlying payload was
     * created, enabling chronological ordering of returnable messages.
     *
     * @param o the other {@code ReturnableMessage} to compare to
     * @return a negative integer, zero, or positive integer as this message's
     *         payload was created before, at the same time as, or after {@code o}'s
     */
    @Override
    public int compareTo(@NotNull ReturnableMessage o) {
        return Long.compare(getPayload().getGottenAt().getTime(), o.getPayload().getGottenAt().getTime());
    }

    /**
     * A periodic task that monitors a {@link ReturnableMessage} and removes it from
     * the active registry if it has timed out (as determined by
     * {@link ProxiedMessageManager#killReturnable(ReturnableMessage)}).
     *
     * <p>The timer cancels itself once the kill succeeds.</p>
     */
    @Setter
    @Getter
    public static class TimeoutTimer extends BaseRunnable {

        /**
         * The {@link ReturnableMessage} that this timer is monitoring.
         */
        private ReturnableMessage parent;

        /**
         * Creates a new {@code TimeoutTimer} for the given returnable message,
         * using {@link ReturnableMessage#getTestingTicks()} for both the initial
         * delay and the repeat interval.
         *
         * @param returnableMessage the message to monitor
         */
        public TimeoutTimer(ReturnableMessage returnableMessage) {
            super(getTestingTicks(), getTestingTicks());
            setParent(returnableMessage);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Attempts to kill the parent returnable message via
         * {@link ReturnableMessage#kill()}. Cancels this timer once the kill
         * is confirmed.</p>
         */
        @Override
        public void run() {
            if (getParent().kill()) cancel();
        }
    }
}
