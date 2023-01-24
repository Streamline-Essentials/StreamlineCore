package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.messages.ProxiedStreamlinePlayer;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.utils.MessageUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SavablePlayerMessageBuilder {
    @Getter
    private static final String subChannel = "savable-player";

    public static ProxiedMessage build(StreamlinePlayer player, boolean isProxyOriginated) {
        ProxiedMessage r = new ProxiedMessage(player, isProxyOriginated);

        r.setSubChannel(getSubChannel());
        r.write("latest_name", player.getLatestName());
        r.write("display_name", player.getDisplayName());
        r.write("tag_list", player.getTagList());
        r.write("points", String.valueOf(player.getPoints()));
        r.write("latest_message", player.getLastMessage());
        r.write("online", String.valueOf(player.isOnline()));
        r.write("latest_server", player.getLatestServer());
        r.write("bypass", String.valueOf(player.isBypassPermissions()));
        r.write("xp_total", String.valueOf(player.getTotalXP()));
        r.write("xp_current", String.valueOf(player.getCurrentXP()));
        r.write("level", String.valueOf(player.getLevel()));
        r.write("play_seconds", String.valueOf(player.getPlaySeconds()));
        r.write("latest_ip", "null");
        r.write("ip_list", "null");
        r.write("name_list", player.getNameList());
        r.write("user_uuid", player.getUuid());

        r.setServer(player.getLatestServer());

        return r;
    }

    public static ProxiedStreamlinePlayer unbuild(ProxiedMessage messageIn) {
        if (! messageIn.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + SavablePlayerMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        ProxiedStreamlinePlayer player = new ProxiedStreamlinePlayer();

        player.setLatestName(messageIn.getString("latest_name"));
        player.setDisplayName(messageIn.getString("display_name"));
        player.setTagList(messageIn.getConcurrentStringList("list_tags"));
        player.setPoints(messageIn.getDouble("points"));
        player.setLastMessage(messageIn.getString(""));
        player.setOnline(messageIn.getBoolean("online"));
        player.setLatestServer(messageIn.getString("latest_server"));
        player.setBypassPermissions(messageIn.getBoolean("bypass"));
        player.setTotalXP(messageIn.getDouble("xp_total"));
        player.setCurrentXP(messageIn.getDouble("xp_current"));
        player.setLevel(messageIn.getInteger("level"));
        player.setPlaySeconds(messageIn.getInteger("play_seconds"));
        player.setLatestIP(messageIn.getString("latest_ip"));
        player.setIpList(messageIn.getConcurrentStringList("ip_list"));
        player.setNameList(messageIn.getConcurrentStringList("name_list"));
        player.setUuid(messageIn.getString("user_uuid"));

        return player;
    }

    public static String getStringsAsString(List<String> strings) {
        StringBuilder builder = new StringBuilder();

        AtomicInteger integer = new AtomicInteger(0);
        for (String s : strings) {
            if (integer.get() < strings.size()) {
                builder.append(s).append(",");
            } else {
                builder.append(s);
            }
            integer.incrementAndGet();
        }

        return builder.toString();
    }
}
