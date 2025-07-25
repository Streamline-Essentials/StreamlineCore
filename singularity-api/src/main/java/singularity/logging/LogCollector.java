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

public class LogCollector extends CosmicListener {
    @Getter @Setter
    private static ConcurrentSkipListMap<Integer, ServerLogTextEvent> logQueue = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static AtomicInteger idCounter = new AtomicInteger(0);

    public static int getNextId() {
        return idCounter.getAndIncrement();
    }

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

    public static void resetQueue() {
        clearLogs();
        setIdCounter(new AtomicInteger(0));
    }

    public static void clearLogs() {
        logQueue.clear();
    }

    public static void init() {
        setInstance(new LogCollector());

        setLogPopTimer(new LogPopTimer());

        MessageUtils.logInfo("LogCollector initialized. Listening for log events.");
    }

    public static void popAndEvent() {
        ConcurrentSkipListMap<Integer, ServerLogTextEvent> logs = getLogs();
        if (! logs.isEmpty()) {
            CosmicLogPopEvent event = new CosmicLogPopEvent(logs).fire();
        }
    }

    @Getter @Setter
    private static LogCollector instance;
    @Getter @Setter
    private static LogPopTimer logPopTimer;

    @BaseProcessor
    public void onServerLogTextEvent(ServerLogTextEvent event) {
        addLog(event);
    }
}
