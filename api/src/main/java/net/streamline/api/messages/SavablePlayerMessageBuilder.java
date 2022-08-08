package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SavablePlayerMessageBuilder {
    @Getter
    private static final String subChannel = "server-info";

    @Getter
    private static final List<String> lines = List.of(
            "latest_name=%this_latest_name%;",
            "displayname=%this_displayname%;",
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
        List<String> l = new ArrayList<>(getLines());
        l.set(0, l.get(0).replace("%this_latest_name%", player.getLatestName()));
        l.set(1, l.get(1).replace("%this_display_name%", player.getDisplayName()));
        l.set(2, l.get(2).replace("%this_list_tags%", getStringsAsString(player.getTagList())));
        l.set(3, l.get(3).replace("%this_points%", String.valueOf(player.getPoints())));
        l.set(4, l.get(4).replace("%this_latest_message%", player.getLastMessage()));
        l.set(5, l.get(5).replace("%this_online%", String.valueOf(player.isOnline())));
        l.set(6, l.get(6).replace("%this_latest_server%", player.getLatestServer()));
        l.set(7, l.get(7).replace("%this_bypass%", String.valueOf(player.isBypassPermissions())));
        l.set(8, l.get(8).replace("%this_xp_total%", String.valueOf(player.getTotalXP())));
        l.set(9, l.get(9).replace("%this_xp_current%", String.valueOf(player.getCurrentXP())));
        l.set(10, l.get(10).replace("%this_level%", String.valueOf(player.getLevel())));
        l.set(11, l.get(11).replace("%this_play_seconds%", String.valueOf(player.getPlaySeconds())));
        l.set(12, l.get(12).replace("%this_latest_ip%", player.getLatestIP()));
        l.set(13, l.get(13).replace("%this_list_ips%", getStringsAsString(player.getIpList())));
        l.set(14, l.get(14).replace("%this_list_names%", getStringsAsString(player.getNameList())));
        l.set(15, l.get(15).replace("%this_user_uuid%", player.getUUID()));
        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), l);
    }

    public static ProxiedStreamlinePlayer unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());

        ProxiedStreamlinePlayer player = new ProxiedStreamlinePlayer();

        player.setLatestName(input.readUTF());

        return player;
    }

    public static List<StreamlineUser> extrapolateUsers(String from) {
        String[] uuids = from.split(",");

        List<StreamlineUser> r = new ArrayList<>();

        Arrays.stream(uuids).forEach(a -> {
            StreamlineUser user = SLAPI.getInstance().getUserManager().getOrGetUser(a);
            if (user == null) return;
            r.add(user);
        });

        return r;
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

    public static SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) return new SingleSet<>("", "");

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }
}
