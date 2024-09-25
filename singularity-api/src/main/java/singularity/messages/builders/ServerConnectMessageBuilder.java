package singularity.messages.builders;

import lombok.Getter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicServerInfo;
import singularity.utils.MessageUtils;

import java.util.List;

public class ServerConnectMessageBuilder {
    @Getter
    private static final String subChannel = "server-connect";

    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_identifier%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxiedMessage build(CosmicPlayer carrier, CosmicServerInfo serverInfo, String uuid) {
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

        CosmicPlayer player = ModuleUtils.getOrCreatePlayer(uuid);

        Singularity.getInstance().getUserManager().connect(player, messageIn.getString("identifier"));
    }
}
