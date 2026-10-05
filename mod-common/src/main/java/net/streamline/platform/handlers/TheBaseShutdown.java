package net.streamline.platform.handlers;

import gg.drak.thebase.async.AsyncTask;
import gg.drak.thebase.async.AsyncUtils;
import singularity.utils.MessageUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Stops every task TheBase still has queued once the server is gone.
 *
 * <p>Each TheBase {@link AsyncTask} runs on a {@code javax.swing.Timer}, and a running Swing
 * timer keeps AWT's non-daemon event-dispatch thread alive. Bukkit and the proxies end with
 * {@code System.exit}, so it never matters there; a mod-loader dedicated server instead
 * waits for every non-daemon thread and would never exit. TheBase (1.1.0) exposes no way to
 * cancel all of its tasks, so its private task set is read reflectively.</p>
 */
public final class TheBaseShutdown {

    private TheBaseShutdown() {}

    public static void stopQueuedTasks() {
        try {
            Field field = AsyncUtils.class.getDeclaredField("queuedTasks");
            field.setAccessible(true);
            Object tasks = field.get(null);
            if (! (tasks instanceof Collection)) return;

            for (Object task : new ArrayList<>((Collection<?>) tasks)) {
                if (task instanceof AsyncTask) ((AsyncTask) task).stop();
            }
            ((Collection<?>) tasks).clear();
        } catch (ReflectiveOperationException | RuntimeException e) {
            MessageUtils.logWarning("Could not stop TheBase's queued tasks; the server may not exit on its own: " + e);
        }
    }
}
