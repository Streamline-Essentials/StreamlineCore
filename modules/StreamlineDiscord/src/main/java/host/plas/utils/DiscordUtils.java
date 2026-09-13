package host.plas.utils;

import host.plas.discord.DiscordHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DiscordUtils {
    public static Optional<JDA> getJDA() {
        return Optional.ofNullable(DiscordHandler.getDiscordAPI());
    }

    public static JDA getJDAOrNull() {
        return getJDA().orElse(null);
    }

    public static Optional<Guild> getGuildById(@Nullable Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getGuildById(id));
    }

    public static Optional<Role> getRoleById(@Nullable Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getRoleById(id));
    }

    public static Optional<User> getUserById(@Nullable Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getUserById(id));
    }

    public static Optional<TextChannel> getTextChannelById(@Nullable Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getTextChannelById(id));
    }

    public static Optional<StageChannel> getStageById(@Nullable Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getStageChannelById(id));
    }

    public static Optional<VoiceChannel> getVoiceChannelById(Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getVoiceChannelById(id));
    }

    public static Optional<Channel> getBasicChannelById(@Nullable Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        Optional<Channel> channel = Optional.empty();
        channel = getTextChannelById(id).map(s -> s);
        if (channel.isEmpty()) {
            channel = getStageById(id).map(s -> s);
            if (channel.isEmpty()) {
                channel = getVoiceChannelById(id).map(s -> s);
            }
        }

        return channel;
    }

    public static Optional<MessageChannel> getMessageChannelById(@Nullable Long id) {
        return getBasicChannelById(id).filter(c -> c instanceof MessageChannel).map(c -> (MessageChannel) c);
    }

    public static Optional<Message> getMessageById(@Nullable Long channelId, @Nullable Long id) {
        Optional<MessageChannel> channel = getMessageChannelById(channelId);
        if (channel.isEmpty()) return Optional.empty();

        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(channel.get().retrieveMessageById(id).complete());
    }

    public static Optional<Category> getCategoryById(Long id) {
        if (id == null) return Optional.empty();
        if (getJDA().isEmpty()) return Optional.empty();

        return Optional.ofNullable(getJDAOrNull().getCategoryById(id));
    }
}
