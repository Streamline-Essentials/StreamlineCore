package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;

public class ProxyParseMessageBuilder {
    @Getter
    private static final String subChannel = "api-parse";

    public static ReturnableMessage build(StreamlinePlayer carrier, String toParse, StreamlineUser user) {
        ProxiedMessage r = new ProxiedMessage(carrier, false);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("parse", toParse);

        return new ReturnableMessage(r);
    }

    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ServerConnectMessageBuilder.class.getSimpleName() + "'.");
            return;
        }
        if (! in.isReturnableLike()) {
            MessageUtils.logWarning("Tried to reply to a ProxiedMessage with sub-channel '" + in.getSubChannel() + "', but it was not ReturnableLike.");
            return;
        }

        StreamlineUser user = ModuleUtils.getOrGetUser(in.getString("user_uuid"));
        String parsedAs = ModuleUtils.replaceAllPlayerBungee(in.getString("user_uuid"), in.getString("parse"));

        ProxiedMessage r = new ProxiedMessage(in.getCarrier(), true);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("parse", in.getString("parse"));
        r.write("parsed", parsedAs);
        r.write(ReturnableMessage.getKey(), in.getString(ReturnableMessage.getKey()));

        SLAPI.getInstance().getProxyMessenger().sendMessage(r);
    }
}
