package net.streamline.api.placeholders.callbacks;

import net.streamline.api.placeholders.handling.RATHandledString;

import java.util.List;

public record CallbackString(String string, RATHandledString handledString) {
    public List<String> simplyGet() {
        return handledString.isolateIn(string);
    }

    public String get(int index) {
        return simplyGet().get(index);
    }

    public String get() {
        return get(0);
    }
}
