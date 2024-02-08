package net.streamline.api.placeholders.callbacks;

import java.util.function.BiFunction;

@FunctionalInterface
public interface UserPlaceholderCallback extends BiFunction<CallbackString, StreamlineUser, String>, RATCallback {
}
