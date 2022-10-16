package net.streamline.api.scheduler;


import net.streamline.api.modules.StreamlineModule;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleTaskManager {
    public ConcurrentHashMap<StreamlineModule, TreeMap<Integer, ModuleRunnable>> currentRunnables = new ConcurrentHashMap<>();

    public void start(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.module);
        if (map == null) map = new TreeMap<>();

        map.put(moduleRunnable.getIndex(), moduleRunnable);

        currentRunnables.put(moduleRunnable.module, map);
    }

    public void cancel(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.module);
        if (map == null) map = new TreeMap<>();

        map.remove(moduleRunnable.getIndex());

        currentRunnables.put(moduleRunnable.module, map);
    }

    public void cancelAll(Module module) {
        for (ModuleRunnable runnable : currentRunnables.get(module).values()) {
            runnable.cancel();
        }
    }

    public void tick() {
        for (StreamlineModule streamlineModule : currentRunnables.keySet()) {
            for (ModuleRunnable runnable : currentRunnables.get(streamlineModule).values()) {
                runnable.tick();
            }
        }
    }
}
