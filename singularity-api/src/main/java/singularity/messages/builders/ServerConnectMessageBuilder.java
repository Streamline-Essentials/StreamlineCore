package singularity.messages.builders;

import lombok.Getter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicServerInfo;
import singularity.utils.MessageUtils;

import java.util.List;

/**
 * Builds and handles {@link ProxiedMessage} payloads that instruct the proxy to
 * connect a player to a different backend server.
 *
 * <p>A backend sends the message via {@link #build} encoding the target server
 * identifier and the player's UUID. The proxy processes it through {@link #handle},
 * looks up the player, and delegates to the platform user manager to perform the
 * actual connection.</p>
 *
 * <p>The sub-channel identifier is {@value #subChannel}.</p>
 */
public class ServerConnectMessageBuilder {

    /**
     * The plugin-messaging sub-channel name used to route server-connect messages.
     */
    @Getter
    private static final String subChannel = "server-connect";

    /**
     * Template lines that describe the expected payload structure.
     * Each entry is a {@code key=placeholder;} pair.
     */
    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_identifier%;",
            "user_uuid=%this_user_uuid%;"
    );

    /**
     * Constructs a {@link ProxiedMessage} that requests the proxy to transfer the
     * player identified by {@code uuid} to the server described by {@code serverInfo}.
     *
     * @param carrier    the online {@link CosmicPlayer} used to deliver the plugin message
     * @param serverInfo the target {@link CosmicServerInfo} containing the server identifier
     * @param uuid       the UUID of the player to connect
     * @return a fully populated {@link ProxiedMessage} ready to be sent
     */
    public static ProxiedMessage build(CosmicPlayer carrier, CosmicServerInfo serverInfo, String uuid) {
        ProxiedMessage r = new ProxiedMessage(carrier, false);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", uuid);
        r.write("identifier", serverInfo.getIdentifier());

        return r;
    }

    /**
     * Processes an incoming server-connect request on the proxy and initiates the
     * player transfer.
     *
     * <p>Returns early with a warning if the sub-channel does not match, if the
     * message was proxy-originated (only backend-originated requests are accepted),
     * or if the referenced player cannot be resolved.</p>
     *
     * @param messageIn the incoming {@link ProxiedMessage} to process
     */
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

        CosmicPlayer player = ModuleUtils.getOrCreatePlayer(uuid).orElse(null);
        if (player == null) {
            MessageUtils.logWarning("Failed to find player with UUID '" + uuid + "' for ServerConnectMessageBuilder.");
            return;
        }

        Singularity.getInstance().getUserManager().connect(player, messageIn.getString("identifier"));
    }
}
