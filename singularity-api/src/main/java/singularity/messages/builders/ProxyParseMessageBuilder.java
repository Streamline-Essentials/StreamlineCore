package singularity.messages.builders;

import lombok.Getter;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.messages.answered.ReturnableMessage;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

public class ProxyParseMessageBuilder {
    @Getter
    private static final String subChannel = "api-parse";

    public static ReturnableMessage build(CosmicPlayer carrier, String toParse, CosmicSender user) {
        ProxiedMessage r = new ProxiedMessage(carrier, Singularity.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("parse", toParse);

        return new ReturnableMessage(r, false);
    }

    public static String parse(ProxiedMessage answeredMessage) {
        return answeredMessage.getString("parsed");
    }

    public static void handle(ProxiedMessage in) {
        if (! Singularity.isProxy()) {
            MessageUtils.logDebug("Tried to handle a ProxiedMessage with sub-channel '" + in.getSubChannel() + "', but this is not a proxy.");
            return;
        }

        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ServerConnectMessageBuilder.class.getSimpleName() + "'.");
            return;
        }
        if (! in.isReturnableLike()) {
            MessageUtils.logWarning("Tried to reply to a ProxiedMessage with sub-channel '" + in.getSubChannel() + "', but it was not ReturnableLike.");
            return;
        }

        String uuid = in.getString("user_uuid");
        String parse = in.getString("parse");
        String key = in.getString(ReturnableMessage.getKey());

//        MessageUtils.logInfo("ProxiedMessage in > uuid = '" + uuid + "', parse = '" + parse + "', key = '" + key + "'.");
        CosmicSender sender = UserUtils.getOrCreateSender(uuid);

        String parsed = ModuleUtils.replacePlaceholders(sender, parse);

        ProxiedMessage r = new ProxiedMessage(in.getCarrier(), Singularity.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", uuid);
        r.write("parse", parse);
        r.write("parsed", parsed);
        r.write(ReturnableMessage.getKey(), key);

        r.send();
    }
}
