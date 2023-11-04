package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MessageUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;

@Getter
public abstract class ModuleDependencyHolder<T> {
    @Setter
    private ConcurrentSkipListMap<Integer, String> keysToTry;
    @Setter
    private T api;
    @Setter
    private String identifier;

    public ModuleDependencyHolder(String identifier, String... keysToTry) {
        this.identifier = identifier;
        this.keysToTry = new ConcurrentSkipListMap<>();
        for (String key : keysToTry) {
            this.keysToTry.put(this.keysToTry.size(), key);
        }
    }

    public boolean isPresent() {
        for (String key : keysToTry.values()) {
            if (isPresentCertain(key)) return true;
        }

        return false;
    }

    public static boolean isPresentCertain(String toTry) {
        return ModuleManager.hasModule(toTry);
    }

    public CompletableFuture<Void> nativeComplete() {
        return CompletableFuture.runAsync(() -> {
            setApi((T) ModuleManager.getModule(getIdentifier()));
        });
    }

    public void nativeLoad() {
        new NativeLoader();
    }

    public class NativeLoader extends BaseRunnable {
        public NativeLoader() {
            super(40, 1);
        }

        @Override
        public void run() {
            nativeComplete().join();
            cancel();
        }
    }

    public void tryLoad(Callable<Void> callable) {
        try {
            callable.call();
        } catch (Exception e) {
            MessageUtils.logWarning("Could not load '" + getIdentifier() + "'... Retrying in " + LoaderRunnable.getRetryDelay() + " ticks...");
            new LoaderRunnable(0, callable);
        }
    }

    public static class LoaderRunnable extends BaseRunnable {
        @Getter
        private static final long retryDelay = 60L;
        @Getter
        private static final long retryPeriod = 1L;
        @Getter
        private static final int retryMax = 3;

        @Getter
        private final int timesTried;
        @Getter
        private final Callable<Void> toRun;

        public LoaderRunnable(int timesTried, Callable<Void> toRun) {
            super(getRetryDelay(), getRetryPeriod());
            this.timesTried = timesTried;
            this.toRun = toRun;
        }

        @Override
        public void run() {
            try {
                getToRun().call();
            } catch (Exception e) {
                if (getTimesTried() < getRetryMax()) new LoaderRunnable(getTimesTried() + 1, getToRun());
            }
            this.cancel();
        }
    }
}
