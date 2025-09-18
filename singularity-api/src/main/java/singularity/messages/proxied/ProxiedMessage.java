package singularity.messages.proxied;

import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.server.CosmicServer;
import singularity.messages.answered.ReturnableMessage;
import singularity.objects.SingleSet;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Getter
public class ProxiedMessage implements Comparable<ProxiedMessage> {
    private final String defaultListSeparator = "{{,}}";
    private final String argumentKeyMaster = "{{arg[%index%]}}";
    private final String subChannelKey = "{{sub-channel}}";

    @Setter
    private ConcurrentSkipListMap<Integer, SingleSet<String, String>> arguments = new ConcurrentSkipListMap<>();

    @Setter
    private ConcurrentSkipListMap<String, String> literalContents = new ConcurrentSkipListMap<>();

    public void setSubChannel(String subChannel) {
        write(getSubChannelKey(), subChannel);
    }

    public String getSubChannel() {
        return getString(getSubChannelKey());
    }

    public String getLiteralAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getSubChannel()).append("->");
        getLiteralContents().forEach((key, value) -> {
            builder.append(getJustifiedContent(key, value));
        });
        return builder.toString();
    }

    private final CosmicPlayer carrier;
    private final boolean proxyOriginated;
    private final String mainChannel;
    private final Date gottenAt;

    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated, String mainChannel) {
        this.carrier = carrier;
        this.proxyOriginated = proxyOriginated;
        this.mainChannel = mainChannel;
        this.gottenAt = new Date();
    }

    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated) {
        this(carrier, proxyOriginated, Singularity.getApiChannel());
    }

    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated, byte[] message) {
        this(carrier, proxyOriginated);
        writeAll(message);
    }

    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated, byte[] message, String mainChannel) {
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
        write(r.getKey(), r.getValue());
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
        return getString(getArguments().get(index).getKey());
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
        String s = getString(key);
        if (s == null) return new ArrayList<>();
        return Arrays.stream(
                    s.split(MatcherUtils.makeLiteral(separator))
                ).collect(Collectors.toList());
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
        write(key, new ArrayList<>(stringList), separator);
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

    public void send() {
        Singularity.getInstance().getProxyMessenger().sendMessage(this);
    }

    @Override
    public int compareTo(@NotNull ProxiedMessage o) {
        return Long.compare(getGottenAt().getTime(), o.getGottenAt().getTime());
    }

    public String getServerName() {
        return getCarrier().getServerName();
    }

    public CosmicServer getServer() {
        return getCarrier().getServer();
    }
}
