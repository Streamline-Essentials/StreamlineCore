package host.plas.discord.messaging;

import lombok.Getter;
import net.streamline.api.SLAPI;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordHandler;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;

public class DiscordProxiedMessage extends ProxiedMessage {
    @Getter
    private static final String selfSubChannel = "discord-proxy-message";
    @Getter
    private static final String messageKey = "{{message}}";
    @Getter
    private static final String inputTypeKey = "{{input_type}}";
    @Getter
    private static final String inputIdentifierKey = "{{input_identifier}}";

    public static DiscordProxiedMessage translate(ProxiedMessage from) {
        if (! from.getSubChannel().equals(getSelfSubChannel())) return null;
        if (! from.hasKey(getMessageKey())) {
            StreamlineDiscord.getInstance().logWarning("Received a ProxiedMessage that emulates a DiscordProxiedMessage, but has no message attached! Voiding...");
            return null;
        }
        if (! from.hasKey(getInputTypeKey())) {
            StreamlineDiscord.getInstance().logWarning("Received a ProxiedMessage that emulates a DiscordProxiedMessage, but has no input type attached! Voiding...");
            return null;
        }
        if (! from.hasKey(getInputIdentifierKey())) {
            StreamlineDiscord.getInstance().logWarning("Received a ProxiedMessage that emulates a DiscordProxiedMessage, but has no input identifier attached! Voiding...");
            return null;
        }

        return new DiscordProxiedMessage(
                from.getCarrier(), from.getString(getMessageKey()),
                from.getString(getInputTypeKey()), from.getString(getInputIdentifierKey())
        );
    }

    public DiscordProxiedMessage(CosmicPlayer carrier, String message, String inputType, String inputIdentifier, boolean send) {
        super(carrier, ! DiscordHandler.isBackEnd(), SLAPI.getApiChannel());

        setSubChannel(getSelfSubChannel());
        write(getMessageKey(), message);
        write(getInputTypeKey(), inputType);
        write(getInputIdentifierKey(), inputIdentifier);

        if (send) send();
    }

    public DiscordProxiedMessage(CosmicPlayer carrier, String message, String inputType, String inputIdentifier) {
        this(carrier, message, inputType, inputIdentifier, false);
    }

    public String getMessage() {
        return getString(getMessageKey());
    }

    public String getInputType() {
        return getString(getInputTypeKey());
    }

    public String getInputIdentifer() {
        return getString(getInputIdentifierKey());
    }

    @Override
    public void send() {
        if (! GivenConfigs.getMainConfig().debugConsoleDebugDisabled()) StreamlineDiscord.getInstance().logDebug("Sending DiscordProxiedMessage to " + getCarrier().getCurrentName() + "...");
        super.send();
    }
}
