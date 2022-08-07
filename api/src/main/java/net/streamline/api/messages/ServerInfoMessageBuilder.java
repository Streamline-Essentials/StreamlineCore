package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerInfoMessageBuilder {
    @Getter
    private static final String subChannel = "server-info";

    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_identifier%;",
            "name=%this_name%;",
            "motd=%this_motd%;",
            "address=%this_address%;",
            "user_uuids=%this_user_uuids%;"
    );

    public static ProxyMessageOut build(StreamlineServerInfo serverInfo) {
        List<String> l = new ArrayList<>(getLines());
        l.set(0, l.get(0).replace("%this_identifier%", serverInfo.getIdentifier()));
        l.set(1, l.get(1).replace("%this_name%", serverInfo.getName()));
        l.set(2, l.get(2).replace("%this_motd%", serverInfo.getMotd()));
        l.set(3, l.get(3).replace("%this_address%", serverInfo.getAddress()));
        l.set(4, l.get(4).replace("%this_user_uuids%", getUserUUIDs(serverInfo.getOnlineUsers().values().stream().toList())));
        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), l);
    }

    public static StreamlineServerInfo unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());

        List<String> l = new ArrayList<>();
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());

        String identifier = extrapolate(l.get(0)).value;
        String name = extrapolate(l.get(1)).value;
        String motd = extrapolate(l.get(2)).value;
        String address = extrapolate(l.get(3)).value;
        String usersString = extrapolate(l.get(4)).value;
        ConcurrentSkipListMap<String, StreamlineUser> usersMap = new ConcurrentSkipListMap<>();
        extrapolateUsers(usersString).forEach(a -> {
            usersMap.put(a.getUUID(), a);
        });

        return new StreamlineServerInfo(identifier, name, motd, address, usersMap);
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

    public static String getUserUUIDs(List<StreamlineUser> users) {
        StringBuilder builder = new StringBuilder();

        AtomicInteger integer = new AtomicInteger(0);
        users.forEach(a -> {
            if (integer.get() < users.size()) {
                builder.append(a.getUUID()).append(",");
            } else {
                builder.append(a.getUUID());
            }
            integer.incrementAndGet();
        });

        return builder.toString();
    }

    public static SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) return new SingleSet<>("", "");

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }
}
