package singularity.messages.proxied;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gg.drak.thebase.utils.MathUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.command.CommandMessageBuilder;
import singularity.messages.builders.*;
import singularity.messages.answered.ReturnableMessage;
import singularity.objects.SingleSet;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.BaseRunnable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;

public class ProxiedMessageManager {
    @Getter @Setter
    private static PendingTicker pendingTicker;

    public static void init() {
        setPendingTicker(new PendingTicker());
    }

    @Getter @Setter
    private static Cache<Date, ReturnableMessage> loadedReturnableMessaged = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(2))
            .build();

    public static void loadReturnableMessage(ReturnableMessage returnableMessage) {
        getLoadedReturnableMessaged().put(returnableMessage.getPayload().getGottenAt(), returnableMessage);
    }

    public static void unloadReturnableMessage(ReturnableMessage returnableMessage) {
        if (returnableMessage.getTimeoutTimer() != null) {
            returnableMessage.getTimeoutTimer().cancel();
        }
        getLoadedReturnableMessaged().invalidate(returnableMessage.getPayload().getGottenAt());
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
        if (proxiedMessage.isReturnableLike()) {
            CompletableFuture.runAsync(() -> {
                getLoadedReturnableMessaged().asMap().forEach((date, returnableMessage) -> {
                    returnableMessage.tryAnswer(proxiedMessage);
                });
            });
        }

        CompletableFuture.runAsync(() -> {
            handle(proxiedMessage);
        });
    }

    public static void handle(ProxiedMessage proxiedMessage) {
        if (proxiedMessage.getMainChannel().equals(Singularity.getApiChannel())) {
            if (proxiedMessage.getSubChannel().equals(ResourcePackMessageBuilder.getSubChannel())) {
                SingleSet<String, CosmicResourcePack> set = ResourcePackMessageBuilder.unbuild(proxiedMessage);
                CosmicResourcePack resourcePack = set.getValue();

                Singularity.getInstance().getPlatform().sendResourcePack(resourcePack, set.getKey());
                return;
            }
            if (proxiedMessage.getSubChannel().equals(CommandMessageBuilder.getSubChannel())) {
                CommandMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(ProxyParseMessageBuilder.getSubChannel())) {
                ProxyParseMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(PlayerLocationMessageBuilder.getSubChannel())) {
                PlayerLocationMessageBuilder.handle(proxiedMessage);
                return;
            }
            if (proxiedMessage.getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
                ServerConnectMessageBuilder.handle(proxiedMessage);
                return;
            }
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
            Singularity.getInstance().getProxyMessenger().sendMessage(proxiedMessage);
            if (MathUtils.isDateOlderThan(date, 10, ChronoUnit.MINUTES)) unpendMessage(proxiedMessage);
        });
    }
}
