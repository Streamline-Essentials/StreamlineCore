package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.data.players.location.PlayerRotation;
import net.streamline.api.data.players.location.PlayerWorld;
import net.streamline.api.data.players.location.WorldPosition;
import net.streamline.api.data.server.StreamServer;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.MessageUtils;

public class PlayerLocationMessageBuilder {
    @Getter
    private static final String subChannel = "player-location";

    public static ProxiedMessage build(StreamPlayer carrier, PlayerLocation location, StreamPlayer user) {
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

    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + PlayerLocationMessageBuilder.class.getSimpleName() + "'.");
            return;
        }
        if (! SLAPI.isProxy()) {
            MessageUtils.logWarning("ProxyMessageIn for '" + PlayerLocationMessageBuilder.class.getSimpleName() + "' received on non-proxy server.");
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

        StreamPlayer player = ModuleUtils.getOrCreatePlayer(uuid);

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

        player.setServer(streamServer);
        player.setLocation(location);
    }
}
