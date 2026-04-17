package singularity.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleUtils;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Abstract base for holders that depend on an external server plugin (not a Streamline module).
 *
 * <p>Checks whether one or more candidate plugin names are present on the server, then
 * exposes the resolved API instance of type {@code T}.  If an initial load attempt fails,
 * a {@link LoaderRunnable} retries the operation up to {@link LoaderRunnable#retryMax} times.</p>
 *
 * @param <T> the type of the external plugin API object held by this dependency
 */
@Setter
@Getter
public abstract class CosmicDependencyHolder<T> implements CosmicHolder {

    /**
     * Ordered map of candidate plugin-name keys to try when detecting presence,
     * keyed by insertion order index.
     */
    private ConcurrentSkipListMap<Integer, String> keysToTry;

    /**
     * The resolved API instance obtained from the external plugin, or {@code null}
     * if the plugin has not been successfully loaded yet.
     */
    private T api;

    /**
     * A human-readable identifier used in log messages and error reporting.
     */
    private String identifier;

    /**
     * Constructs a new {@code CosmicDependencyHolder} with the given identifier and
     * one or more plugin-name keys to probe when checking for presence.
     *
     * @param identifier  a human-readable name for this dependency, used in log messages
     * @param keysToTry   one or more plugin names to check via {@link ModuleUtils#serverHasPlugin}
     */
    public CosmicDependencyHolder(String identifier, String... keysToTry) {
        this.identifier = identifier;
        this.keysToTry = new ConcurrentSkipListMap<>();
        for (String key : keysToTry) {
            this.keysToTry.put(this.keysToTry.size(), key);
        }
    }

    /**
     * Returns {@code true} if at least one of the candidate plugin names is loaded on the server.
     *
     * @return {@code true} when the dependency plugin is detected, {@code false} otherwise
     */
    public boolean isPresent() {
        for (String key : keysToTry.values()) {
            if (isPresentCertain(key)) return true;
        }

        return false;
    }

    /**
     * Checks whether the server currently has a plugin loaded with the exact name {@code toTry}.
     *
     * @param toTry the plugin name to probe
     * @return {@code true} if the server has a plugin with that name, {@code false} otherwise
     */
    public static boolean isPresentCertain(String toTry) {
        return ModuleUtils.serverHasPlugin(toTry);
    }

    /**
     * Attempts to execute {@code callable} immediately.  If an exception is thrown, schedules
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
     * A repeating task that retries a failed dependency-load callable up to
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
