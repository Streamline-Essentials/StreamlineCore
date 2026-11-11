package host.plas.discord.messaging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordHandler;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.awt.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;

public class DiscordMessenger {
    /** Links Discord accepts in embeds; anything else (e.g. an unresolved placeholder) is left out. */
    private static final Pattern URL = Pattern.compile("^(https?|attachment)://\\S+$", Pattern.CASE_INSENSITIVE);

    public static SingleSet<MessageCreateData, BotMessageConfig> simpleMessage(String message, CosmicSender on, boolean format) {
        if (format) message = ModuleUtils.replaceAllPlayerBungee(on, message);

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.addContent(BotMessages.truncate(message, 2000));

        return new SingleSet<>(builder.build(), new BotMessageConfig(new JsonObject()));
    }

    public static void message(long channelId, MessageCreateData data, BotMessageConfig config, long... threadedUsers) {
        TextChannel channel = DiscordHandler.getTextChannelById(channelId);
        if (channel == null) {
            StreamlineDiscord.getInstance().logWarning("Tried to send a message to TextChannel with ID of '" + channelId + "', but failed.");
            return;
        }
        MessageChannel finalChannel = channel;

        if (threadedUsers != null && threadedUsers.length > 0) {
            ThreadChannel threadChannel = channel.createThreadChannel("verify", true).complete();
            for (long userId : threadedUsers) {
                threadChannel.addThreadMemberById(userId).queue();
            }
            finalChannel = threadChannel;
        }

        finalChannel.sendMessage(data).queue();
        if (config.isAvatarChange()) {
            DiscordHandler.updateBotAvatar(config.getChangeAfterMessage());
        }
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> simpleMessage(String message, CosmicSender user) {
        return simpleMessage(message, user, true);
    }

    public static void sendSimpleMessage(long channelId, String message, CosmicSender user) {
        SingleSet<MessageCreateData, BotMessageConfig> data = simpleMessage(message, user);
        message(channelId, data.getKey(), data.getValue());
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> simpleMessage(String message, boolean format) {
        return simpleMessage(message, UserUtils.getConsole(), format);
    }

    public static void sendSimpleMessage(long channelId, String message, boolean format) {
        SingleSet<MessageCreateData, BotMessageConfig> data = simpleMessage(message, format);
        message(channelId, data.getKey(), data.getValue());
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> simpleMessage(String message) {
        return simpleMessage(message, UserUtils.getConsole(), true);
    }

    public static void sendSimpleMessage(long channelId, String message) {
        SingleSet<MessageCreateData, BotMessageConfig> data = simpleMessage(message);
        message(channelId, data.getKey(), data.getValue());
    }

    public static void sendSimpleEmbed(long channelId, String json) {
        SingleSet<MessageCreateData, BotMessageConfig> data = simpleEmbed(json);
        message(channelId, data.getKey(), data.getValue());
    }

    /**
     * Converts Discord-style embed JSON to an {@link EmbedBuilder}. Every key is optional:
     * {@code title}, {@code url}, {@code description}, {@code color} (a number or {@code "#rrggbb"}),
     * {@code author} ({@code name}, {@code url}, {@code icon_url}), {@code fields}
     * ({@code name}, {@code value}, {@code inline}), {@code thumbnail} and {@code image} (a link or
     * {@code {"url": ...}}), {@code footer} ({@code text}, {@code icon_url}) and {@code timestamp}
     * ({@code true} / {@code "now"}, an ISO-8601 date or epoch milliseconds). Texts are cut to Discord's
     * limits and links Discord would refuse are left out, so a malformed file still sends what it can.
     * The {@code content} text sent above the embed is read by the caller.
     *
     * @param json The embed JSON
     * @return The bot settings of the {@code bot} block and the embed
     */
    public static SingleSet<BotMessageConfig, EmbedBuilder> jsonToEmbed(JsonObject json) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        BotMessageConfig botMessageConfig = new BotMessageConfig(json);

        String title = stringOf(json.get("title"));
        if (notBlank(title)) {
            embedBuilder.setTitle(BotMessages.truncate(title, MessageEmbed.TITLE_MAX_LENGTH), link(stringOf(json.get("url"))));
        }

        JsonObject author = objectOf(json.get("author"));
        if (author != null) {
            String name = stringOf(author.get("name"));
            if (notBlank(name)) {
                String url = link(stringOf(author.has("url") ? author.get("url") : author.get("author_url")));
                String iconUrl = link(stringOf(author.get("icon_url")));
                embedBuilder.setAuthor(BotMessages.truncate(name, MessageEmbed.AUTHOR_MAX_LENGTH), url, iconUrl);
            }
        }

        String description = stringOf(json.get("description"));
        if (notBlank(description)) {
            embedBuilder.setDescription(BotMessages.truncate(description, MessageEmbed.DESCRIPTION_MAX_LENGTH));
        }

        Color color = colorOf(json.get("color"));
        if (color != null) embedBuilder.setColor(color);

        JsonElement fields = json.get("fields");
        if (fields != null && fields.isJsonArray()) {
            for (JsonElement element : fields.getAsJsonArray()) {
                JsonObject field = objectOf(element);
                if (field == null) continue;
                if (embedBuilder.getFields().size() >= 25) break;

                String name = stringOf(field.get("name"));
                String value = stringOf(field.get("value"));
                boolean inline = booleanOf(field.get("inline"));
                // Discord refuses blank field names and values; a zero-width space keeps the field.
                embedBuilder.addField(
                        notBlank(name) ? BotMessages.truncate(name, MessageEmbed.TITLE_MAX_LENGTH) : EmbedBuilder.ZERO_WIDTH_SPACE,
                        notBlank(value) ? BotMessages.truncate(value, MessageEmbed.VALUE_MAX_LENGTH) : EmbedBuilder.ZERO_WIDTH_SPACE,
                        inline);
            }
        }

        String thumbnail = link(urlOf(json.get("thumbnail")));
        if (thumbnail != null) embedBuilder.setThumbnail(thumbnail);

        String image = link(urlOf(json.get("image")));
        if (image != null) embedBuilder.setImage(image);

        JsonObject footer = objectOf(json.get("footer"));
        if (footer != null) {
            String text = stringOf(footer.get("text"));
            if (notBlank(text)) {
                embedBuilder.setFooter(BotMessages.truncate(text, MessageEmbed.TEXT_MAX_LENGTH), link(stringOf(footer.get("icon_url"))));
            }
        }

        OffsetDateTime timestamp = timestampOf(json.get("timestamp"));
        if (timestamp != null) embedBuilder.setTimestamp(timestamp);

        return new SingleSet<>(botMessageConfig, embedBuilder);
    }

    public static void sendSimpleEmbed(long channelId, String message, String title, CosmicSender on, boolean formatMessage, boolean formatTitle, String authorName, String authorUrl, String iconUrl) {
        if (formatMessage) message = ModuleUtils.stripColor(ModuleUtils.replaceAllPlayerBungee(on, message));
        if (formatTitle) title = ModuleUtils.stripColor(ModuleUtils.replaceAllPlayerBungee(on, title));

        TextChannel channel = DiscordHandler.getTextChannelById(channelId);
        if (channel == null) {
            StreamlineDiscord.getInstance().logWarning("Tried to send a message to TextChannel with ID of '" + channelId + "', but failed.");
            return;
        }

        MessageCreateBuilder builder = new MessageCreateBuilder(); // https://javacord.org/wiki/basic-tutorials/message-builder.html
        builder.addEmbeds(new EmbedBuilder()
                .setAuthor(ModuleUtils.stripColor(authorName), link(authorUrl), link(iconUrl))
                .setTitle(title)
                .setDescription(message).build()
        );
        MessageCreateData mess = builder.build();

        channel.sendMessage(mess).queue();
    }

    public static void sendSimpleEmbed(long channelId, String message, String title, CosmicSender on, boolean formatMessage, boolean formatTitle) {
        sendSimpleEmbed(channelId, message, title, on, formatMessage, formatTitle,
                on.getCurrentName(), "https://dsc.gg/streamline-project", ModuleUtils.replaceAllPlayerBungee(on, "%discord_user_avatar_url%"));
    }

    /** A message from embed JSON text, including its optional {@code content} line. */
    public static SingleSet<MessageCreateData, BotMessageConfig> simpleEmbed(String jsonString) {
        JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
        SingleSet<BotMessageConfig, EmbedBuilder> set = jsonToEmbed(json);

        MessageCreateBuilder builder = new MessageCreateBuilder(); // https://javacord.org/wiki/basic-tutorials/message-builder.html
        String content = stringOf(json.get("content"));
        if (notBlank(content)) builder.setContent(BotMessages.truncate(content, 2000));
        if (! set.getValue().isEmpty()) builder.setEmbeds(List.of(set.getValue().build()));

        return new SingleSet<>(builder.build(), set.getKey());
    }

    /** The text of a JSON string, number or boolean; null for anything else. */
    public static String stringOf(JsonElement element) {
        if (element == null || ! element.isJsonPrimitive()) return null;
        return element.getAsString();
    }

    private static JsonObject objectOf(JsonElement element) {
        if (element == null || ! element.isJsonObject()) return null;
        return element.getAsJsonObject();
    }

    private static boolean booleanOf(JsonElement element) {
        if (element == null || ! element.isJsonPrimitive()) return false;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        return Boolean.parseBoolean(primitive.getAsString().trim());
    }

    /** A link given as a string or as {@code {"url": ...}}. */
    private static String urlOf(JsonElement element) {
        JsonObject object = objectOf(element);
        if (object != null) return stringOf(object.get("url"));
        return stringOf(element);
    }

    private static Color colorOf(JsonElement element) {
        if (element == null || ! element.isJsonPrimitive()) return null;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            if (primitive.isNumber()) return new Color(primitive.getAsInt());

            String text = primitive.getAsString().trim();
            if (text.isEmpty()) return null;
            if (text.startsWith("#")) return new Color(Integer.parseInt(text.substring(1), 16));
            if (text.toLowerCase().startsWith("0x")) return new Color(Integer.parseInt(text.substring(2), 16));
            if (text.matches("[0-9]+")) return new Color(Integer.parseInt(text));
            return new Color(Integer.parseInt(text, 16));
        } catch (Exception e) {
            return null;
        }
    }

    private static OffsetDateTime timestampOf(JsonElement element) {
        if (element == null || ! element.isJsonPrimitive()) return null;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            if (primitive.isBoolean()) return primitive.getAsBoolean() ? OffsetDateTime.now(ZoneOffset.UTC) : null;
            if (primitive.isNumber()) return OffsetDateTime.ofInstant(Instant.ofEpochMilli(primitive.getAsLong()), ZoneOffset.UTC);

            String text = primitive.getAsString().trim();
            if (text.isEmpty() || text.equalsIgnoreCase("false")) return null;
            if (text.equalsIgnoreCase("now") || text.equalsIgnoreCase("true")) return OffsetDateTime.now(ZoneOffset.UTC);
            if (text.matches("[0-9]+")) return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), ZoneOffset.UTC);
            return OffsetDateTime.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static String link(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        return URL.matcher(trimmed).matches() ? trimmed : null;
    }

    private static boolean notBlank(String text) {
        return text != null && ! text.isBlank();
    }
}
