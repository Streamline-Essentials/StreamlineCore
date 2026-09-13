package host.plas.discord;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import host.plas.StreamlineDiscord;
import singularity.modules.ModuleUtils;

import java.util.List;

@Setter
@Getter
public class MessagedString {
    @NonNull
    private User author;
    @NonNull
    private MessageChannel channel;
    @NonNull
    private String totalMessage;

    public MessagedString(@NotNull User author, @NonNull MessageChannel channel, @NonNull final String totalMessage) {
        setAuthor(author);
        setChannel(channel);
        setTotalMessage(totalMessage);
    }

    @NonNull
    public String[] getTotalArgs() {
        return getTotalMessage().split(" ");
    }

    @NonNull
    public String getTotalBase() {
        return getTotalArgs()[0];
    }

    @NonNull
    public String getPrefix() {
        return StreamlineDiscord.getConfig().getBotLayout().getPrefix();
    }

    public boolean hasPrefix() {
        return getTotalMessage().startsWith(getPrefix());
    }

    @NonNull
    public String getBase() {
        if (! hasPrefix()) return getTotalBase();
        return getTotalBase().substring(getPrefix().length());
    }

    @NonNull
    public String[] getCommandArgs() {
        if (getTotalArgs().length <= 1) return List.of("").toArray(new String[0]);
        return ModuleUtils.argsMinus(getTotalArgs(), 0);
    }

    public boolean hasCommandArgs() {
        return ! getCommandArgs()[0].equals("") || getCommandArgs().length > 0;
    }

    public String getCommandArgsStringed() {
        return ModuleUtils.argsToString(getCommandArgs());
    }

    public boolean hasSlashPrefix() {
        return getTotalMessage().startsWith("/");
    }
}
