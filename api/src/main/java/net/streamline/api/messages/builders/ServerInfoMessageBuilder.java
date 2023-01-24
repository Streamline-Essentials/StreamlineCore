package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.utils.MessageUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class ServerInfoMessageBuilder {
    @Getter
    private static final String subChannel = "server-info";

    public static ProxiedMessage build(StreamlinePlayer carrier, StreamlineServerInfo serverInfo) {
        ProxiedMessage r = new ProxiedMessage(carrier, true);

        r.setSubChannel(getSubChannel());
        r.write("identifier", serverInfo.getIdentifier());
        r.write("name", serverInfo.getName());
        r.write("motd", serverInfo.getMotd());
        r.write("address", serverInfo.getAddress());
        r.write("user_uuids", serverInfo.getOnlineUsers());

        return r;
    }

    public static void handle(ProxiedMessage messageIn) {
        if (! messageIn.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ServerConnectMessageBuilder.class.getSimpleName() + "'.");
            return;
        }

        if (! messageIn.isProxyOriginated()) {
            MessageUtils.logWarning("Tried to handle a ProxiedMessage with sub-channel '" + messageIn.getSubChannel() + "', but it was not ProxyOriginated...");
            return;
        }

        String identifier = messageIn.getString("identifier");
        String name = messageIn.getString("name");
        String motd = messageIn.getString("motd");
        String address = messageIn.getString("address");
        ConcurrentSkipListSet<String> userUuids = messageIn.getConcurrentStringList("user_uuids");

        StreamlineServerInfo serverInfo = new StreamlineServerInfo(identifier, name, motd, address, userUuids);

        SLAPI.getInstance().getProfiler();
    }
}
