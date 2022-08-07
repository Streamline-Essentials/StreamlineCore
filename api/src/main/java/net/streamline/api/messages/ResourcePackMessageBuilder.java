package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourcePackMessageBuilder {
    @Getter
    private static final String subChannel = "resource-pack";

    @Getter
    private static final List<String> lines = List.of(
            "identifier=%this_url%;",
            "identifier=%this_prompt%;",
            "identifier=%this_hash%;",
            "identifier=%this_force%;"
    );

    public static ProxyMessageOut build(StreamlineResourcePack resourcePack) {
        List<String> l = new ArrayList<>(getLines());
        l.set(0, l.get(0).replace("%this_url%", resourcePack.getUrl()));
        l.set(1, l.get(1).replace("%this_identifier%", resourcePack.getPrompt()));
        l.set(2, l.get(2).replace("%this_hash%",  getBytes(resourcePack.getHash())));
        l.set(3, l.get(3).replace("%this_force%", String.valueOf(resourcePack.isForce())));
        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), l);
    }

    public static StreamlineResourcePack unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());

        List<String> l = new ArrayList<>();
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());

        String url = extrapolate(l.get(0)).value;
        String prompt = extrapolate(l.get(1)).value;
        byte[] hash = extrapolateBytes(extrapolate(l.get(2)).value);
        boolean force = Boolean.parseBoolean(extrapolate(l.get(3)).value);

        return new StreamlineResourcePack(url, hash, prompt, force);
    }

    public static byte[] extrapolateBytes(String from) {
        String[] fromSplit = from.split(",");

        byte[] bytes = new byte[fromSplit.length];

        for (int i = 0; i < fromSplit.length; i ++) {
            bytes[i] = Byte.parseByte(fromSplit[i]);
        }

        return bytes;
    }

    public static String getBytes(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        AtomicInteger integer = new AtomicInteger(0);
        for (byte b : bytes) {
            if (integer.get() < bytes.length) {
                builder.append(b).append(",");
            } else {
                builder.append(b);
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
