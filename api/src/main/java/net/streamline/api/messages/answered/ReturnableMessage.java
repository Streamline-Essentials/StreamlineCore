package net.streamline.api.messages.answered;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.messages.events.AnsweredMessageEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.scheduler.BaseRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReturnableMessage implements Comparable<ReturnableMessage> {
    @Getter
    private static final String key = "{{key}}";
    @Getter
    private static final int timeoutTicks = 200;
    @Getter
    private static final int testingTicks = 40;

    @Getter @Setter
    private String answerKey;
    @Getter @Setter
    private ProxiedMessage payload;
    @Getter @Setter
    private boolean answered = false;
    @Getter @Setter
    private boolean fired = false;
    @Getter @Setter
    private boolean called = false;
    @Getter @Setter
    private TimeoutTimer timeoutTimer;
    @Getter @Setter
    private ConcurrentSkipListMap<Integer, Consumer<ProxiedMessage>> registeredEvents = new ConcurrentSkipListMap<>();

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

    public void acceptAnswer(ProxiedMessage message) {
        setAnswered(true);

        AnsweredMessageEvent event = new AnsweredMessageEvent(this, message);
        ModuleUtils.fireEvent(event);
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

    public static class TimeoutTimer extends BaseRunnable {
        @Getter @Setter
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
