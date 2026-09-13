package singularity.logging;

import gg.drak.thebase.events.processing.BaseProcessor;
import lombok.Getter;
import lombok.Setter;
import singularity.events.server.CosmicLogPopEvent;
import singularity.events.server.ServerLogTextEvent;
import singularity.listeners.CosmicListener;
import singularity.logging.timers.LogPopTimer;
import singularity.utils.MessageUtils;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link CosmicListener} that accumulates {@link ServerLogTextEvent} instances in an
 * in-memory queue and periodically publishes them in bulk via a {@link CosmicLogPopEvent}.
 *
 * <p>The queue is flushed on a fixed schedule driven by {@link LogPopTimer}. Each flush
 * atomically drains the queue, fires a {@link CosmicLogPopEvent} with the collected
 * entries, and resets the ID counter.</p>
 *
 * <p>This class is a singleton; call {@link #init()} once during server startup to
 * register the listener and start the timer.</p>
 */
public class LogCollector extends CosmicListener {

    /**
     * Thread-safe ordered map of buffered log events, keyed by monotonically
     * increasing integer IDs assigned at insertion time.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<Integer, ServerLogTextEvent> logQueue = new ConcurrentSkipListMap<>();

    /**
     * Monotonically increasing counter used to assign unique IDs to queued log events.
     */
    @Getter @Setter
    private static AtomicInteger idCounter = new AtomicInteger(0);

    /**
     * Returns the next unique log entry ID and increments the internal counter.
     *
     * @return the next available integer ID
     */
    public static int getNextId() {
        return idCounter.getAndIncrement();
    }

    /**
     * Appends a {@link ServerLogTextEvent} to the queue, assigning it the next
     * available sequential ID.
     *
     * @param event the log event to enqueue
     */
    public static void addLog(ServerLogTextEvent event) {
        getLogQueue().put(getNextId(), event);
    }

    /**
     * Pops all logs from the queue and returns them as a map.
     * @return A map of log entries where the key is the log ID and the value is the log message.
     */
    public static ConcurrentSkipListMap<Integer, ServerLogTextEvent> getLogs() {
        ConcurrentSkipListMap<Integer, ServerLogTextEvent> logs = new ConcurrentSkipListMap<>(getLogQueue());

        resetQueue();

        return logs;
    }

    /**
     * Resets the log queue to an empty state and restarts the ID counter from zero.
     */
    public static void resetQueue() {
        clearLogs();
        setIdCounter(new AtomicInteger(0));
    }

    /**
     * Removes all entries from the log queue without resetting the ID counter.
     */
    public static void clearLogs() {
        logQueue.clear();
    }

    /**
     * Initialises the singleton {@code LogCollector} instance, registers it as a
     * listener, and starts the periodic {@link LogPopTimer}.
     *
     * <p>This method must be called exactly once during server startup before any
     * log events are expected.</p>
     */
    public static void init() {
        setInstance(new LogCollector());

        setLogPopTimer(new LogPopTimer());

        MessageUtils.logInfo("LogCollector initialized. Listening for log events.");
    }

    /**
     * Drains the current log queue and, if it is non-empty, fires a
     * {@link CosmicLogPopEvent} containing all buffered entries.
     *
     * <p>Called periodically by {@link LogPopTimer}.</p>
     */
    public static void popAndEvent() {
        ConcurrentSkipListMap<Integer, ServerLogTextEvent> logs = getLogs();
        if (! logs.isEmpty()) {
            CosmicLogPopEvent event = new CosmicLogPopEvent(logs).fire();
        }
    }

    /**
     * The singleton instance of this {@code LogCollector}.
     */
    @Getter @Setter
    private static LogCollector instance;

    /**
     * The timer responsible for periodically calling {@link #popAndEvent()}.
     */
    @Getter @Setter
    private static LogPopTimer logPopTimer;

    /**
     * Handles an incoming {@link ServerLogTextEvent} by appending it to the log queue.
     *
     * @param event the server log event to buffer
     */
    @BaseProcessor
    public void onServerLogTextEvent(ServerLogTextEvent event) {
        addLog(event);
    }
}
