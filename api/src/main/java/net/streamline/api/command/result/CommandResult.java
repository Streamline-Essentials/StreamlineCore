package net.streamline.api.command.result;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CommandResult<T> {
    private String resultKey;
    private T resultVar;

    public CommandResult(String resultKey, T resultVar) {
        this.resultKey = resultKey;
        this.resultVar = resultVar;
    }

    public static class Success extends CommandResult<Integer> {
        protected static Success instance;

        public static Success get() {
            if (instance == null) instance = new Success();
            return instance;
        }

        public Success() {
            super("success", 1);
        }
    }

    public static class Failure extends CommandResult<Integer> {
        protected static Failure instance;

        public static Failure get() {
            if (instance == null) instance = new Failure();
            return instance;
        }

        public Failure() {
            super("failure", -2);
        }
    }

    public static class Error extends CommandResult<Integer> {
        protected static Error instance;

        public static Error get() {
            if (instance == null) instance = new Error();
            return instance;
        }

        public Error() {
            super("error", -1);
        }
    }

    public static class NotSet extends CommandResult<Integer> {
        protected static NotSet instance;

        public static NotSet get() {
            if (instance == null) instance = new NotSet();
            return instance;
        }

        public NotSet() {
            super("not-set", 0);
        }
    }
}