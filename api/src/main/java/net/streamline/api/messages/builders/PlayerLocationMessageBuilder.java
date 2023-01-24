package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlineLocation;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;

public class PlayerLocationMessageBuilder {
    @Getter
    private static final String subChannel = "player-location";

    public static ProxiedMessage build(StreamlinePlayer carrier, StreamlineLocation location, StreamlineUser user) {
        ProxiedMessage r = new ProxiedMessage(carrier, false);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("location", location.toString());

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
        String location = in.getString("location");

        StreamlinePlayer player = ModuleUtils.getOrGetPlayer(uuid);
        if (player == null) {
            MessageUtils.logWarning("PlayerLocationMessageBuilder received for unknown player '" + uuid + "'.");
            return;
        }
        StreamlineLocation streamlineLocation = new StreamlineLocation(location);
        if (streamlineLocation.isNull()) MessageUtils.logWarning("PlayerLocationMessageBuilder received for null location '" + location + "' for player '" + uuid + "'. Continuing anyway...");

        player.setLocation(streamlineLocation);
    }
}
