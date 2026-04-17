package singularity.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleManager;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Abstract base for holders that depend on a Streamline PF4J module (as opposed to a
 * server plugin).
 *
 * <p>Checks whether one or more candidate module identifiers are loaded via
 * {@link ModuleManager}, resolves the API instance of type {@code T}, and provides
 * retry logic through {@link LoaderRunnable} when an initial load attempt fails.</p>
 *
 * @param <T> the type of the module API object held by this dependency
 */
@Setter
@Getter
public abstract class ModuleDependencyHolder<T> implements CosmicHolder {

    /**
     * Ordered map of candidate module-identifier keys to try when detecting presence,
     * keyed by insertion order index.
     */
    private ConcurrentSkipListMap<Integer, String> keysToTry;

    /**
     * The resolved API instance obtained from the module, or {@code null} if the module
     * has not been successfully loaded yet.
     */
    private T api;

    /**
     * A human-readable identifier used in log messages and error reporting.
     */
    private String identifier;

    /**
     * Constructs a new {@code ModuleDependencyHolder} with the given identifier and one or
     * more module-identifier keys to probe when checking for presence.
     *
     * @param identifier  a human-readable name for this dependency, used in log messages
     * @param keysToTry   one or more module identifiers to check via {@link ModuleManager#hasModule}
     */
    public ModuleDependencyHolder(String identifier, String... keysToTry) {
        this.identifier = identifier;
        this.keysToTry = new ConcurrentSkipListMap<>();
        for (String key : keysToTry) {
            this.keysToTry.put(this.keysToTry.size(), key);
        }
    }

    /**
     * Returns {@code true} if at least one of the candidate module identifiers is currently
     * loaded by {@link ModuleManager}.
     *
     * @return {@code true} when the dependency module is detected, {@code false} otherwise
     */
    public boolean isPresent() {
        for (String key : keysToTry.values()) {
            if (isPresentCertain(key)) return true;
        }

        return false;
    }

    /**
     * Checks whether {@link ModuleManager} has a module loaded with the exact identifier
     * {@code toTry}.
     *
     * @param toTry the module identifier to probe
     * @return {@code true} if the module manager knows that identifier, {@code false} otherwise
     */
    public static boolean isPresentCertain(String toTry) {
        return ModuleManager.hasModule(toTry);
    }

    /**
     * Asynchronously resolves the module API object from {@link ModuleManager} and stores it
     * in {@link #api}.
     *
     * @return a {@link CompletableFuture} that completes when the API has been assigned
     */
    public CompletableFuture<Void> nativeComplete() {
        return CompletableFuture.runAsync(() -> {
            setApi((T) ModuleManager.getModule(getIdentifier()));
        });
    }

    /**
     * Schedules a {@link NativeLoader} task that will asynchronously resolve the module API
     * after a short server-tick delay.
     */
    public void nativeLoad() {
        new NativeLoader();
    }

    /**
     * A one-shot repeating task that waits a brief period before resolving the module API
     * via {@link #nativeComplete()} and then cancels itself.
     */
    public class NativeLoader extends BaseRunnable {

        /**
         * Creates a new {@code NativeLoader} that fires after 40 ticks with a period of 1 tick.
         */
        public NativeLoader() {
            super(40, 1);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Blocks until {@link #nativeComplete()} finishes, then cancels this task.</p>
         */
        @Override
        public void run() {
            nativeComplete().join();
            cancel();
        }
    }

    /**
     * Attempts to execute {@code callable} immediately. If an exception is thrown, schedules
     * a {@link LoaderRunnable} to retry the call after a short delay.
     *
     * @param callable the initialisation logic to run; must return {@code Void}
     */
    public void tryLoad(Callable<Void> callable) {
        try {
            callable.call();
        } catch (Exception e) {
            MessageUtils.logWarning("Could not load '" + getIdentifier() + "'... Retrying in " + LoaderRunnable.getRetryDelay() + " ticks...");
            new LoaderRunnable(0, callable);
        }
    }

    /**
     * A repeating task that retries a failed module-load callable up to
     * {@link #retryMax} times before giving up.
     */
    public static class LoaderRunnable extends BaseRunnable {

        /**
         * Number of ticks to wait before the first (and each subsequent) retry attempt.
         */
        @Getter
        private static final long retryDelay = 60L;

        /**
         * Period (in ticks) between successive retry checks.
         */
        @Getter
        private static final long retryPeriod = 1L;

        /**
         * Maximum number of retry attempts before the runnable stops scheduling further retries.
         */
        @Getter
        private static final int retryMax = 3;

        /**
         * The number of retry attempts that have already been made for this runnable.
         */
        @Getter
        private final int timesTried;

        /**
         * The initialisation callable to retry on each tick.
         */
        @Getter
        private final Callable<Void> toRun;

        /**
         * Creates a new {@code LoaderRunnable} that will attempt to execute {@code toRun}
         * after {@link #retryDelay} ticks, using a period of {@link #retryPeriod}.
         *
         * @param timesTried the number of previous attempts already made
         * @param toRun      the callable to retry
         */
        public LoaderRunnable(int timesTried, Callable<Void> toRun) {
            super(getRetryDelay(), getRetryPeriod());
            this.timesTried = timesTried;
            this.toRun = toRun;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Calls the wrapped callable. If it fails and the attempt count is below
         * {@link #retryMax}, schedules a new {@code LoaderRunnable} with an incremented
         * counter. Cancels this task regardless of outcome.</p>
         */
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
