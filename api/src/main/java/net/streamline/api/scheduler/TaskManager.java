package net.streamline.api.scheduler;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class TaskManager {
    public ConcurrentSkipListMap<Integer, BaseRunnable> currentRunnables = new ConcurrentSkipListMap<>();

    public void start(BaseRunnable runnable) {
        currentRunnables.put(runnable.getIndex(), runnable);
    }

    public void cancel(BaseRunnable runnable) {
        currentRunnables.remove(runnable.getIndex());
    }

    public int getNextIndex() {
        return currentRunnables.size();
    }

    public void tick() {
        for (BaseRunnable runnable : currentRunnables.values()) {
            runnable.tick();
        }
    }
}
