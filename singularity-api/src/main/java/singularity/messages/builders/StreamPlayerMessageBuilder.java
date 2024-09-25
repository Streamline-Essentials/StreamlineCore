package singularity.messages.builders;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.utils.MessageUtils;

public class StreamPlayerMessageBuilder {
    @Getter
    private static final String subChannel = "savable-player";

    public static ProxiedMessage build(CosmicPlayer player, boolean isProxyOriginated) {
        ProxiedMessage r = new ProxiedMessage(player, isProxyOriginated);

        r.setSubChannel(getSubChannel());

        r.write("uuid", player.getUuid());
        r.write("firstJoin", String.valueOf(player.getFirstJoinMillis()));
        r.write("lastJoin", String.valueOf(player.getLastJoinMillis()));
        r.write("lastQuit", String.valueOf(player.getLastQuitMillis()));

        r.write("currentName", player.getCurrentName());
        r.write("currentIP", player.getCurrentIp());

        r.write("nickname", player.getMeta().getNickname());
        r.write("prefix", player.getMeta().getPrefix());
        r.write("suffix", player.getMeta().getSuffix());
        r.write("tags", player.getMeta().getTagsAsString());

        r.write("server", player.getLocation().getServerName());
        r.write("world", player.getLocation().getWorldName());
        r.write("x", String.valueOf(player.getLocation().getX()));
        r.write("y", String.valueOf(player.getLocation().getY()));
        r.write("z", String.valueOf(player.getLocation().getZ()));
        r.write("yaw", String.valueOf(player.getLocation().getYaw()));
        r.write("pitch", String.valueOf(player.getLocation().getPitch()));

        r.write("bypassingPermissions", String.valueOf(player.getPermissions().isBypassingPermissions()));

        return r;
    }

    public static CosmicPlayer unbuild(ProxiedMessage messageIn) {
        if (! messageIn.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + StreamPlayerMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        String uuid = messageIn.getString("uuid");
        long firstJoin = Long.parseLong(messageIn.getString("firstJoin"));
        long lastJoin = Long.parseLong(messageIn.getString("lastJoin"));
        long lastQuit = Long.parseLong(messageIn.getString("lastQuit"));

        String currentName = messageIn.getString("currentName");
        String currentIP = messageIn.getString("currentIP");

        String nickname = messageIn.getString("nickname");
        String prefix = messageIn.getString("prefix");
        String suffix = messageIn.getString("suffix");
        String tags = messageIn.getString("tags");

        String server = messageIn.getString("server");
        String world = messageIn.getString("world");
        double x = Double.parseDouble(messageIn.getString("x"));
        double y = Double.parseDouble(messageIn.getString("y"));
        double z = Double.parseDouble(messageIn.getString("z"));
        float yaw = Float.parseFloat(messageIn.getString("yaw"));
        float pitch = Float.parseFloat(messageIn.getString("pitch"));

        boolean bypassingPermissions = Boolean.parseBoolean(messageIn.getString("bypassingPermissions"));

        CosmicPlayer player = new CosmicPlayer(uuid);
        player.setFirstJoinMillis(firstJoin);
        player.setLastJoinMillis(lastJoin);
        player.setLastQuitMillis(lastQuit);

        player.setCurrentName(currentName);
        player.setCurrentIp(currentIP);

        player.getMeta().setNickname(nickname);
        player.getMeta().setPrefix(prefix);
        player.getMeta().setSuffix(suffix);
        player.getMeta().setTagsFromString(tags);

        player.getLocation().setServerName(server);
        player.getLocation().setWorldName(world);
        player.getLocation().setX(x);
        player.getLocation().setY(y);
        player.getLocation().setZ(z);
        player.getLocation().setYaw(yaw);
        player.getLocation().setPitch(pitch);

        player.getPermissions().setBypassingPermissions(bypassingPermissions);

        return player;
    }
}
