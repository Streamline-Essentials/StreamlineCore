package host.plas;

import host.plas.config.RedisInfoConfig;
import host.plas.config.WebhookConfig;
import host.plas.events.MainListener;
import host.plas.external.MessagingAdapter;
import host.plas.managers.WebhookManager;
import host.plas.ratapi.LoggerExpansion;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.modules.SimpleModule;

import java.util.ArrayList;

public class SimpleLogger extends SimpleModule {
    @Getter @Setter
    private static SimpleLogger instance;

    @Getter @Setter
    private static LoggerExpansion loggerExpansion;

    @Getter @Setter
    private static WebhookConfig webhookConfig;
    @Getter @Setter
    private static RedisInfoConfig redisInfoConfig;

    @Getter @Setter
    private static MainListener mainListener;

    @Getter @Setter
    private static MessagingAdapter messagingAdapter;

    public SimpleLogger(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(new ArrayList<>());
    }

    @Override
    public void onEnable() {
        instance = this;

        webhookConfig = new WebhookConfig();
        redisInfoConfig = new RedisInfoConfig();

        loggerExpansion = new LoggerExpansion();

        mainListener = new MainListener();

        try {
            messagingAdapter = new MessagingAdapter();
        } catch (Throwable e) {
            // Do nothing. This is for hooking. This will throw a ClassNotFoundException if StreamlineMessaging is not present.
        }
    }

    @Override
    public void onDisable() {
        WebhookManager.getRawClients().clear();
    }
}
