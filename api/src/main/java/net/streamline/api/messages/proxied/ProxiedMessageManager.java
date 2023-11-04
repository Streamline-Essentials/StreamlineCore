package net.streamline.api.messages.proxied;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.scheduler.BaseRunnable;
import tv.quaint.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListMap;

public class ProxiedMessageManager {
    @Getter @Setter
    private static PendingTicker pendingTicker;

    public static void init() {
        setPendingTicker(new PendingTicker());
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<Date, ReturnableMessage> loadedReturnableMessaged = new ConcurrentSkipListMap<>();

    public static void loadReturnableMessage(ReturnableMessage returnableMessage) {
        getLoadedReturnableMessaged().put(returnableMessage.getPayload().getGottenAt(), returnableMessage);
    }

    public static void unloadReturnableMessage(ReturnableMessage returnableMessage) {
        getLoadedReturnableMessaged().remove(returnableMessage.getPayload().getGottenAt());
    }

    public static void sendReturnable(ReturnableMessage returnableMessage) {
        loadReturnableMessage(returnableMessage);
        returnableMessage.getPayload().send();
    }

    public static boolean killReturnable(ReturnableMessage returnableMessage) {
        if (! returnableMessage.isAnswered()) {
            if (! MathUtils.isDateOlderThan(returnableMessage.getPayload().getGottenAt(), ReturnableMessage.getTimeoutTicks() / 20, ChronoUnit.SECONDS)) return false;
        }
        unloadReturnableMessage(returnableMessage);
        return true;
    }

    public static void onProxiedMessageReceived(ProxiedMessage proxiedMessage) {
        ProxyMessageInEvent event = new ProxyMessageInEvent(proxiedMessage);
        ModuleUtils.fireEvent(event);

        if (event.getMessage().isReturnableLike()) {
            getLoadedReturnableMessaged().forEach((date, returnableMessage) -> {
                returnableMessage.tryAnswer(event.getMessage());
            });
        }
    }

    public static class PendingTicker extends BaseRunnable {
        public PendingTicker() {
            super(40, 40);
        }

        @Override
        public void run() {
            tickPendingMessages();
        }
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<Date, ProxiedMessage> pendingMessages = new ConcurrentSkipListMap<>();

    public static void pendMessage(ProxiedMessage proxiedMessage) {
        getPendingMessages().put(proxiedMessage.getGottenAt(), proxiedMessage);
    }

    public static void unpendMessage(ProxiedMessage proxiedMessage) {
        getPendingMessages().remove(proxiedMessage.getGottenAt());
    }

    public static void tickPendingMessages() {
        getPendingMessages().forEach((date, proxiedMessage) -> {
            SLAPI.getInstance().getProxyMessenger().sendMessage(proxiedMessage);
            if (MathUtils.isDateOlderThan(date, 10, ChronoUnit.MINUTES)) unpendMessage(proxiedMessage);
        });
    }
}
