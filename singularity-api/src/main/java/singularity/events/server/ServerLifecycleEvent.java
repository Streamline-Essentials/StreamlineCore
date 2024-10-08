package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;

@Setter
@Getter
public class ServerLifecycleEvent extends CosmicEvent {
    private String message;

    public ServerLifecycleEvent(String message) {
        setMessage(message);
    }

    public ServerLifecycleEvent() {
        this(null);
    }

    public boolean isSendable() {
        return getMessage() != null;
    }

    public void appendLine(String line) {
        setMessage(getMessage() + "%newline%" + line);
    }

    public void append(String toAppend) {
        setMessage(getMessage() + toAppend);
    }

    public StringBuilder asStringBuilder() {
        return new StringBuilder(getMessage());
    }

    public String fromStringBuilder(StringBuilder builder) {
        setMessage(builder.toString());
        return getMessage();
    }
}
