package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.utils.MessageUtils;

public class StreamPlayerMessageBuilder {
    @Getter
    private static final String subChannel = "savable-player";

    public static ProxiedMessage build(StreamPlayer player, boolean isProxyOriginated) {
        ProxiedMessage r = new ProxiedMessage(player, isProxyOriginated);

        r.setSubChannel(getSubChannel());

        r.write("uuid", player.getUuid());
        r.write("firstJoin", String.valueOf(player.getFirstJoinMillis()));
        r.write("lastJoin", String.valueOf(player.getLastJoinMillis()));
        r.write("lastQuit", String.valueOf(player.getLastQuitMillis()));

        r.write("currentName", player.getCurrentName());
        r.write("currentIP", player.getCurrentIP());

        r.write("nickname", player.getMeta().getNickname());
        r.write("prefix", player.getMeta().getPrefix());
        r.write("suffix", player.getMeta().getSuffix());
        r.write("tags", player.getMeta().getTagsAsString());

        r.write("level", String.valueOf(player.getLeveling().getLevel()));
        r.write("totalExperience", String.valueOf(player.getLeveling().getTotalExperience()));
        r.write("currentExperience", String.valueOf(player.getLeveling().getCurrentExperience()));
        r.write("equationString", player.getLeveling().getEquationString());
        r.write("startedLevel", String.valueOf(player.getLeveling().getStartedLevel()));
        r.write("startedExperience", String.valueOf(player.getLeveling().getStartedExperience()));

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

    public static StreamPlayer unbuild(ProxiedMessage messageIn) {
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

        int level = Integer.parseInt(messageIn.getString("level"));
        int totalExperience = Integer.parseInt(messageIn.getString("totalExperience"));
        int currentExperience = Integer.parseInt(messageIn.getString("currentExperience"));
        String equationString = messageIn.getString("equationString");
        int startedLevel = Integer.parseInt(messageIn.getString("startedLevel"));
        int startedExperience = Integer.parseInt(messageIn.getString("startedExperience"));

        String server = messageIn.getString("server");
        String world = messageIn.getString("world");
        double x = Double.parseDouble(messageIn.getString("x"));
        double y = Double.parseDouble(messageIn.getString("y"));
        double z = Double.parseDouble(messageIn.getString("z"));
        float yaw = Float.parseFloat(messageIn.getString("yaw"));
        float pitch = Float.parseFloat(messageIn.getString("pitch"));

        boolean bypassingPermissions = Boolean.parseBoolean(messageIn.getString("bypassingPermissions"));

        StreamPlayer player = new StreamPlayer(uuid);
        player.setFirstJoin(firstJoin);
        player.setLastJoin(lastJoin);
        player.setLastQuit(lastQuit);

        player.setCurrentName(currentName);
        player.setCurrentIP(currentIP);

        player.getMeta().setNickname(nickname);
        player.getMeta().setPrefix(prefix);
        player.getMeta().setSuffix(suffix);
        player.getMeta().setTagsFromString(tags);

        player.getLeveling().setLevel(level);
        player.getLeveling().setTotalExperience(totalExperience);
        player.getLeveling().setCurrentExperience(currentExperience);
        player.getLeveling().setEquationString(equationString);
        player.getLeveling().setStartedLevel(startedLevel);
        player.getLeveling().setStartedExperience(startedExperience);

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
