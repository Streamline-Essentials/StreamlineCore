package net.streamline.api.placeholders.callbacks;

import net.streamline.api.data.console.StreamSender;

import java.util.function.BiFunction;

@FunctionalInterface
public interface UserPlaceholderCallback extends BiFunction<CallbackString, StreamSender, String>, RATCallback {
}
