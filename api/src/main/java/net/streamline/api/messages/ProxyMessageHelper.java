package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static byte[] getSubChannel(String subChannel) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(subChannel);

        return output.toByteArray();
    }

    public static byte[] concat(byte[]... arrays) {
        List<Byte> list = new ArrayList<>();

        for (byte[] array : arrays) {
            for (byte b : array) {
                list.add(b);
            }
        }

        byte[] r = new byte[list.size()];

        for (int i = 0; i < list.size(); i ++) {
            r[i] = list.get(i);
        }

        return r;
    }
}
