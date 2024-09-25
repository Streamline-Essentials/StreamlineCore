package singularity.placeholders.callbacks;

import singularity.data.console.CosmicSender;

import java.util.function.BiFunction;

@FunctionalInterface
public interface UserPlaceholderCallback extends BiFunction<CallbackString, CosmicSender, String>, RATCallback {
}
