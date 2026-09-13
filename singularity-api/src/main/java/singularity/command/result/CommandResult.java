package singularity.command.result;

import lombok.Getter;
import lombok.Setter;

/**
 * Carries the outcome of a command execution as a key-value pair. The key is a
 * human-readable category string and the value is the typed result variable.
 *
 * <p>Four predefined singleton sub-types cover the most common outcomes:</p>
 * <ul>
 *   <li>{@link Success} — the command completed successfully (value {@code 1}).</li>
 *   <li>{@link Failure} — the command was rejected (e.g. bad usage) (value {@code -2}).</li>
 *   <li>{@link Error} — an unexpected error occurred during execution (value {@code -1}).</li>
 *   <li>{@link NotSet} — no result has been assigned yet (value {@code 0}).</li>
 * </ul>
 *
 * @param <T> the type of the result variable
 */
@Getter @Setter
public class CommandResult<T> {

    /** A string categorising the result, e.g. {@code "success"} or {@code "error"}. */
    private String resultKey;

    /** The typed result value associated with this outcome. */
    private T resultVar;

    /**
     * Creates a new {@code CommandResult} with the given key and value.
     *
     * @param resultKey the result category key
     * @param resultVar the result value
     */
    public CommandResult(String resultKey, T resultVar) {
        this.resultKey = resultKey;
        this.resultVar = resultVar;
    }

    /**
     * Represents a successful command execution. Uses the key {@code "success"} and
     * the integer value {@code 1}. Singleton — obtain via {@link #get()}.
     */
    public static class Success extends CommandResult<Integer> {
        /** The singleton instance of {@code Success}. */
        protected static Success instance;

        /**
         * Returns the singleton {@code Success} instance, creating it on first call.
         *
         * @return the {@code Success} singleton
         */
        public static Success get() {
            if (instance == null) instance = new Success();
            return instance;
        }

        /**
         * Creates a {@code Success} result with key {@code "success"} and value {@code 1}.
         */
        public Success() {
            super("success", 1);
        }
    }

    /**
     * Represents a command rejection (e.g. invalid usage or missing arguments).
     * Uses the key {@code "failure"} and the integer value {@code -2}.
     * Singleton — obtain via {@link #get()}.
     */
    public static class Failure extends CommandResult<Integer> {
        /** The singleton instance of {@code Failure}. */
        protected static Failure instance;

        /**
         * Returns the singleton {@code Failure} instance, creating it on first call.
         *
         * @return the {@code Failure} singleton
         */
        public static Failure get() {
            if (instance == null) instance = new Failure();
            return instance;
        }

        /**
         * Creates a {@code Failure} result with key {@code "failure"} and value {@code -2}.
         */
        public Failure() {
            super("failure", -2);
        }
    }

    /**
     * Represents an unexpected error that occurred while executing the command.
     * Uses the key {@code "error"} and the integer value {@code -1}.
     * Singleton — obtain via {@link #get()}.
     */
    public static class Error extends CommandResult<Integer> {
        /** The singleton instance of {@code Error}. */
        protected static Error instance;

        /**
         * Returns the singleton {@code Error} instance, creating it on first call.
         *
         * @return the {@code Error} singleton
         */
        public static Error get() {
            if (instance == null) instance = new Error();
            return instance;
        }

        /**
         * Creates an {@code Error} result with key {@code "error"} and value {@code -1}.
         */
        public Error() {
            super("error", -1);
        }
    }

    /**
     * Represents the state where no result has been assigned yet (the initial state).
     * Uses the key {@code "not-set"} and the integer value {@code 0}.
     * Singleton — obtain via {@link #get()}.
     */
    public static class NotSet extends CommandResult<Integer> {
        /** The singleton instance of {@code NotSet}. */
        protected static NotSet instance;

        /**
         * Returns the singleton {@code NotSet} instance, creating it on first call.
         *
         * @return the {@code NotSet} singleton
         */
        public static NotSet get() {
            if (instance == null) instance = new NotSet();
            return instance;
        }

        /**
         * Creates a {@code NotSet} result with key {@code "not-set"} and value {@code 0}.
         */
        public NotSet() {
            super("not-set", 0);
        }
    }
}