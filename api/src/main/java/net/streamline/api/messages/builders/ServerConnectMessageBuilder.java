package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.utils.MessageUtils;

import java.util.List;
import java.util.Optional;

public class ServerConnectMessageBuilder {
    @Getter
    private static final String subChannel = "server-connect";

    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_identifier%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxiedMessage build(StreamPlayer carrier, StreamlineServerInfo serverInfo, String uuid) {
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

        Optional<StreamPlayer> playerOptional = ModuleUtils.getOrGetPlayer(uuid);
        if (playerOptional.isEmpty()) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for unknown player '" + uuid + "'.");
            return;
        }
        StreamPlayer player = playerOptional.get();

        SLAPI.getInstance().getUserManager().connect(player, messageIn.getString("identifier"));
    }
}
