package singularity.placeholders.callbacks;

import java.util.function.Function;

@FunctionalInterface
public interface PlaceholderCallback extends Function<CallbackString, String>, RATCallback {
}
