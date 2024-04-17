package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

public class ProxyParseMessageBuilder {
    @Getter
    private static final String subChannel = "api-parse";

    public static ReturnableMessage build(StreamPlayer carrier, String toParse, StreamSender user) {
        ProxiedMessage r = new ProxiedMessage(carrier, SLAPI.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("parse", toParse);

        return new ReturnableMessage(r, false);
    }

    public static String parse(ProxiedMessage answeredMessage) {
        return answeredMessage.getString("parsed");
    }

    public static void handle(ProxiedMessage in) {
        if (! SLAPI.isProxy()) {
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
        StreamSender sender = UserUtils.getOrCreateSender(uuid);

        String parsed = ModuleUtils.replacePlaceholders(sender, parse);

        ProxiedMessage r = new ProxiedMessage(in.getCarrier(), SLAPI.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", uuid);
        r.write("parse", parse);
        r.write("parsed", parsed);
        r.write(ReturnableMessage.getKey(), key);

        r.send();
    }
}
