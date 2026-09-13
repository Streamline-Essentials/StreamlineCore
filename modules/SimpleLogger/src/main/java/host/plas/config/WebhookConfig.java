package host.plas.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.SimpleLogger;
import host.plas.data.*;
import singularity.logging.LogIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class WebhookConfig extends SimpleConfiguration {
    public WebhookConfig() {
        super("webhooks.yml", SimpleLogger.getInstance(), true);
    }

    @Override
    public void init() {
        getGlobalWebhookUrl();
        getGlobalCommandsPlainEnabled();
        getGlobalCommandsPlainName();
        getGlobalCommandsPlainPicture();
        getGlobalCommandsPlainContent();

        getGlobalCommandsEnabled();

        getGlobalCommandsEmbedEnabled();
        getGlobalCommandsEmbedColor();
        getGlobalCommandsEmbedTitle();
        getGlobalCommandsEmbedDescription();
        getGlobalCommandsEmbedPicture();

        getGlobalChatPlainEnabled();
        getGlobalChatPlainName();
        getGlobalChatPlainPicture();
        getGlobalChatPlainContent();

        getGlobalChatEnabled();

        getGlobalChatEmbedEnabled();
        getGlobalChatEmbedColor();
        getGlobalChatEmbedTitle();
        getGlobalChatEmbedDescription();
        getGlobalChatEmbedPicture();

        getServerSetups();

        getMessageChannelSetups();

        getLogsSetup();
        getLogListenTo();
    }

    public String getGlobalWebhookUrl() {
        reloadResource();

        return getOrSetDefault("global.url", "WEBHOOK_URL");
    }

    public boolean getGlobalCommandsEnabled() {
        reloadResource();

        return getOrSetDefault("global.commands.enabled", true);
    }

    public boolean getGlobalCommandsPlainEnabled() {
        reloadResource();

        return getOrSetDefault("global.commands.plain.enabled", true);
    }

    public String getGlobalCommandsPlainName() {
        reloadResource();

        return getOrSetDefault("global.commands.plain.name", "%streamline_user_absolute%");
    }

    public String getGlobalCommandsPlainPicture() {
        reloadResource();

        return getOrSetDefault("global.commands.plain.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public String getGlobalCommandsPlainContent() {
        reloadResource();

        return getOrSetDefault("global.commands.plain.content", "%this_command%");
    }

    public boolean getGlobalCommandsEmbedEnabled() {
        reloadResource();

        return getOrSetDefault("global.commands.embed.enabled", false);
    }

    public int getGlobalCommandsEmbedColor() {
        reloadResource();

        return getOrSetDefault("global.commands.embed.color", 0);
    }

    public String getGlobalCommandsEmbedTitle() {
        reloadResource();

        return getOrSetDefault("global.commands.embed.title", "%streamline_user_absolute%");
    }

    public String getGlobalCommandsEmbedDescription() {
        reloadResource();

        return getOrSetDefault("global.commands.embed.description", "%this_command%");
    }

    public String getGlobalCommandsEmbedPicture() {
        reloadResource();

        return getOrSetDefault("global.commands.embed.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public boolean getGlobalChatEnabled() {
        reloadResource();

        return getOrSetDefault("global.chat.enabled", true);
    }

    public boolean getGlobalChatPlainEnabled() {
        reloadResource();

        return getOrSetDefault("global.chat.plain.enabled", true);
    }

    public String getGlobalChatPlainName() {
        reloadResource();

        return getOrSetDefault("global.chat.plain.name", "%streamline_user_absolute%");
    }

    public String getGlobalChatPlainPicture() {
        reloadResource();

        return getOrSetDefault("global.chat.plain.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public String getGlobalChatPlainContent() {
        reloadResource();

        return getOrSetDefault("global.chat.plain.content", "%this_command%");
    }

    public boolean getGlobalChatEmbedEnabled() {
        reloadResource();

        return getOrSetDefault("global.chat.embed.enabled", false);
    }

    public int getGlobalChatEmbedColor() {
        reloadResource();

        return getOrSetDefault("global.chat.embed.color", 0);
    }

    public String getGlobalChatEmbedTitle() {
        reloadResource();

        return getOrSetDefault("global.chat.embed.title", "%streamline_user_absolute%");
    }

    public String getGlobalChatEmbedDescription() {
        reloadResource();

        return getOrSetDefault("global.chat.embed.description", "%this_command%");
    }

    public String getGlobalChatEmbedPicture() {
        reloadResource();

        return getOrSetDefault("global.chat.embed.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public WebhookSetup getGlobalSetup() {
        reloadResource();

        return new WebhookSetup(
                WebhookType.GLOBAL,
                "PROXY",
                getGlobalWebhookUrl(),
                new DifferedSetup(
                        getGlobalCommandsEnabled(),
                        DifferedType.COMMAND,
                        new PlainHook(
                                getGlobalCommandsPlainEnabled(),
                                getGlobalCommandsPlainName(),
                                getGlobalCommandsPlainPicture(),
                                getGlobalCommandsPlainContent()
                        ),
                        new EmbedHook(
                                getGlobalCommandsEmbedEnabled(),
                                getGlobalCommandsEmbedColor(),
                                getGlobalCommandsEmbedTitle(),
                                getGlobalCommandsEmbedDescription(),
                                getGlobalCommandsEmbedPicture()
                        )
                ),
                new DifferedSetup(
                        getGlobalChatEnabled(),
                        DifferedType.CHAT,
                        new PlainHook(
                                getGlobalChatPlainEnabled(),
                                getGlobalChatPlainName(),
                                getGlobalChatPlainPicture(),
                                getGlobalChatPlainContent()
                        ),
                        new EmbedHook(
                                getGlobalChatEmbedEnabled(),
                                getGlobalChatEmbedColor(),
                                getGlobalChatEmbedTitle(),
                                getGlobalChatEmbedDescription(),
                                getGlobalChatEmbedPicture()
                        )
                )
        );
    }

    public String getServerWebhookUrl(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".url", "WEBHOOK_URL");
    }

    public boolean getServerCommandsEnabled(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.enabled", true);
    }

    public boolean getServerCommandsPlainEnabled(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.plain.enabled", true);
    }

    public String getServerCommandsPlainName(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.plain.name", "%streamline_user_absolute%");
    }

    public String getServerCommandsPlainPicture(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.plain.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public String getServerCommandsPlainContent(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.plain.content", "%this_command%");
    }

    public boolean getServerCommandsEmbedEnabled(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.embed.enabled", false);
    }

    public int getServerCommandsEmbedColor(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.embed.color", 0);
    }

    public String getServerCommandsEmbedTitle(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.embed.title", "%streamline_user_absolute%");
    }

    public String getServerCommandsEmbedDescription(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.embed.description", "%this_command%");
    }

    public String getServerCommandsEmbedPicture(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".commands.embed.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public boolean getServerChatEnabled(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.enabled", true);
    }

    public boolean getServerChatPlainEnabled(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.plain.enabled", true);
    }

    public String getServerChatPlainName(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.plain.name", "%streamline_user_absolute%");
    }

    public String getServerChatPlainPicture(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.plain.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public String getServerChatPlainContent(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.plain.content", "%this_command%");
    }

    public boolean getServerChatEmbedEnabled(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.embed.enabled", false);
    }

    public int getServerChatEmbedColor(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.embed.color", 0);
    }

    public String getServerChatEmbedTitle(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.embed.title", "%streamline_user_absolute%");
    }

    public String getServerChatEmbedDescription(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.embed.description", "%this_command%");
    }

    public String getServerChatEmbedPicture(String server) {
        reloadResource();

        return getOrSetDefault("servers." + server + ".chat.embed.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public WebhookSetup getServerSetup(String server) {
        return new WebhookSetup(
                WebhookType.SERVER,
                server,
                getServerWebhookUrl(server),
                new DifferedSetup(
                        getServerCommandsEnabled(server),
                        DifferedType.COMMAND,
                        new PlainHook(
                                getServerCommandsPlainEnabled(server),
                                getServerCommandsPlainName(server),
                                getServerCommandsPlainPicture(server),
                                getServerCommandsPlainContent(server)
                        ),
                        new EmbedHook(
                                getServerCommandsEmbedEnabled(server),
                                getServerCommandsEmbedColor(server),
                                getServerCommandsEmbedTitle(server),
                                getServerCommandsEmbedDescription(server),
                                getServerCommandsEmbedPicture(server)
                        )
                ),
                new DifferedSetup(
                        getServerChatEnabled(server),
                        DifferedType.CHAT,
                        new PlainHook(
                                getServerChatPlainEnabled(server),
                                getServerChatPlainName(server),
                                getServerChatPlainPicture(server),
                                getServerChatPlainContent(server)
                        ),
                        new EmbedHook(
                                getServerChatEmbedEnabled(server),
                                getServerChatEmbedColor(server),
                                getServerChatEmbedTitle(server),
                                getServerChatEmbedDescription(server),
                                getServerChatEmbedPicture(server)
                        )
                )
        );
    }

    public ConcurrentSkipListSet<WebhookSetup> getServerSetups() {
        reloadResource();

        ConcurrentSkipListSet<WebhookSetup> r = new ConcurrentSkipListSet<>();

        singleLayerKeySet("servers").forEach(s -> {
            try {
                r.add(getServerSetup(s));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return r;
    }

    public ConcurrentSkipListSet<WebhookSetup> getWebookSetups() {
        ConcurrentSkipListSet<WebhookSetup> r = new ConcurrentSkipListSet<>();

        r.addAll(getServerSetups());
        r.add(getGlobalSetup());

        return r;
    }

    public String getMessageChannelWebhookUrl(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".url", "WEBHOOK_URL");
    }

    public boolean getMessageChannelChatEnabled(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.enabled", true);
    }

    public boolean getMessageChannelChatPlainEnabled(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.plain.enabled", true);
    }

    public String getMessageChannelChatPlainName(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.plain.name", "%streamline_user_absolute%");
    }

    public String getMessageChannelChatPlainPicture(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.plain.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public String getMessageChannelChatPlainContent(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.plain.content", "%this_command%");
    }

    public boolean getMessageChannelChatEmbedEnabled(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.embed.enabled", false);
    }

    public int getMessageChannelChatEmbedColor(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.embed.color", 0);
    }

    public String getMessageChannelChatEmbedTitle(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.embed.title", "%streamline_user_absolute%");
    }

    public String getMessageChannelChatEmbedDescription(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.embed.description", "%this_command%");
    }

    public String getMessageChannelChatEmbedPicture(String messageChannel) {
        reloadResource();

        return getOrSetDefault("message-channels." + messageChannel + ".chat.embed.picture", "https://crafatar.com/avatars/%streamline_user_uuid%?size=128&overlay");
    }

    public WebhookSetup getMessageChannelSetup(String messageChannel) {
        return new WebhookSetup(
                WebhookType.CHANNEL,
                messageChannel,
                getMessageChannelWebhookUrl(messageChannel),
                null,
                new DifferedSetup(
                        getMessageChannelChatEnabled(messageChannel),
                        DifferedType.CHAT,
                        new PlainHook(
                                getMessageChannelChatPlainEnabled(messageChannel),
                                getMessageChannelChatPlainName(messageChannel),
                                getMessageChannelChatPlainPicture(messageChannel),
                                getMessageChannelChatPlainContent(messageChannel)
                        ),
                        new EmbedHook(
                                getMessageChannelChatEmbedEnabled(messageChannel),
                                getMessageChannelChatEmbedColor(messageChannel),
                                getMessageChannelChatEmbedTitle(messageChannel),
                                getMessageChannelChatEmbedDescription(messageChannel),
                                getMessageChannelChatEmbedPicture(messageChannel)
                        )
                )
        );
    }

    public ConcurrentSkipListSet<WebhookSetup> getMessageChannelSetups() {
        reloadResource();

        ConcurrentSkipListSet<WebhookSetup> r = new ConcurrentSkipListSet<>();

        singleLayerKeySet("message-channels").forEach(s -> {
            try {
                r.add(getMessageChannelSetup(s));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return r;
    }

    public WebhookSetup getLogsSetup() {
        reloadResource();

        return new WebhookSetup(
                WebhookType.LOGS,
                "logs",
                getOrSetDefault("logs.main-url", "WEBHOOK_URL"),
                null,
                new DifferedSetup(
                        true,
                        DifferedType.CHAT,
                        new PlainHook(
                                getOrSetDefault("logs.plain.enabled", true),
                                "",
                                "",
                                getOrSetDefault("logs.plain.content", "%content%")
                        ),
                        new EmbedHook(
                                getOrSetDefault("logs.embed.enabled", false),
                                getOrSetDefault("logs.embed.color", 0),
                                getOrSetDefault("logs.embed.title", "Server Log Entry"),
                                getOrSetDefault("logs.embed.description", "%content%"),
                                getOrSetDefault("logs.embed.picture", "https://assets.bedwarssmp.net/images/bedlogo.png")
                        )
                )
        );
    }

    public WebhookSetup getLogFor(String server) {
        reloadResource();

        return new WebhookSetup(
                WebhookType.LOGS,
                "logs",
                getOrSetDefault("logs.filtered-urls." + server, "WEBHOOK_URL"),
                null,
                new DifferedSetup(
                        true,
                        DifferedType.CHAT,
                        new PlainHook(
                                getOrSetDefault("logs.plain.enabled", true),
                                "",
                                "",
                                getOrSetDefault("logs.plain.content", "%content%")
                        ),
                        new EmbedHook(
                                getOrSetDefault("logs.embed.enabled", false),
                                getOrSetDefault("logs.embed.color", 0),
                                getOrSetDefault("logs.embed.title", "Server Log Entry"),
                                getOrSetDefault("logs.embed.description", "%content%"),
                                getOrSetDefault("logs.embed.picture", "https://assets.bedwarssmp.net/images/bedlogo.png")
                        )
                )
        );
    }

    public ConcurrentSkipListSet<LogIntent> getLogListenTo() {
        reloadResource();

        ConcurrentSkipListSet<LogIntent> r = new ConcurrentSkipListSet<>();

        List<String> intents = new ArrayList<>(getOrSetDefault("logs.listen-to", List.of("INFO", "WARNING", "SEVERE", "DEBUG")));
        intents.forEach(intent -> {
            try {
                r.add(LogIntent.valueOf(intent.toUpperCase()));
            } catch (IllegalArgumentException e) {
                SimpleLogger.getInstance().logWarning("Invalid log intent in config: " + intent);
            }
        });

        return r;
    }
}
