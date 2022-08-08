package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.utils.MatcherUtils;

import java.util.List;

public class ProxyMessageHelper {
    public static SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) return new SingleSet<>("", "");

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }

    public static byte[] removeSubChannel(byte[] data) {
        byte[] newData = new byte[data.length - 1];
        for (int i = 1; i < data.length; i ++) {
            newData[i - 1] = data[i];
        }
        return newData;
    }
}
