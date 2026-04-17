package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;

/**
 * Fired when one or more server log entries are "popped" (dequeued) from the
 * internal log buffer and are ready for downstream processing.
 *
 * <p>Each entry in the map is keyed by an integer ordering index and holds the
 * corresponding {@link ServerLogTextEvent} that was captured at log time.</p>
 */
@Getter @Setter
public class CosmicLogPopEvent extends CosmicEvent {

    /**
     * The ordered collection of log entries that were popped from the buffer.
     * Keys represent the sequential position of each entry; values carry the
     * raw log text and intent.
     */
    private final ConcurrentSkipListMap<Integer, ServerLogTextEvent> poppedLogs;

    /**
     * Constructs a new {@code CosmicLogPopEvent} with the supplied map of
     * dequeued log entries.
     *
     * @param poppedLogs an ordered map of log index to {@link ServerLogTextEvent};
     *                   must not be {@code null}
     */
    public CosmicLogPopEvent(ConcurrentSkipListMap<Integer, ServerLogTextEvent> poppedLogs) {
        this.poppedLogs = poppedLogs;
    }

    /**
     * Iterates over every popped log entry in ascending key order and passes
     * each index/event pair to the given consumer.
     *
     * @param consumer a {@link BiConsumer} that receives the log index and the
     *                 associated {@link ServerLogTextEvent}; must not be {@code null}
     */
    public void forEachLog(BiConsumer<Integer, ServerLogTextEvent> consumer) {
        poppedLogs.forEach(consumer);
    }
}
