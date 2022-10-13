package net.streamline.api.messages.proxied;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.utils.MatcherUtils;
import net.streamline.api.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProxiedMessage implements Comparable<ProxiedMessage> {
    @Getter
    private final String defaultListSeparator = "{{,}}";
    @Getter
    private final String argumentKeyMaster = "{{arg[%index%]}}";
    @Getter
    private final String subChannelKey = "{{sub-channel}}";

    @Getter @Setter
    private ConcurrentSkipListMap<Integer, SingleSet<String, String>> arguments = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private ConcurrentSkipListMap<String, String> literalContents = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private String server = "lobby";

    public void setSubChannel(String subChannel) {
        write(getSubChannelKey(), subChannel);
    }

    public String getSubChannel() {
        return getString(getSubChannelKey());
    }

    @Getter
    private final StreamlinePlayer carrier;
    @Getter
    private final boolean proxyOriginated;
    @Getter
    private final String mainChannel;
    @Getter
    private final Date gottenAt;

    public ProxiedMessage(StreamlinePlayer carrier, boolean proxyOriginated, String mainChannel) {
        this.carrier = carrier;
        this.proxyOriginated = proxyOriginated;
        this.mainChannel = mainChannel;
        this.gottenAt = new Date();
    }

    public ProxiedMessage(StreamlinePlayer carrier, boolean proxyOriginated) {
        this(carrier, proxyOriginated, SLAPI.getApiChannel());
    }

    public ProxiedMessage(StreamlinePlayer carrier, boolean proxyOriginated, byte[] message) {
        this(carrier, proxyOriginated);
        writeAll(message);
    }

    public ProxiedMessage(StreamlinePlayer carrier, boolean proxyOriginated, byte[] message, String mainChannel) {
        this(carrier, proxyOriginated, mainChannel);
        writeAll(message);
    }

    public boolean isBackendOriginated() {
        return ! isProxyOriginated();
    }

    public void write(String key, String value) {
        if (key == null) key = "";
        if (value == null) value = "";
        getLiteralContents().put(key, value);
//        if (getSubChannel() == null) return;
//        if (! getSubChannel().equals(ServerInfoMessageBuilder.getSubChannel())) MessageUtils.logInfo("Wrote '" + key + "' : '" + value + "'.");
    }

    public void write(String utf) {
        SingleSet<String, String> r = extrapolate(utf);
        write(r.key, r.value);
    }

    public void writeAll(byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);

        boolean errored = false;
        while (! errored) {
            try {
                String utf = input.readUTF();
                write(utf);
            } catch (Exception e) {
                errored = true;
            }
        }
    }

    public SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) {
            return getArgumentSetFrom(from);
        }

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }

    public SingleSet<String, String> getArgumentSetFrom(String from) {
        String key = getNextArgument();
        SingleSet<String, String> r = new SingleSet<>(key, from);
        getArguments().put(getArguments().size(), r);
        return r;
    }

    public String getNextArgument() {
        return getArgumentKeyMaster().replace("%index%", String.valueOf(getArguments().size()));
    }

    public String getArgument(int index) {
        return getString(getArguments().get(index).key);
    }

    public boolean getBoolean(String key) {
        try {
            return Boolean.parseBoolean(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public long getLong(String key) {
        try {
            return Long.parseLong(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public int getInteger(String key) {
        try {
            return Integer.parseInt(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0D;
        }
    }

    public float getFloat(String key) {
        try {
            return Float.parseFloat(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0F;
        }
    }

    public <T extends Enum<T>> T getEnum(String key, Class<T> en) {
        try {
            return Enum.valueOf(en, getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConcurrentSkipListSet<String> getJustifiedContents() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getLiteralContents().forEach((key, value) -> {
            r.add(getJustifiedContent(key, value));
        });

        return r;
    }

    public String getJustifiedContent(String key, String value) {
        return key + "=" + value + ";";
    }

    public String getString(String key) {
        return getLiteralContents().get(key);
    }

    public ConcurrentSkipListSet<String> getConcurrentStringList(String key, String separator) {
        return new ConcurrentSkipListSet<>(getStringList(key, separator));
    }

    public ConcurrentSkipListSet<String> getConcurrentStringList(String key) {
        return getConcurrentStringList(key, getDefaultListSeparator());
    }

    public List<String> getStringList(String key, String separator) {
        return Arrays.stream(getString(key).split(separator)).toList();
    }

    public List<String> getStringList(String key) {
        return getStringList(key, getDefaultListSeparator());
    }

    public void write(String key, List<String> stringList, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= stringList.size(); i ++) {
            String current = stringList.get(i - 1);
            if (i == stringList.size()) builder.append(current);
            else builder.append(current).append(separator);
        }
        write(key, builder.toString());
    }

    public void write(String key, List<String> stringList) {
        write(key, stringList, getDefaultListSeparator());
    }

    public void write(String key, ConcurrentSkipListSet<String> stringList, String separator) {
        write(key, stringList.stream().toList(), separator);
    }

    public void write(String key, ConcurrentSkipListSet<String> stringList) {
        write(key, stringList, getDefaultListSeparator());
    }

    public byte[] read() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        for (String content : getJustifiedContents()) {
            output.writeUTF(content);
        }

        return output.toByteArray();
    }

    public boolean isReturnableLike() {
        return hasKey(ReturnableMessage.getKey());
    }

    public boolean hasKey(String key) {
        return getLiteralContents().containsKey(key);
    }

    public boolean hasValue(String value) {
        return getLiteralContents().containsValue(value);
    }

    @Override
    public int compareTo(@NotNull ProxiedMessage o) {
        return Long.compare(getGottenAt().getTime(), o.getGottenAt().getTime());
    }
}
