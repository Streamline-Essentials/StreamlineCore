package singularity.placeholders.callbacks;

import singularity.placeholders.handling.RATHandledString;

import java.util.List;

public class CallbackString {
    private final String string;
    private final RATHandledString handledString;

    public String string() {
        return string;
    }

    public RATHandledString handledString() {
        return handledString;
    }

    public CallbackString(String string, RATHandledString handledString) {
        this.string = string;
        this.handledString = handledString;
    }

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
