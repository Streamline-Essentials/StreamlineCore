package host.plas.redis;

import host.plas.data.WebhookType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter @Setter
public class ProxiedLoggedMessage implements Comparable<ProxiedLoggedMessage> {
    public static final String NONE_SENDER = "NONE";
    public static final String MESSAGE_SPLIT = "<,!,!,>";

    private Instant timestamp;
    private String server;
    private String message;
    private WebhookType webhookType;
    private String sender;

    public ProxiedLoggedMessage(String server, String message, WebhookType webhookType, String sender) {
        this.timestamp = Instant.now();
        this.server = server;
        this.message = message;
        this.webhookType = webhookType;
        this.sender = sender;
    }

    @Override
    public int compareTo(@NotNull ProxiedLoggedMessage o) {
        return this.getTimestamp().compareTo(o.getTimestamp());
    }

    public void queue() {
        LoggerRedisManager.queueMessage(this);
    }

    public void dequeue() {
        LoggerRedisManager.dequeueMessage(this);
    }

    public boolean isQueued() {
        return LoggerRedisManager.isQueued(this);
    }

    public boolean hasSender() {
        return ! sender.equals(NONE_SENDER);
    }

    public boolean hasSplit() {
        return getMessage().contains(MESSAGE_SPLIT);
    }

    public String[] getSplitMessage() {
        return getMessage().split(MESSAGE_SPLIT);
    }

    public static String withSplit(String... parts) {
        return String.join(MESSAGE_SPLIT, parts);
    }
}
