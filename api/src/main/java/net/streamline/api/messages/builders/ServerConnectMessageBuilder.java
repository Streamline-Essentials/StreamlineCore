package net.streamline.api.messages.builders;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.List;

public class ServerConnectMessageBuilder {
    @Getter
    private static final String subChannel = "server-connect";

    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_identifier%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxiedMessage build(StreamlinePlayer carrier, StreamlineServerInfo serverInfo, String uuid) {
        ProxiedMessage r = new ProxiedMessage(carrier, false);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", uuid);
        r.write("identifier", serverInfo.getIdentifier());

        return r;
    }

    public static void handle(ProxiedMessage messageIn) {
        if (! messageIn.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ServerConnectMessageBuilder.class.getSimpleName() + "'.");
            return;
        }

        if (messageIn.isProxyOriginated()) {
            MessageUtils.logWarning("Tried to handle a ProxiedMessage with sub-channel '" + messageIn.getSubChannel() + "', but it was ProxyOriginated...");
            return;
        }

        String uuid = messageIn.getString("user_uuid");

        StreamlineUser user = ModuleUtils.getOrGetUser(uuid);
        if (user == null) {
            MessageUtils.logWarning("Tried to handle a ProxiedMessage with sub-channel '" + messageIn.getSubChannel() + "', but it was could not find a user for '" + uuid + "'...");
            return;
        }

        SLAPI.getInstance().getUserManager().connect(user, messageIn.getString("identifier"));
    }
}
