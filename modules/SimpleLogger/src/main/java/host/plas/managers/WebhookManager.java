package host.plas.managers;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import host.plas.SimpleLogger;
import host.plas.config.WebhookConfig;
import host.plas.config.WebhookSetup;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.data.DifferedSetup;
import host.plas.data.WebhookType;
import host.plas.utils.SetupUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.events.server.CosmicLogPopEvent;
import singularity.events.server.ServerLogTextEvent;
import singularity.logging.LogIntent;
import singularity.modules.ModuleUtils;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class WebhookManager {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, WebhookClient> rawClients = new ConcurrentSkipListMap<>();

    public static WebhookClient getClient(String url) {
        if (rawClients == null) rawClients = new ConcurrentSkipListMap<>();

        WebhookClient rawClient = rawClients.get(url);
        if (rawClient == null) {
            WebhookClientBuilder builder = new WebhookClientBuilder(url);
            builder.setThreadFactory((job) -> {
                Thread thread = new Thread(job);
                thread.setName("SCL-Webhook-Thread");
                thread.setDaemon(true);
                return thread;
            });
            builder.setWait(true);
            rawClient = builder.build();

            rawClients.put(url, rawClient);

            SimpleLogger.getInstance().logInfo("Created WebhookClient.");
        }

        return rawClient;
    }

    public static void sendWebhookText(CosmicSender sender, String message) {
        boolean isCommand = message.startsWith("/");

        SimpleLogger.getWebhookConfig().getWebookSetups().forEach(setup -> {
            try {
                if (setup.getType() == WebhookType.GLOBAL) {
                    WebhookClient client = getClient(setup.getUrl());
                    if (isCommand && setup.getCommands().isEnabled()) {
                        sendWebhook(client, setup.getCommands(), sender, message);
                    } else if (! isCommand && setup.getChat().isEnabled()) {
                        sendWebhook(client, setup.getChat(), sender, message);
                    }
                } else {
                    if (sender.getServerName() != null && sender.getServerName().equalsIgnoreCase(setup.getIdentifier())) {
                        WebhookClient client = getClient(setup.getUrl());
                        if (isCommand && setup.getCommands().isEnabled()) {
                            sendWebhook(client, setup.getCommands(), sender, message);
                        } else if (! isCommand && setup.getChat().isEnabled()) {
                            sendWebhook(client, setup.getChat(), sender, message);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendWebhookLogText(CosmicLogPopEvent event) {
        WebhookSetup webhookSetup = SimpleLogger.getWebhookConfig().getLogsSetup();
        sendWebhookLogText(webhookSetup, SetupUtils.getLoggerString(event));
    }

    public static void sendWebhookLogText(WebhookSetup webhookSetup, String logMessage) {
        try {
            WebhookClient client = getClient(webhookSetup.getUrl());

            DifferedSetup setup = webhookSetup.getChat();

            if (setup == null) {
                SimpleLogger.getInstance().logInfo("WebhookSetup is null.");
                return;
            }

            if (setup.getPlain().isEnabled()) {
                WebhookMessageBuilder builder = new WebhookMessageBuilder();

                String content = logMessage;

                if (content.endsWith("\n")) {
                    content = content.substring(0, content.length() - "\n".length());
                }
                String contentPart = content;
                while (! contentPart.isEmpty()) {
                    String part = contentPart.substring(0, Math.min(1950, contentPart.length()));
                    int length = part.length();
                    if (length == 1950 && part.contains("\n")) {
                        contentPart = contentPart.substring(0, part.lastIndexOf("\n"));
                    }

                    String finalContentPart = contentPart;

                    WebhookMessage message = new WebhookMessageBuilder()
                            .setContent(finalContentPart)
                            .build();
                    client.send(message);

                    contentPart = contentPart.substring(finalContentPart.length());
                }
            }

            if (setup.getEmbed().isEnabled()) {
                int color = setup.getEmbed().getColor();
                String title = setup.getEmbed().getTitle();

                String avatar = setup.getEmbed().getPicture();

                String content = logMessage;

                if (content.endsWith("\n")) {
                    content = content.substring(0, content.length() - "\n".length());
                }
                String contentPart = content;
                while (! contentPart.isEmpty()) {
                    String part = contentPart.substring(0, Math.min(1950, contentPart.length()));
                    int length = part.length();
                    if (length == 1950 && part.contains("\n")) {
                        contentPart = contentPart.substring(0, part.lastIndexOf("\n"));
                    }

                    String finalContentPart = contentPart;

                    WebhookEmbed embed = new WebhookEmbedBuilder()
                            .setDescription(finalContentPart)
                            .setColor(color)
                            .setTitle(new WebhookEmbed.EmbedTitle(title, null))
                            .setFooter(new WebhookEmbed.EmbedFooter("Made possible by StreamlineCore.", avatar))
                            .build();

                    client.send(embed);

                    contentPart = contentPart.substring(finalContentPart.length());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendWebhook(String webhookUrl, DifferedSetup setup, CosmicSender sender, String message) {
        sendWebhook(getClient(webhookUrl), setup, sender, message);
    }

    public static void sendWebhook(WebhookClient client, DifferedSetup setup, CosmicSender sender, String message) {
        if (setup == null) {
            SimpleLogger.getInstance().logInfo("WebhookSetup is null.");
            return;
        }

        if (setup.getPlain().isEnabled()) {
            String name = setup.getPlain().getName();
            String content = setup.getPlain().getContent().replace("%content%", message);
            String avatar = setup.getPlain().getPicture();

            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            if (sender != null) {
                avatar = avatar.replace("%streamline_user_uuid%", sender.getUuid());

                name = ModuleUtils.replacePlaceholders(sender, name);
                content = ModuleUtils.replacePlaceholders(sender, content);
                avatar = ModuleUtils.replacePlaceholders(sender, avatar);

                builder = builder
                        .setUsername(name)
                        .setContent(content)
                        .setAvatarUrl(avatar);
            } else {
                builder = builder
                        .setContent(content);
            }

            client.send(builder.build());
        }

        if (setup.getEmbed().isEnabled()) {
            int color = setup.getEmbed().getColor();
            String title = setup.getEmbed().getTitle();
            String description = setup.getEmbed().getDescription().replace("%content%", message);
            String avatar = setup.getEmbed().getPicture();

            WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                    .setColor(color)
                    .setTitle(new WebhookEmbed.EmbedTitle(title, null))
                    ;
            if (sender != null) {
                avatar = avatar
                        .replace("%streamline_user_uuid%", sender.getUuid());

                title = ModuleUtils.replacePlaceholders(sender, title);
                description = ModuleUtils.replacePlaceholders(sender, description);
                avatar = ModuleUtils.replacePlaceholders(sender, avatar);

                builder = builder
                        .setDescription(description)
                        .setFooter(new WebhookEmbed.EmbedFooter("Made possible by StreamlineCore.", avatar))
                        ;
            } else {
                builder = builder
                        .setDescription(description)
                        .setFooter(new WebhookEmbed.EmbedFooter("Made possible by StreamlineCore.", null))
                        ;
            }

            client.send(builder.build());
        }
    }

    public static void sendChannelMessage(ConfiguredChatChannel channel, CosmicSender sender, String message) {
        SimpleLogger.getWebhookConfig().getMessageChannelSetups().forEach(setup -> {
            try {
                if (setup.getIdentifier().equals(channel.getIdentifier())) {
                    WebhookClient client = getClient(setup.getUrl());
                    if (setup.getChat().isEnabled()) sendWebhook(client, setup.getChat(), sender, message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
