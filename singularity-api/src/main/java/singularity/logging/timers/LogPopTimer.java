package singularity.logging.timers;

import gg.drak.thebase.async.AsyncTask;
import singularity.logging.LogCollector;

/**
 * An {@link AsyncTask} that periodically drains the {@link LogCollector} queue
 * and fires a bulk log-pop event.
 *
 * <p>The task runs every 5 seconds (100 ticks at 20 ticks per second) with no
 * initial delay. It is queued and started immediately upon construction.</p>
 */
public class LogPopTimer extends AsyncTask {

    /**
     * Creates, queues, and starts the periodic log-pop task.
     *
     * <p>The task fires every {@code 20 * 5} ticks (5 seconds) with no initial delay.</p>
     */
    public LogPopTimer() {
        super(LogPopTimer::runTask, 0, 20 * 5); // Runs every 5 seconds
        queue();
        start();
    }

    /**
     * Task body executed on each tick interval. Delegates to
     * {@link LogCollector#popAndEvent()} to flush buffered log entries.
     *
     * @param task the running {@link AsyncTask} instance (unused)
     */
    public static void runTask(AsyncTask task) {
        LogCollector.popAndEvent();
    }
}
