package net.streamline.api.scheduler;


import net.streamline.api.modules.BundledModule;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleTaskManager {
    public ConcurrentHashMap<BundledModule, TreeMap<Integer, ModuleRunnable>> currentRunnables = new ConcurrentHashMap<>();

    public void start(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.module);
        if (map == null) map = new TreeMap<>();

        map.put(moduleRunnable.index, moduleRunnable);

        currentRunnables.put(moduleRunnable.module, map);
    }

    public void cancel(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.module);
        if (map == null) map = new TreeMap<>();

        map.remove(moduleRunnable.index);

        currentRunnables.put(moduleRunnable.module, map);
    }

    public void cancelAll(Module module) {
        for (ModuleRunnable runnable : currentRunnables.get(module).values()) {
            runnable.cancel();
        }
    }

    public void tick() {
        for (BundledModule bundledModule : currentRunnables.keySet()) {
            for (ModuleRunnable runnable : currentRunnables.get(bundledModule).values()) {
                runnable.tick();
            }
        }
    }
}
