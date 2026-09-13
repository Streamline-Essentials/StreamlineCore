package host.plas.events.streamline.bot.posting;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import host.plas.StreamlineDiscord;
import singularity.events.modules.ModuleEvent;

@Setter
@Getter
public class DiscordVoiceStateUpdate extends ModuleEvent {
    VoiceDispatchInterceptor.VoiceStateUpdate update;

    public DiscordVoiceStateUpdate(VoiceDispatchInterceptor.VoiceStateUpdate update) {
        super(StreamlineDiscord.getInstance());

        setUpdate(update);
    }
}
