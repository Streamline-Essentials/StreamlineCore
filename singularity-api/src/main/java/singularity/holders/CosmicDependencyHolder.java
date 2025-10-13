package singularity.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleUtils;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;

@Setter
@Getter
public abstract class CosmicDependencyHolder<T> implements CosmicHolder {
    private ConcurrentSkipListMap<Integer, String> keysToTry;
    private T api;
    private String identifier;

    public CosmicDependencyHolder(String identifier, String... keysToTry) {
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
        return ModuleUtils.serverHasPlugin(toTry);
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
