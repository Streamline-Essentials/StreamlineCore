package singularity.messages.builders;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.utils.MessageUtils;

import java.util.Date;

public class ServerNameMessageBuilder {
    @Getter @Setter
    private static String subChannel = "server-name-setter";

    @Getter @Setter
    private static Date lastUpdate = null;

    public static ProxiedMessage build(CosmicPlayer carrier, String serverName) {
        ProxiedMessage r = new ProxiedMessage(carrier, Singularity.isProxy()); // only run from proxy...

        r.setSubChannel(getSubChannel());
        r.write("serverName", serverName);

        return r;
    }

    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) return;
        if (! isNeedUpdate()) return;

        String serverName = in.getString("serverName");

        if (serverName == null) {
            MessageUtils.logWarning("Received an invalid server name update call: " + in.getLiteralAsString());
            return;
        }

        GivenConfigs.writeServerName(serverName);
        update();
    }

    public static boolean isNeedUpdate() {
        if (getLastUpdate() == null) return true;
        return (new Date().getTime() - getLastUpdate().getTime()) > getUpdateMillis();
    }

    public static void update() {
        setLastUpdate(new Date());
    }

    public static long getUpdateMillis() {
        return 30000 * 60; // 30 minutes
    }
}
