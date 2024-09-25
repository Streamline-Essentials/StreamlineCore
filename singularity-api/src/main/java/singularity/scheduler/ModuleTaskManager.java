package singularity.scheduler;


import singularity.modules.ModuleLike;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleTaskManager {
    public ConcurrentHashMap<ModuleLike, TreeMap<Integer, ModuleRunnable>> currentRunnables = new ConcurrentHashMap<>();

    public void start(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.getModule());
        if (map == null) map = new TreeMap<>();

        map.put(moduleRunnable.getIndex(), moduleRunnable);

        currentRunnables.put(moduleRunnable.getModule(), map);
    }

    public void cancel(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.getModule());
        if (map == null) map = new TreeMap<>();

        map.remove(moduleRunnable.getIndex());

        currentRunnables.put(moduleRunnable.getModule(), map);
    }

    public void cancelAll(ModuleLike module) {
        for (ModuleRunnable runnable : currentRunnables.get(module).values()) {
            runnable.cancel();
        }
    }

    public void tick() {
        for (ModuleLike module : currentRunnables.keySet()) {
            for (ModuleRunnable runnable : currentRunnables.get(module).values()) {
                runnable.tick();
            }
        }
    }
}
