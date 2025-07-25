package singularity.logging.timers;

import gg.drak.thebase.async.AsyncTask;
import singularity.logging.LogCollector;

public class LogPopTimer extends AsyncTask {
    public LogPopTimer() {
        super(LogPopTimer::runTask, 0, 20 * 5); // Runs every 5 seconds
    }

    public static void runTask(AsyncTask task) {
        LogCollector.popAndEvent();
    }
}
