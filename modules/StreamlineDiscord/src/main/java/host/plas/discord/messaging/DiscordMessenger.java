package host.plas.discord.messaging;

import com.google.gson.JsonArray;
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
import java.util.List;

public class DiscordMessenger {
    public static SingleSet<MessageCreateData, BotMessageConfig> simpleMessage(String message, CosmicSender on, boolean format) {
        if (format) message = ModuleUtils.replaceAllPlayerBungee(on, message);

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.addContent(message);

        // Gets the first set of json data.
        String json;
        try {
            json = message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1);
        } catch (Exception e) {
//            e.printStackTrace();
            json = "{}";
        }
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

        return new SingleSet<>(builder.build(), new BotMessageConfig(jsonObject));
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
     * Converts a {@link JsonObject} to {@link MessageEmbed}.
     * Supported Fields: Title, Author, Description, Color, Fields, Thumbnail, Footer.
     *
     * @param json The JsonObject
     * @return The Embed
     */
    public static SingleSet<BotMessageConfig, EmbedBuilder> jsonToEmbed(JsonObject json){
        EmbedBuilder embedBuilder = new EmbedBuilder();

        BotMessageConfig botMessageConfig = new BotMessageConfig(json);

        JsonPrimitive titleObj = json.getAsJsonPrimitive("title");
        if (titleObj != null){ // Make sure the object is not null before adding it onto the embed.
            embedBuilder.setTitle(titleObj.getAsString());
        }

        JsonObject authorObj = json.getAsJsonObject("author");
        if (authorObj != null) {
            String authorName = authorObj.get("name").getAsString();
            String authorUrl = authorObj.get("author_url").getAsString();
            String authorIconUrl = authorObj.get("icon_url").getAsString();
            if (authorUrl != null && authorIconUrl != null) // Make sure the icon_url is not null before adding it onto the embed. If its null then add just the author's name.
                embedBuilder.setAuthor(authorName, authorUrl, authorIconUrl);
            else
                embedBuilder.setAuthor(authorName);
        }

        JsonPrimitive descObj = json.getAsJsonPrimitive("description");
        if (descObj != null){
            embedBuilder.setDescription(descObj.getAsString());
        }

        JsonPrimitive colorObj = json.getAsJsonPrimitive("color");
        if (colorObj != null){
            Color color = new Color(colorObj.getAsInt());
            embedBuilder.setColor(color);
        }

        JsonArray fieldsArray = json.getAsJsonArray("fields");
        if (fieldsArray != null) {
            // Loop over the fields array and add each one by order to the embed.
            fieldsArray.forEach(ele -> {
                String name = ele.getAsJsonObject().get("name").getAsString();
                String content = ele.getAsJsonObject().get("value").getAsString();
                boolean inline = ele.getAsJsonObject().get("inline").getAsBoolean();
                embedBuilder.addField(name, content, inline);
            });
        }

        JsonPrimitive thumbnailObj = json.getAsJsonPrimitive("thumbnail");
        if (thumbnailObj != null){
            embedBuilder.setThumbnail(thumbnailObj.getAsString());
        }

        JsonObject footerObj = json.getAsJsonObject("footer");
        if (footerObj != null){
            String content = footerObj.get("text").getAsString();
            String footerIconUrl = footerObj.get("icon_url").getAsString();

            if (footerIconUrl != null)
                embedBuilder.setFooter(content, footerIconUrl);
            else
                embedBuilder.setFooter(content);
        }

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
                .setAuthor(ModuleUtils.stripColor(authorName), authorUrl, iconUrl)
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

    public static SingleSet<MessageCreateData, BotMessageConfig> simpleEmbed(String jsonString) {
        SingleSet<BotMessageConfig, EmbedBuilder> set = jsonToEmbed((JsonObject) new JsonParser().parse(jsonString));

        MessageCreateBuilder builder = new MessageCreateBuilder(); // https://javacord.org/wiki/basic-tutorials/message-builder.html
        builder.setEmbeds(List.of(set.getValue().build()));

        return new SingleSet<>(builder.build(), set.getKey());
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> verificationMessage(CosmicSender user, String message) {
        if (StreamlineDiscord.isJsonFile(message)) {
            String fileName = StreamlineDiscord.getJsonFile(message);

            if (! StreamlineDiscord.loadFile(fileName)) {
                StreamlineDiscord.getInstance().logInfo("Failed to load verification message file '" + fileName + "'.");
                return simpleMessage(ModuleUtils.replacePlaceholders(user, "An error occurred while loading the verification message. Please contact an administrator."), user, false);
            }

            return simpleEmbed(ModuleUtils.replacePlaceholders(user, StreamlineDiscord.getJsonFromFile(fileName)));
        } else {
            return simpleMessage(ModuleUtils.replacePlaceholders(user, message));
        }
    }

    public static void sendVerificationMessage(long userId, long channelId, CosmicSender user, String message, boolean fromCommand) {
        SingleSet<MessageCreateData, BotMessageConfig> set = verificationMessage(user, message);
        if (StreamlineDiscord.getConfig().verificationResponsesPrivate() && ! fromCommand) {
            message(channelId, set.getKey(), set.getValue(), userId);
        } else {
            message(channelId, set.getKey(), set.getValue());
        }
    }
}
