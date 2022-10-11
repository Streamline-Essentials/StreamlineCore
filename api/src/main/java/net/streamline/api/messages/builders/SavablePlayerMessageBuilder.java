package net.streamline.api.messages.builders;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.ProxiedStreamlinePlayer;
import net.streamline.api.messages.ProxyMessageHelper;
import net.streamline.api.messages.ProxyMessageIn;
import net.streamline.api.messages.ProxyMessageOut;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class SavablePlayerMessageBuilder {
    @Getter
    private static final String subChannel = "server-info";

    @Getter
    private static final List<String> lines = List.of(
            "latest_name=%this_latest_name%;",
            "displayname=%this_display_name%;",
            "list_tags=%this_list_tags%;",
            "points=%this_points%;",
            "latest_message=%this_latest_message%;",
            "online=%this_online%;",
            "latest_server=%this_latest_server%;",
            "bypass=%this_bypass%;",
            "xp_total=%this_xp_total%;",
            "xp_current=%this_xp_current%;",
            "level=%this_level%;",
            "play_seconds=%this_play_seconds%;",
            "latest_ip=null;",
            "list_ips=null;",
            "list_names=%this_list_names%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxyMessageOut build(StreamlinePlayer player) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(getSubChannel());
        output.writeUTF(lines.get(0).replace("%this_latest_name%", player.getLatestName()));
        output.writeUTF(lines.get(1).replace("%this_display_name%", player.getDisplayName()));
        output.writeUTF(lines.get(2).replace("%this_list_tags%", getStringsAsString(player.getTagList().stream().toList())));
        output.writeUTF(lines.get(3).replace("%this_points%", String.valueOf(player.getPoints())));
        output.writeUTF(lines.get(4).replace("%this_latest_message%", player.getLastMessage() == null ? "" : player.getLastMessage()));
        output.writeUTF(lines.get(5).replace("%this_online%", String.valueOf(player.isOnline())));
        output.writeUTF(lines.get(6).replace("%this_latest_server%", player.getLatestServer()));
        output.writeUTF(lines.get(7).replace("%this_bypass%", String.valueOf(player.isBypassPermissions())));
        output.writeUTF(lines.get(8).replace("%this_xp_total%", String.valueOf(player.getTotalXP())));
        output.writeUTF(lines.get(9).replace("%this_xp_current%", String.valueOf(player.getCurrentXP())));
        output.writeUTF(lines.get(10).replace("%this_level%", String.valueOf(player.getLevel())));
        output.writeUTF(lines.get(11).replace("%this_play_seconds%", String.valueOf(player.getPlaySeconds())));
        output.writeUTF(lines.get(12).replace("%this_latest_ip%", player.getLatestIP()));
        output.writeUTF(lines.get(13).replace("%this_list_ips%", getStringsAsString(player.getIpList().stream().toList())));
        output.writeUTF(lines.get(14).replace("%this_list_names%", getStringsAsString(player.getNameList().stream().toList())));
        output.writeUTF(lines.get(15).replace("%this_user_uuid%", player.getUuid()));

        ProxyMessageOut message = new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), output.toByteArray());
        message.setServer(player.getLatestServer());
        return message;
    }

    public static ProxiedStreamlinePlayer unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());
        if (! messageIn.getSubChannel().equals(input.readUTF())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + SavablePlayerMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        ProxiedStreamlinePlayer player = new ProxiedStreamlinePlayer();

        player.setLatestName(ProxyMessageHelper.extrapolate(input.readUTF()).value);
        player.setDisplayName(ProxyMessageHelper.extrapolate(input.readUTF()).value);
        player.setTagList(new ConcurrentSkipListSet<>(Arrays.stream(ProxyMessageHelper.extrapolate(input.readUTF()).value.split(",")).toList()));
        player.setPoints(Double.parseDouble(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setLastMessage(ProxyMessageHelper.extrapolate(input.readUTF()).value);
        player.setOnline(Boolean.parseBoolean(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setLatestServer(ProxyMessageHelper.extrapolate(input.readUTF()).value);
        player.setBypassPermissions(Boolean.parseBoolean(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setTotalXP(Double.parseDouble(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setCurrentXP(Double.parseDouble(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setLevel(Integer.parseInt(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setPlaySeconds(Integer.parseInt(ProxyMessageHelper.extrapolate(input.readUTF()).value));
        player.setLatestIP(ProxyMessageHelper.extrapolate(input.readUTF()).value);
        player.setIpList(new ConcurrentSkipListSet<>(Arrays.stream(ProxyMessageHelper.extrapolate(input.readUTF()).value.split(",")).toList()));
        player.setNameList(new ConcurrentSkipListSet<>(Arrays.stream(ProxyMessageHelper.extrapolate(input.readUTF()).value.split(",")).toList()));
        player.setUuid(ProxyMessageHelper.extrapolate(input.readUTF()).value);

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
