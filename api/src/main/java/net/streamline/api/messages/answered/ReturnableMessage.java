package net.streamline.api.messages.answered;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.messages.events.AnsweredMessageEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.scheduler.BaseRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

@Setter
public class ReturnableMessage implements Comparable<ReturnableMessage> {
    @Getter
    private static final String key = "{{key}}";
    @Getter
    private static final int timeoutTicks = 200;
    @Getter
    private static final int testingTicks = 40;

    @Getter
    private String answerKey;
    @Getter
    private ProxiedMessage payload;
    @Getter
    private boolean fired = false;
    @Getter
    private boolean called = false;
    @Getter
    private TimeoutTimer timeoutTimer;
    @Getter
    private ConcurrentSkipListMap<Integer, Consumer<ProxiedMessage>> registeredEvents = new ConcurrentSkipListMap<>();
    @Getter
    private Optional<ProxiedMessage> answer = Optional.empty();

    public ReturnableMessage(ProxiedMessage payload, boolean send) {
        this.payload = payload;
        this.answerKey = UUID.randomUUID().toString();
        getPayload().write(getKey(), getAnswerKey());
        setTimeoutTimer(new TimeoutTimer(this));
        if (send) send();
    }

    public ReturnableMessage(ProxiedMessage payload) {
        this(payload, true);
    }

    public void send() {
        ProxiedMessageManager.sendReturnable(this);
    }

    public boolean tryAnswer(ProxiedMessage answer) {
        if (answer.getString(getKey()).equals(getAnswerKey())) {
            acceptAnswer(answer);
            return true;
        }
        return false;
    }

    public boolean isAnswered() {
        return answer.isPresent();
    }

    public void acceptAnswer(ProxiedMessage message) {
        answer = Optional.of(message);

        AnsweredMessageEvent event = new AnsweredMessageEvent(this, message).fire();
        if (event.isCancelled()) {
            return;
        }

        fire(message);
        setFired(true);

        callEvents(message);
        setCalled(true);
    }

    public void fire(ProxiedMessage message) {
        // do nothing.
    }

    public boolean kill() {
        return ProxiedMessageManager.killReturnable(this);
    }

    public void killNow() {
        getTimeoutTimer().cancel();
        ProxiedMessageManager.unloadReturnableMessage(this);
    }

    public void registerEventCall(Consumer<ProxiedMessage> consumer) {
        getRegisteredEvents().put(getRegisteredEvents().size() + 1, consumer);
    }

    public void callEvents(ProxiedMessage accepted) {
        getRegisteredEvents().forEach((integer, consumer) -> {
            try {
                consumer.accept(accepted);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int compareTo(@NotNull ReturnableMessage o) {
        return Long.compare(getPayload().getGottenAt().getTime(), o.getPayload().getGottenAt().getTime());
    }

    @Setter
    @Getter
    public static class TimeoutTimer extends BaseRunnable {
        private ReturnableMessage parent;

        public TimeoutTimer(ReturnableMessage returnableMessage) {
            super(getTestingTicks(), getTestingTicks());
            setParent(returnableMessage);
        }

        @Override
        public void run() {
            if (getParent().kill()) cancel();
        }
    }
}
