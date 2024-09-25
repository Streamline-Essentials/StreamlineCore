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

public class TeleportMessageBuilder {
    @Getter
    private static final String subChannel = "player-location";

    public static ProxiedMessage build(CosmicPlayer carrier, CosmicLocation location, CosmicPlayer user) {
        ProxiedMessage r = new ProxiedMessage(carrier, false);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("location", location.toString());

        return r;
    }

    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + TeleportMessageBuilder.class.getSimpleName() + "'.");
            return;
        }
        if (Singularity.isProxy()) {
            MessageUtils.logWarning("ProxyMessageIn for '" + TeleportMessageBuilder.class.getSimpleName() + "' received on proxy server.");
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

        CosmicPlayer player = ModuleUtils.getOrCreatePlayer(uuid);

        CosmicServer cosmicServer = new CosmicServer(server);
        CosmicLocation location;
        try {
            PlayerWorld playerWorld = new PlayerWorld(world);
            WorldPosition position = new WorldPosition(x, y, z);
            PlayerRotation rotation = new PlayerRotation(yaw, pitch);
            location = new CosmicLocation(cosmicServer, playerWorld, position, rotation);
        } catch (Exception e) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for invalid location '" + server + ", " + world + ", " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + "' for player '" + uuid + "'.");
            return;
        }

        MessageUtils.logDebug("Teleporting player '" + player.getUuid() + "' to '" + location.asString() + "'.");

        Singularity.getInstance().getUserManager().teleport(player, location);
    }
}
