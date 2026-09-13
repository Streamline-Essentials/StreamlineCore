package host.plas.discord.data.channeling;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;

@Setter
@Getter
public class RoutedUser {
    private CosmicSender user;
    private long discordId;

    public RoutedUser(CosmicSender user) {
        setUser(user);
        setDiscordId(0L);
    }

    public RoutedUser(long discordId) {
        setUser(null);
        setDiscordId(discordId);
    }

    public boolean isMinecraft() {
        return getUser() != null;
    }

    public boolean isDiscord() {
        return getDiscordId() != 0L;
    }
}
