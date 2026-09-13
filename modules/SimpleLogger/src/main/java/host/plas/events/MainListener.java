package host.plas.events;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.SimpleLogger;
import host.plas.data.WebhookType;
import host.plas.managers.WebhookManager;
import host.plas.redis.LoggerRedisManager;
import host.plas.redis.ProxiedLoggedMessage;
import singularity.data.console.CosmicSender;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.CosmicLogPopEvent;
import singularity.events.server.ServerLogTextEvent;
import singularity.logging.LogIntent;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class MainListener implements BaseEventListener {
    public MainListener() {
        BaseEventHandler.bake(this, SimpleLogger.getInstance());
    }

    @BaseProcessor(priority = BaseEventPriority.HIGHEST)
    public void onMessage(CosmicChatEvent event) {
        CosmicSender sender = event.getSender();
        if (sender == null) return;
        String message = event.getMessage();

        if (LoggerRedisManager.isUseRedis() && ! LoggerRedisManager.isProxy()) {
            LoggerRedisManager.sendMessage(message, WebhookType.PROXIED, sender.getUuid());
        } else {
            WebhookManager.sendWebhookText(sender, message);
        }
    }

    @BaseProcessor
    public void onLogMessage(CosmicLogPopEvent event) {
        if (LoggerRedisManager.isUseRedis() && ! LoggerRedisManager.isProxy()) {
            ConcurrentSkipListSet<String> s = event.getPoppedLogs().values().stream()
                    .filter(e ->
                            e.getIntent() == LogIntent.INFO ||
                                    e.getIntent() == LogIntent.WARNING ||
                                    e.getIntent() == LogIntent.SEVERE ||
                                    e.getIntent() == LogIntent.DEBUG
                    )
                    .map(ServerLogTextEvent::getMessage).collect(Collectors.toCollection(ConcurrentSkipListSet::new));
            String finalMessage = ProxiedLoggedMessage.withSplit(s.toArray(String[]::new));
            if (finalMessage.isEmpty()) return;

            LoggerRedisManager.sendMessage(finalMessage, WebhookType.LOGS);
        } else {
            WebhookManager.sendWebhookLogText(event);
        }
    }
}
