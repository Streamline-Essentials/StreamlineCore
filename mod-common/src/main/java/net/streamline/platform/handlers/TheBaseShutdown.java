package net.streamline.platform.handlers;

import gg.drak.thebase.async.AsyncTask;
import gg.drak.thebase.async.AsyncUtils;

import java.util.ArrayList;

/**
 * Stops every task TheBase still has queued once the server is gone.
 *
 * <p>Each TheBase {@link AsyncTask} runs on a {@code javax.swing.Timer}, and a running Swing
 * timer keeps AWT's non-daemon event-dispatch thread alive. Bukkit and the proxies end with
 * {@code System.exit}, so it never matters there; a mod-loader dedicated server instead
 * waits for every non-daemon thread and would never exit.</p>
 */
public final class TheBaseShutdown {

    private TheBaseShutdown() {}

    public static void stopQueuedTasks() {
        for (AsyncTask task : new ArrayList<>(AsyncUtils.getQueuedTasks())) {
            task.stop();
        }
        AsyncUtils.getQueuedTasks().clear();
    }
}
