package host.plas.discord.data;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Activity;
import host.plas.StreamlineDiscord;

@Setter
@Getter
public class BotLayout {
    private String token;
    private String prefix;
    private Activity.ActivityType activityType;
    private String activityValue;
    private String avatarUrl;
    private boolean slashCommandsEnabled;
    private long mainGuildId;

    public BotLayout(String token, String commandPrefix, Activity.ActivityType activityType, String activityValue, String avatarUrl, boolean slashCommandsEnabled, long mainGuildId) {
        setToken(token);
        setPrefix(commandPrefix);
        setActivityType(activityType);
        setActivityValue(activityValue);
        setAvatarUrl(avatarUrl);
        setSlashCommandsEnabled(slashCommandsEnabled);
        setMainGuildId(mainGuildId);
    }

    public void save() {
        StreamlineDiscord.getConfig().saveBotLayout(this);
    }
}
