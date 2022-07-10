package net.streamline.api.scheduler;

import net.streamline.api.modules.BundledModule;

import java.util.Date;
import java.util.TreeMap;

public class TaskManager {
    public TreeMap<Integer, BaseRunnable> currentRunnables = new TreeMap<>();

    public void start(BaseRunnable runnable) {
        currentRunnables.put(runnable.index, runnable);
    }

    public void cancel(BaseRunnable runnable) {
        currentRunnables.remove(runnable.index);
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
