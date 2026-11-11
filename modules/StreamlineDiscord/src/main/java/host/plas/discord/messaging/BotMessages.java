package host.plas.discord.messaging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import host.plas.StreamlineDiscord;
import host.plas.config.Messages;
import host.plas.config.Messaging;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Every message the Discord bot sends, looked up by a dotted key such as {@code events.join} or
 * {@code commands.ping.response}.
 *
 * <p>Owners customise a message in two files of the module's data folder:
 * <ul>
 *     <li>{@code messages.yml} holds the plain text of every key (Discord markdown, placeholders allowed).</li>
 *     <li>{@code messaging.yml} says how each key is sent: {@code default} sends that plain text, the name of a
 *     file in {@code embeds/} ({@code .json} optional) sends that embed, and {@code disabled} / {@code off}
 *     sends nothing.</li>
 * </ul>
 *
 * <p>Code adding a new bot message calls {@link #register(String, String, String)} once (e.g. in a
 * constructor or on enable) and then {@link #build} or {@link #send} with the key. Placeholders resolve
 * in this order: the {@code extra} map's {@code %name%} entries, then Streamline placeholders relative to
 * {@code on}; colour codes are stripped afterwards, since Discord cannot show them.
 */
public final class BotMessages {
    /** The {@code messaging.yml} value that sends the plain text from {@code messages.yml}. */
    public static final String DEFAULT = "default";

    /** {@code messaging.yml} values that turn a message off. */
    private static final Set<String> DISABLED = Set.of("disabled", "disable", "off", "false", "no", "none");

    /** Discord's limit on a message's plain content. */
    private static final int CONTENT_LIMIT = 2000;

    /** Key -> {default text, default setting}, for every key registered since the module loaded. */
    private static final ConcurrentSkipListMap<String, String[]> REGISTERED = new ConcurrentSkipListMap<>();

    private BotMessages() {
    }

    /**
     * Declares a bot message and writes its defaults: {@code defaultText} to {@code messages.yml} and
     * {@code defaultEmbedOrDefault} to {@code messaging.yml}, each only where the key holds no value yet, so
     * an owner's settings are never overwritten. When the default names an embed, the shipped
     * {@code embeds/<name>.json} is copied out if missing. Safe to call more than once; keys registered before the
     * module enables are written to the files when it does.
     *
     * @param key                   dotted key, e.g. {@code events.join}
     * @param defaultText           plain text used for {@code default} and as the fallback when the embed is unreadable
     * @param defaultEmbedOrDefault {@code default}, {@code disabled}, or an embed file name in {@code embeds/}
     */
    public static synchronized void register(String key, String defaultText, String defaultEmbedOrDefault) {
        REGISTERED.put(key, new String[] { defaultText, defaultEmbedOrDefault });

        Messages messages = StreamlineDiscord.getMessages();
        if (messages != null) messages.ensureDefault(key, defaultText);

        Messaging messaging = StreamlineDiscord.getMessaging();
        if (messaging != null) messaging.ensureDefault(key, defaultEmbedOrDefault);

        if (isEmbedSetting(defaultEmbedOrDefault)) Embeds.ensureShipped(defaultEmbedOrDefault);
    }

    /** Every key registered since the module loaded. */
    public static Set<String> getRegisteredKeys() {
        return Collections.unmodifiableSet(REGISTERED.keySet());
    }

    /**
     * Builds the message for {@code key}.
     *
     * @param key   dotted key, registered with {@link #register(String, String, String)}
     * @param on    whose Streamline placeholders to resolve; null or the console for none in particular
     * @param extra {@code %name%} placeholders replaced before Streamline's own, keyed by {@code name}
     *              (with or without the percent signs); may be null
     * @return the message and its bot settings, or null when the key is disabled or resolves to nothing
     */
    public static SingleSet<MessageCreateData, BotMessageConfig> build(String key, CosmicSender on, Map<String, String> extra) {
        String setting = getSetting(key);
        if (isDisabled(setting)) return null;

        if (isEmbedSetting(setting)) {
            SingleSet<MessageCreateData, BotMessageConfig> embed = buildEmbed(setting, on, extra);
            if (embed != null) return embed;
            StreamlineDiscord.getInstance().logWarning("Sending the plain text of '" + key + "' from messages.yml instead of its embed '" + Embeds.fileName(setting) + "'.");
        }

        return buildText(getText(key), on, extra);
    }

    /**
     * Builds the message for {@code key} and sends it to the Discord text channel {@code channelId}.
     * Sends nothing when the key is disabled. Parameters as in {@link #build}.
     */
    public static void send(String key, long channelId, CosmicSender on, Map<String, String> extra) {
        SingleSet<MessageCreateData, BotMessageConfig> data = build(key, on, extra);
        if (data == null) return;

        DiscordMessenger.message(channelId, data.getKey(), data.getValue());
    }

    /** {@link #build} with no extra placeholders. */
    public static SingleSet<MessageCreateData, BotMessageConfig> build(String key, CosmicSender on) {
        return build(key, on, null);
    }

    /**
     * Builds a message from the embed file {@code fileName} in {@code embeds/}, or null when the file is
     * missing, unreadable or holds nothing to send.
     */
    public static SingleSet<MessageCreateData, BotMessageConfig> buildEmbed(String fileName, CosmicSender on, Map<String, String> extra) {
        JsonObject json = Embeds.read(fileName);
        if (json == null) return null;

        CosmicSender sender = on == null ? UserUtils.getConsole() : on;
        JsonObject resolved = resolveJson(json, null, sender, extra).getAsJsonObject();

        try {
            SingleSet<BotMessageConfig, EmbedBuilder> set = DiscordMessenger.jsonToEmbed(resolved);
            String content = DiscordMessenger.stringOf(resolved.get("content"));

            MessageCreateBuilder builder = new MessageCreateBuilder();
            if (content != null && ! content.isBlank()) builder.setContent(truncate(content, CONTENT_LIMIT));
            if (! set.getValue().isEmpty()) builder.setEmbeds(set.getValue().build());
            if (builder.isEmpty()) {
                StreamlineDiscord.getInstance().logWarning("The embed file 'embeds/" + Embeds.fileName(fileName) + "' has nothing to send.");
                return null;
            }

            return new SingleSet<>(builder.build(), set.getKey());
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Could not build the embed 'embeds/" + Embeds.fileName(fileName) + "': " + e.getMessage());
            return null;
        }
    }

    /** Builds a plain-text message, or null when the text resolves to nothing. */
    public static SingleSet<MessageCreateData, BotMessageConfig> buildText(String text, CosmicSender on, Map<String, String> extra) {
        if (text == null) return null;

        CosmicSender sender = on == null ? UserUtils.getConsole() : on;
        String resolved = ModuleUtils.stripColor(resolve(text, sender, extra));
        if (resolved == null || resolved.isBlank()) return null;

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setContent(truncate(resolved, CONTENT_LIMIT));
        return new SingleSet<>(builder.build(), new BotMessageConfig(new JsonObject()));
    }

    /** The plain text of {@code key} in {@code messages.yml}, or its registered default. */
    public static String getText(String key) {
        Messages messages = StreamlineDiscord.getMessages();
        String text = messages == null ? null : messages.getText(key);
        if (text != null) return text;

        String[] defaults = REGISTERED.get(key);
        if (defaults != null) return defaults[0];

        StreamlineDiscord.getInstance().logWarning("No text for the bot message '" + key + "' in messages.yml.");
        return null;
    }

    /** The {@code messaging.yml} setting of {@code key}, or its registered default, or {@code default}. */
    public static String getSetting(String key) {
        Messaging messaging = StreamlineDiscord.getMessaging();
        String setting = messaging == null ? null : messaging.getSetting(key);
        if (setting != null && ! setting.isEmpty()) return setting;

        String[] defaults = REGISTERED.get(key);
        if (defaults != null) return defaults[1];

        return DEFAULT;
    }

    public static boolean isDisabled(String setting) {
        return setting != null && DISABLED.contains(setting.toLowerCase(Locale.ROOT));
    }

    /** Whether a {@code messaging.yml} value names an embed file (rather than {@code default} or off). */
    public static boolean isEmbedSetting(String setting) {
        return setting != null && ! setting.isBlank() && ! setting.equalsIgnoreCase(DEFAULT) && ! isDisabled(setting);
    }

    /**
     * Replaces the {@code extra} placeholders, then Streamline's placeholders for {@code on}. Extra values are
     * inserted as they are and never parsed for placeholders themselves, since they often hold player text
     * (chat, death messages, item names): each one stands in as an inert token during Streamline's pass.
     */
    public static String resolve(String text, CosmicSender on, Map<String, String> extra) {
        if (text == null) return null;

        String result = text;
        List<String> values = new ArrayList<>();
        if (extra != null) {
            for (Map.Entry<String, String> entry : extra.entrySet()) {
                if (entry.getKey() == null) continue;
                String name = entry.getKey();
                if (! name.startsWith("%")) name = "%" + name + "%";
                if (! result.contains(name)) continue;

                result = result.replace(name, token(values.size()));
                values.add(entry.getValue() == null ? "" : entry.getValue());
            }
        }

        result = ModuleUtils.replacePlaceholders(on == null ? UserUtils.getConsole() : on, result);

        for (int i = 0; i < values.size(); i++) {
            result = result.replace(token(i), values.get(i));
        }
        return result;
    }

    /** A private-use marker no placeholder pattern matches. */
    private static String token(int index) {
        return "\uE000" + index + "\uE001";
    }

    /**
     * Placeholders from alternating names and values: {@code placeholders("this_message", msg, "this_server", s)}.
     */
    public static Map<String, String> placeholders(String... namesAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            map.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return map;
    }

    /**
     * Resolves placeholders in every string of an embed after it is parsed, so a value holding quotes or
     * new lines cannot break the JSON. Colour codes are stripped everywhere but in links.
     */
    private static JsonElement resolveJson(JsonElement element, String fieldName, CosmicSender on, Map<String, String> extra) {
        if (element == null || element.isJsonNull()) return element;

        if (element.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                copy.add(entry.getKey(), resolveJson(entry.getValue(), entry.getKey(), on, extra));
            }
            return copy;
        }

        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                copy.add(resolveJson(child, fieldName, on, extra));
            }
            return copy;
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (! primitive.isString()) return primitive;

        String resolved = resolve(primitive.getAsString(), on, extra);
        if (! isLinkField(fieldName)) resolved = ModuleUtils.stripColor(resolved);
        return new JsonPrimitive(resolved == null ? "" : resolved);
    }

    private static boolean isLinkField(String fieldName) {
        if (fieldName == null) return false;
        String name = fieldName.toLowerCase(Locale.ROOT);
        return name.contains("url") || name.equals("thumbnail") || name.equals("image")
                || name.startsWith("to-");
    }

    static String truncate(String text, int limit) {
        if (text == null || text.length() <= limit) return text;
        return text.substring(0, limit - 1) + "…";
    }

    /**
     * Registers every built-in bot message. Runs on enable, after {@code messages.yml} and
     * {@code messaging.yml} are loaded and old settings are migrated.
     */
    public static void registerDefaults() {
        String usageFooter = "\n\nMore help: https://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup";

        register("verification.success.discord", "You have successfully verified your account!", "verify-success-response");
        register("verification.failure.generic", "Incorrect code!", "verify-failure-response");
        register("verification.failure.already-verified", "You have already verified your account!", "verify-already-response");

        register("unverification.success.discord", "You have successfully unverified your account!", "unverify-success-response");
        register("unverification.failure.generic", "There was an issue handling that request...", "unverify-failure-response");
        register("unverification.failure.already-unverified", "You have already unverified your account!", "unverify-already-response");

        register("events.join", ":inbox_tray: **%this_player%** joined %this_server%.", DEFAULT);
        register("events.leave", ":outbox_tray: **%this_player%** left %this_server%.", DEFAULT);
        register("events.advancement", ":trophy: **%this_player%** earned **%this_advancement_title%** on %this_server%.", DEFAULT);
        register("events.death", ":skull: %this_death_message%%this_server_tag%", DEFAULT);
        register("events.server-start", ":green_circle: **%this_server_name%** is online.", DEFAULT);
        register("events.server-stop", ":red_circle: **%this_server_name%** went offline.", DEFAULT);

        register("commands.ping.response", "Current latency is **%discord_bot_ping%**ms", "ping-response");
        register("commands.help.response", "Bot setup help: https://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup\nSupport Discord: https://dsc.gg/streamline", "help-response");
        register("commands.reload.response", "Reloaded Streamline!", "reload-response");
        register("commands.restart.response", "Restarted the bot!", "restart-response");

        String channelUsage = "**Usage:**\n`%discord_bot_prefix%%this_command_label% set <type> <identifier>`\n`%discord_bot_prefix%%this_command_label% remove [<type> <identifier>]`\nTypes: GLOBAL_NATIVE, SPECIFIC_NATIVE, PERMISSION, GUILD, PARTY, SPECIFIC_HANDLED, DISCORD_TEXT";
        String setText = "Linked %this_channel_id% to %this_type% `%this_identifier%` with the format `%this_format%`.";
        String removeText = "Unlinked %this_channel_id% from %this_type% `%this_identifier%`.";
        String noneText = "Sorry, but I could not find that route...";

        register("commands.channel.info", channelUsage + usageFooter, "channel-info-response");
        register("commands.channel.set", setText, "channel-set-response");
        register("commands.channel.remove", removeText, "channel-remove-response");
        register("commands.channel.none", noneText, "channel-none-response");
        register("commands.channel.nothing-to-remove", "No channel found to remove!", DEFAULT);

        register("commands.channelset.info", "**Usage:** `%discord_bot_prefix%%this_command_label% <type> <identifier>`" + usageFooter, "channelset-info-response");
        register("commands.channelset.set", setText, "channel-set-response");
        register("commands.channelset.none", noneText, "channel-none-response");

        register("commands.channelremove.info", "**Usage:** `%discord_bot_prefix%%this_command_label% [<type> <identifier>]`" + usageFooter, "channelremove-info-response");
        register("commands.channelremove.remove", removeText, "channel-remove-response");
        register("commands.channelremove.none", noneText, "channel-none-response");
        register("commands.channelremove.nothing-to-remove", "No channel found to remove!", DEFAULT);

        register("commands.channels.list", "**All routes**\n%this_list_all%", "channel-list-response");
        register("commands.channels.list-here", "**Routes in this channel**\n%this_list_here%", "channel-list-here-response");

        register("commands.events.info", "**Usage:**\n`%discord_bot_prefix%%this_command_label% add <type> <identifier>`\n`%discord_bot_prefix%%this_command_label% remove <type> <identifier>`\n`%discord_bot_prefix%%this_command_label% clear all <identifier>`\nTypes: LOGIN, LOGOUT, ADVANCEMENT, DEATH, SERVER_START, SERVER_STOP, all", "event-info-response");
        register("commands.events.add", "Added the event(s) for route `%this_identifier%`:%this_type_named%", "event-add-response");
        register("commands.events.remove", "Removed the event(s) for route `%this_identifier%`:%this_type_named%", "event-remove-response");
        register("commands.events.clear", "Cleared the events of route `%this_identifier%`.", "event-clear-response");
        register("commands.events.none", noneText, "event-none-response");

        String contact = "Please tell an administrator to contact @drakdv on Discord.";
        register("commands.errors.server-only", "This command can only be used in a server channel.", DEFAULT);
        register("commands.errors.member-lookup-failed", "Error: could not look up your server membership. " + contact, DEFAULT);
        register("commands.errors.owner-only", "Only the server owner or a bot super admin can use this command. "
                + "Add a role's ID to 'bot.super-admin-roles' in the Discord module's config.yml, "
                + "or set 'permissions.default' in '%this_command%.yml' to a role's ID.", DEFAULT);
        register("commands.errors.role-missing", "Error: the role '%this_role_id%' configured for this command does not exist. " + contact, DEFAULT);
        register("commands.errors.no-role", "You need the '%this_role_name%' role to use this command.", DEFAULT);

        // Keys registered before the configs loaded so far live only in memory.
        REGISTERED.forEach((key, defaults) -> register(key, defaults[0], defaults[1]));

        // A route's format may name an embed ('--file:default-route-message.json'); this one ships as the example.
        Embeds.ensureShipped("default-route-message");

        // Embed versions of the game event messages, for owners who switch an event to one in messaging.yml.
        for (String embed : new String[] { "on-login", "on-logout", "on-advancement", "on-death", "on-server-start", "on-server-stop" }) {
            Embeds.ensureShipped(embed);
        }
    }
}
