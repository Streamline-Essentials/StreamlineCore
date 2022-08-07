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

public class ServerConnectMessageBuilder {
    @Getter
    private static final String subChannel = "server-connect";

    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_identifier%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxyMessageOut build(StreamlineServerInfo serverInfo, StreamlineUser user) {
        List<String> l = new ArrayList<>(getLines());
        l.set(0, l.get(0).replace("%this_identifier%", serverInfo.getIdentifier()));
        l.set(1, l.get(1).replace("%this_user_uuid%", user.getUUID()));
        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), l);
    }

    public static SingleSet<String, String> unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());

        List<String> l = new ArrayList<>();
        l.add(input.readUTF());

        String identifier = extrapolate(l.get(0)).value;
        String uuid = extrapolate(l.get(1)).value;

        return new SingleSet<>(identifier, uuid);
    }

    public static SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) return new SingleSet<>("", "");

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }
}
