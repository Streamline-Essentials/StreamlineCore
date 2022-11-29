package net.streamline.api.placeholders.callbacks;

import net.streamline.api.placeholders.handling.RATHandledString;

import java.util.List;

public record CallbackString(String string, RATHandledString handledString) {
    public List<String> simplyGet(int group) {
        return handledString.getRegexMatchesForGroup(string, group);
    }

    public String get(int index, int group) {
        return simplyGet(group).get(index);
    }

    public String get(int index) {
        return get(index, 1);
    }

    public String get() {
        return get(0);
    }

    public String getSimpleGroup(int group) {
        return get(0, group);
    }
}
