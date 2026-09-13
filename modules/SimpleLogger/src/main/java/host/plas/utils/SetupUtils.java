package host.plas.utils;

import host.plas.SimpleLogger;
import host.plas.config.WebhookSetup;
import host.plas.data.DifferedSetup;
import singularity.events.server.CosmicLogPopEvent;
import singularity.logging.LogIntent;

import java.util.concurrent.ConcurrentSkipListSet;

public class SetupUtils {
    public static String getLoggerString(CosmicLogPopEvent event) {
        WebhookSetup webhookSetup = SimpleLogger.getWebhookConfig().getLogsSetup();

        DifferedSetup setup = webhookSetup.getChat();

        ConcurrentSkipListSet<LogIntent> allowedIntents = SimpleLogger.getWebhookConfig().getLogListenTo();
        StringBuilder contentBuilder = new StringBuilder();
        event.forEachLog((i, e) -> {
            if (! allowedIntents.contains(e.getIntent())) return;

            String content = setup.getPlain().getContent().replace("%content%", e.getMessage());
            if (! contentBuilder.toString().isBlank()) {
                contentBuilder.append("\n");
            }

            contentBuilder.append(content);
        });

        return contentBuilder.toString();
    }
}
