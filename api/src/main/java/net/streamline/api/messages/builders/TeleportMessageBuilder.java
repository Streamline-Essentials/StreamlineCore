package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.data.players.location.PlayerRotation;
import net.streamline.api.data.players.location.PlayerWorld;
import net.streamline.api.data.players.location.WorldPosition;
import net.streamline.api.data.server.StreamServer;
import net.streamline.api.interfaces.IBackendHandler;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.utils.MessageUtils;

import java.util.Optional;

public class TeleportMessageBuilder {
    @Getter
    private static final String subChannel = "player-location";

    public static ProxiedMessage build(StreamPlayer carrier, PlayerLocation location, StreamPlayer user) {
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
        if (SLAPI.isProxy()) {
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

        Optional<StreamPlayer> playerOptional = ModuleUtils.getOrGetPlayer(uuid);
        if (playerOptional.isEmpty()) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for unknown player '" + uuid + "'.");
            return;
        }
        StreamPlayer player = playerOptional.get();

        StreamServer streamServer = new StreamServer(server);
        PlayerLocation location;
        try {
            PlayerWorld playerWorld = new PlayerWorld(world);
            WorldPosition position = new WorldPosition(x, y, z);
            PlayerRotation rotation = new PlayerRotation(yaw, pitch);
            location = new PlayerLocation(player, playerWorld, position, rotation);
        } catch (Exception e) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for invalid location '" + server + ", " + world + ", " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + "' for player '" + uuid + "'.");
            return;
        }

        IBackendHandler backendHandler = SLAPI.getBackendHandler();
        if (backendHandler == null) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for player '" + uuid + "' but no backend handler is set.");
            return;
        }

        MessageUtils.logDebug("Teleporting player '" + player.getUuid() + "' to '" + location.asString() + "'.");

        backendHandler.teleport(player, location);
    }
}
