package host.plas.events.streamline.verification;

import host.plas.discord.MessagedString;
import host.plas.utils.DiscordUtils;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;

@Getter @Setter
public class WithVerificationEvent extends VerificationEvent {
    @Nullable
    private String uuid;
    @Nullable
    private Long discordId;

    @Nullable
    private MessagedString message;
    @Nullable
    private String verification;

    public WithVerificationEvent(boolean isFromCommand, @Nullable String uuid, @Nullable Long discordId, @Nullable MessagedString message, @Nullable String verification) {
        super(isFromCommand);
        this.uuid = uuid;
        this.discordId = discordId;
        this.message = message;
        this.verification = verification;
    }

    public Optional<User> getAuthor() {
        if (message == null) return Optional.empty();

        return Optional.of(message.getAuthor());
    }

    public Optional<CosmicSender> getSender() {
        if (uuid == null) return Optional.empty();
        return UserUtils.getOrGetSender(uuid);
    }

    public Optional<CosmicPlayer> getPlayer() {
        if (uuid == null) return Optional.empty();
        return UserUtils.getOrCreatePlayer(uuid);
    }

    public Optional<User> getDiscordUser() {
        return DiscordUtils.getUserById(discordId);
    }

    public boolean hasMinecraftUuid() {
        return uuid != null;
    }

    public boolean hasDiscordId() {
        return discordId != null;
    }
}
