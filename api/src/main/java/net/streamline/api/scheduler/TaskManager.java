package net.streamline.api.scheduler;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.utils.MessageUtils;

import javax.swing.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class TaskManager {
    @Getter @Setter
    private static ConcurrentSkipListMap<Integer, BaseRunnable> currentRunnables = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static Timer timer;

    public static void start(BaseRunnable runnable) {
        currentRunnables.put(runnable.getIndex(), runnable);
    }

    public static void cancel(BaseRunnable runnable) {
        cancel(runnable.getIndex());
    }

    public static int getNextIndex() {
        return currentRunnables.size();
    }

    public static void tick() {
        for (BaseRunnable runnable : currentRunnables.values()) {
            try {
                runnable.tick();
            } catch (Throwable e) {
                MessageUtils.logDebug("Error while ticking runnable: " + runnable, e);
            }
        }
    }

    public static void stop() {
        timer.stop();
        currentRunnables.clear();
    }

    public static boolean isCancelled(int index) {
        return ! currentRunnables.containsKey(index);
    }

    public static void cancel(int index) {
        currentRunnables.remove(index);
    }

    public static BaseRunnable getRunnable(int index) {
        return currentRunnables.get(index);
    }

    public static void init() {
        timer = new Timer(50, e -> tick());
        timer.start();

        MessageUtils.logInfo("&cTaskManager &fis now initialized!");
    }
}
