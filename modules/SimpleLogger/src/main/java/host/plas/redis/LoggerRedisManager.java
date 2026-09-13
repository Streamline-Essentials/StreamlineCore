package host.plas.redis;

import host.plas.SimpleLogger;
import host.plas.config.WebhookSetup;
import host.plas.data.WebhookType;
import host.plas.managers.WebhookManager;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.redis.RedisClient;
import singularity.redis.RedisMessage;
import singularity.utils.UserUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class LoggerRedisManager {
    public static final String CHANNEL = "sl-simple-logger";

    @Getter @Setter
    private static LoggerRedisListener listener;
    @Getter @Setter
    private static LoggerRedisTicker ticker;

    public static void init() {
        if (! isUseRedis()) return;

        listener = new LoggerRedisListener();
        ticker = new LoggerRedisTicker();
    }

    public static boolean isUseRedis() {
        return RedisClient.isConnected() && SimpleLogger.getRedisInfoConfig().isUseRedis();
    }

    public static String getServer() {
        return GivenConfigs.getServerName();
    }

    public static void sendMessage(String server, String message, WebhookType webhookType, String sender) {
        RedisMessage redisMessage = new RedisMessage(CHANNEL, server + ":::" + message + ":::" + webhookType.name() + ":::" + sender);
        redisMessage.send();
    }

    public static void sendMessage(String server, String message, WebhookType webhookType) {
        sendMessage(server, message, webhookType, ProxiedLoggedMessage.NONE_SENDER);
    }

    public static void sendMessage(String message, WebhookType webhookType, String sender) {
        sendMessage(getServer(), message, webhookType, sender);
    }

    public static void sendMessage(String message, WebhookType webhookType) {
        sendMessage(getServer(), message, webhookType);
    }

    public static boolean isProxy() {
        return SLAPI.isProxy();
    }

    public static boolean isOfChannel(RedisMessage redisMessage) {
        if (! isUseRedis()) return false;
        if (! isProxy()) return false;

        return redisMessage.getChannel().equals(CHANNEL);
    }

    public static ProxiedLoggedMessage readMessage(RedisMessage redisMessage) {
        if (! isUseRedis()) return null;
        if (! isProxy()) return null;
        if (! isOfChannel(redisMessage)) return null;

        String[] parts = redisMessage.getMessage().split(":::");
        if (parts.length < 4) return null;
        String server = parts[0];
        String message = parts[1];
        String webhookTypeStr = parts[2];
        String sender = parts[3];

        WebhookType webhookType;
        try {
            webhookType = WebhookType.valueOf(webhookTypeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new ProxiedLoggedMessage(server, message, webhookType, sender);
    }

    public static void readAndQueueMessage(RedisMessage redisMessage) {
        ProxiedLoggedMessage loggedMessage = readMessage(redisMessage);
        if (loggedMessage == null) return;

        queueMessage(loggedMessage);
    }

    @Getter @Setter
    private static ConcurrentSkipListSet<ProxiedLoggedMessage> messageQueue = new ConcurrentSkipListSet<>();

    public static void queueMessage(ProxiedLoggedMessage message) {
        getMessageQueue().add(message);
    }

    public static void dequeueMessage(ProxiedLoggedMessage message) {
        getMessageQueue().removeIf(m -> m.getTimestamp().equals(message.getTimestamp()));
    }

    public static void clearQueue() {
        getMessageQueue().clear();
    }

    public static Optional<ProxiedLoggedMessage> getMessage(Instant instant) {
        return getMessageQueue().stream()
                .filter(m -> m.getTimestamp().equals(instant))
                .findFirst();
    }

    public static boolean isQueued(ProxiedLoggedMessage message) {
        return getMessage(message.getTimestamp()).isPresent();
    }

    public static void tickQueue() {
        if (getMessageQueue().isEmpty()) return;

        getMessageQueue().forEach(message -> {
            String server = message.getServer();
            String msg = message.getMessage();
            WebhookType webhookType = message.getWebhookType();

            WebhookSetup setup = null;
            if (webhookType == WebhookType.LOGS) {
                setup = SimpleLogger.getWebhookConfig().getLogFor(server);
                if (setup == null) {
                    setup = SimpleLogger.getWebhookConfig().getLogsSetup();
                }
                if (setup == null) return;

                WebhookManager.sendWebhookLogText(setup, msg);
            } else {
                if (! message.hasSender()) return;
                String uuid = message.getSender();
                CosmicSender sender = UserUtils.getOrGetSender(uuid).orElse(null);
                if (sender == null) return;

                setup = SimpleLogger.getWebhookConfig().getServerSetup(server);
                if (setup == null) {
//                    setup = SimpleLogger.getWebhookConfig().getGlobalSetup();
                }
                if (setup == null) return;

                if (msg.startsWith("/")) {
                    WebhookManager.sendWebhook(setup.getUrl(), setup.getCommands(), sender, msg);
                } else {
                    WebhookManager.sendWebhook(setup.getUrl(), setup.getChat(), sender, msg);
                }
            }

            message.dequeue();
        });
    }
}
