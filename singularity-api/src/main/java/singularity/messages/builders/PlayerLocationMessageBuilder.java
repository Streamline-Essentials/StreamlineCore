package singularity.messages.builders;

import lombok.Getter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;

/**
 * Builds and handles {@link ProxiedMessage} payloads that synchronise a player's
 * {@link CosmicLocation} across the proxy and backend servers.
 *
 * <p>The sub-channel identifier is {@value #subChannel}. Call {@link #build} on the
 * originating side and {@link #handle} on the receiving side to keep player location
 * data consistent across the network.</p>
 */
public class PlayerLocationMessageBuilder {

    /**
     * The plugin-messaging sub-channel name used to route location update messages.
     */
    @Getter
    private static final String subChannel = "player-location";

    /**
     * Constructs a {@link ProxiedMessage} that encodes the given location for the
     * specified user, using the supplied carrier player as the transport vehicle.
     *
     * @param carrier  the online {@link CosmicPlayer} used to send the plugin message
     * @param location the {@link CosmicLocation} to encode
     * @param user     the {@link CosmicPlayer} whose location is being reported
     * @return a fully populated {@link ProxiedMessage} ready to be sent
     */
    public static ProxiedMessage build(CosmicPlayer carrier, CosmicLocation location, CosmicPlayer user) {
        ProxiedMessage r = new ProxiedMessage(carrier, false);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("server", location.getServerName());
        r.write("world", location.getWorldName());
        r.write("x", String.valueOf(location.getX()));
        r.write("y", String.valueOf(location.getY()));
        r.write("z", String.valueOf(location.getZ()));
        r.write("yaw", String.valueOf(location.getYaw()));
        r.write("pitch", String.valueOf(location.getPitch()));

        return r;
    }

    /**
     * Processes an incoming location update message and applies the encoded
     * {@link CosmicLocation} to the referenced player.
     *
     * <p>On proxy-side environments the player's current server object is preferred
     * as the location server; on backend environments the server name encoded in the
     * message is used instead. The method logs a warning and returns early if the
     * sub-channel does not match, the player cannot be resolved, or the location
     * data is malformed.</p>
     *
     * @param in the incoming {@link ProxiedMessage} to process
     */
    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + PlayerLocationMessageBuilder.class.getSimpleName() + "'.");
            return;
        }

        String uuid = in.getString("user_uuid");
        String server = in.getString("server");
        String world = in.getString("world");
        double x = Double.parseDouble(in.getString("x"));
        double y = Double.parseDouble(in.getString("y"));
        double z = Double.parseDouble(in.getString("z"));
        float yaw = Float.parseFloat(in.getString("yaw"));
        float pitch = Float.parseFloat(in.getString("pitch"));

        CosmicPlayer player = ModuleUtils.getOrCreatePlayer(uuid).orElse(null);
        if (player == null) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for invalid player '" + uuid + "'.");
            return;
        }

        CosmicServer cosmicServer = new CosmicServer(server);
        CosmicLocation location;
        try {
            PlayerWorld playerWorld = new PlayerWorld(world);
            WorldPosition position = new WorldPosition(x, y, z);
            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

            if (Singularity.isProxy()) {
                location = new CosmicLocation(player.getServer(), playerWorld, position, rotation);
            } else {
                location = new CosmicLocation(cosmicServer, playerWorld, position, rotation);
            }
        } catch (Exception e) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for invalid location '" + server + ", " + world + ", " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + "' for player '" + uuid + "'.");
            return;
        }

        player.setLocation(location);
    }
}
