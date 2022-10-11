package net.streamline.api.messages.proxied;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListMap;

public class ProxiedMessageManager {
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
        SLAPI.getInstance().getProxyMessenger().sendMessage(returnableMessage.getPayload());
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
        proxiedMessage = event.getMessage();

        if (proxiedMessage.isReturnableLike()) {
            ProxiedMessage finalProxiedMessage = proxiedMessage;
            getLoadedReturnableMessaged().forEach((date, returnableMessage) -> {
                returnableMessage.tryAnswer(finalProxiedMessage);
            });
        }
    }
}
