package host.plas.events.streamline.channels;

import host.plas.discord.data.channeling.EndPoint;
import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineDiscord;
import singularity.events.modules.ModuleEvent;

@Setter
@Getter
public class ChanneledMessageEvent extends ModuleEvent {
    private String message;
    private EndPoint input;
    private EndPoint desiredOutput;

    public ChanneledMessageEvent(String message, EndPoint input, EndPoint desiredOutput) {
        super(StreamlineDiscord.getInstance());
        setMessage(message);
        setInput(input);
        setDesiredOutput(desiredOutput);
    }
}
