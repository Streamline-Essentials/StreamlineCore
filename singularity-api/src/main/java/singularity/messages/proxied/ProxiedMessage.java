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

/**
 * A structured plugin-messaging-channel packet that can be sent between the
 * proxy and backend servers.
 *
 * <p>Each message is tied to a {@link CosmicPlayer} who acts as the carrier,
 * a main channel identifier, and an optional sub-channel.  Arbitrary key/value
 * pairs are stored in {@code literalContents} and serialised to a compact
 * {@code key=value;} wire format.  Positional (unnamed) arguments are
 * auto-keyed via an index template and tracked separately in {@code arguments}.
 *
 * <p>Comparable by the time the message was received; earlier messages sort
 * before later ones.
 */
@Getter
public class ProxiedMessage implements Comparable<ProxiedMessage> {

    /**
     * Default separator token used when encoding/decoding {@link List} values
     * as a single delimited string.
     */
    private final String defaultListSeparator = "{{,}}";

    /**
     * Template key pattern for positional arguments; {@code %index%} is
     * replaced with the zero-based argument index at insertion time.
     */
    private final String argumentKeyMaster = "{{arg[%index%]}}";

    /**
     * Literal key used to store the sub-channel name inside
     * {@code literalContents}.
     */
    private final String subChannelKey = "{{sub-channel}}";

    /**
     * Ordered map of positional argument entries, indexed by insertion order.
     * Each entry holds the generated key and the raw string value.
     */
    @Setter
    private ConcurrentSkipListMap<Integer, SingleSet<String, String>> arguments = new ConcurrentSkipListMap<>();

    /**
     * All key/value pairs stored in this message, including named fields and
     * positional argument slots.  This is the canonical backing store that is
     * serialised to bytes via {@link #read()}.
     */
    @Setter
    private ConcurrentSkipListMap<String, String> literalContents = new ConcurrentSkipListMap<>();

    /**
     * Writes the sub-channel identifier into this message's literal contents.
     *
     * @param subChannel the sub-channel name to set
     */
    public void setSubChannel(String subChannel) {
        write(getSubChannelKey(), subChannel);
    }

    /**
     * Returns the sub-channel identifier stored in this message.
     *
     * @return the sub-channel string, or {@code null} if not set
     */
    public String getSubChannel() {
        return getString(getSubChannelKey());
    }

    /**
     * Produces a human-readable summary of this message's contents in the
     * form {@code subChannel->key=value;key=value;...}.
     *
     * @return a formatted string representation of the message payload
     */
    public String getLiteralAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getSubChannel()).append("->");
        getLiteralContents().forEach((key, value) -> {
            builder.append(getJustifiedContent(key, value));
        });
        return builder.toString();
    }

    /** The player used as the messaging carrier for this packet. */
    private final CosmicPlayer carrier;

    /**
     * Whether this message originated on the proxy side ({@code true}) or on
     * a backend server ({@code false}).
     */
    private final boolean proxyOriginated;

    /** The BungeeCord/Velocity plugin-messaging channel this message travels on. */
    private final String mainChannel;

    /** The wall-clock timestamp at which this message object was created. */
    private final Date gottenAt;

    /**
     * Creates a new outbound message on the specified channel.
     *
     * @param carrier          the player used to route the message
     * @param proxyOriginated  {@code true} if the message was created on the proxy
     * @param mainChannel      the plugin-messaging channel name
     */
    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated, String mainChannel) {
        this.carrier = carrier;
        this.proxyOriginated = proxyOriginated;
        this.mainChannel = mainChannel;
        this.gottenAt = new Date();
    }

    /**
     * Creates a new message on the default Singularity API channel.
     *
     * @param carrier         the player used to route the message
     * @param proxyOriginated {@code true} if the message was created on the proxy
     */
    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated) {
        this(carrier, proxyOriginated, Singularity.getApiChannel());
    }

    /**
     * Creates a message on the default API channel and immediately deserialises
     * the supplied byte array into its contents.
     *
     * @param carrier         the player used to route the message
     * @param proxyOriginated {@code true} if the message was created on the proxy
     * @param message         raw byte payload to deserialise
     */
    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated, byte[] message) {
        this(carrier, proxyOriginated);
        writeAll(message);
    }

    /**
     * Creates a message on the specified channel and immediately deserialises
     * the supplied byte array into its contents.
     *
     * @param carrier         the player used to route the message
     * @param proxyOriginated {@code true} if the message was created on the proxy
     * @param message         raw byte payload to deserialise
     * @param mainChannel     the plugin-messaging channel name
     */
    public ProxiedMessage(CosmicPlayer carrier, boolean proxyOriginated, byte[] message, String mainChannel) {
        this(carrier, proxyOriginated, mainChannel);
        writeAll(message);
    }

    /**
     * Returns {@code true} if this message did not originate on the proxy
     * (i.e. it was sent by a backend server).
     *
     * @return {@code true} when backend-originated
     */
    public boolean isBackendOriginated() {
        return ! isProxyOriginated();
    }

    /**
     * Stores a named string value in this message.  {@code null} keys and
     * values are normalised to empty strings.
     *
     * @param key   the field key
     * @param value the field value
     */
    public void write(String key, String value) {
        if (key == null) key = "";
        if (value == null) value = "";
        getLiteralContents().put(key, value);
//        if (getSubChannel() == null) return;
//        if (! getSubChannel().equals(ServerInfoMessageBuilder.getSubChannel())) MessageUtils.logInfo("Wrote '" + key + "' : '" + value + "'.");
    }

    /**
     * Parses a single {@code key=value;} UTF string and stores the extracted
     * pair.  If the string does not match the pattern it is stored as a
     * positional argument.
     *
     * @param utf the encoded UTF string to parse and write
     */
    public void write(String utf) {
        SingleSet<String, String> r = extrapolate(utf);
        write(r.getKey(), r.getValue());
    }

    /**
     * Deserialises a full byte-array payload by reading each UTF string entry
     * and writing it into this message until the stream is exhausted.
     *
     * @param bytes the raw byte payload to read
     */
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

    /**
     * Attempts to parse a {@code key=value;} string into a key/value pair.
     * If the pattern does not match, the whole string is stored as the next
     * positional argument.
     *
     * @param from the raw string to parse
     * @return a {@link SingleSet} containing the resolved key and value
     */
    public SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) {
            return getArgumentSetFrom(from);
        }

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }

    /**
     * Registers a raw string as the next positional argument, generating an
     * auto-indexed key for it.
     *
     * @param from the raw argument value
     * @return the generated key/value pair that was inserted
     */
    public SingleSet<String, String> getArgumentSetFrom(String from) {
        String key = getNextArgument();
        SingleSet<String, String> r = new SingleSet<>(key, from);
        getArguments().put(getArguments().size(), r);
        return r;
    }

    /**
     * Returns the auto-generated key that the <em>next</em> positional
     * argument would receive.
     *
     * @return the next argument key string
     */
    public String getNextArgument() {
        return getArgumentKeyMaster().replace("%index%", String.valueOf(getArguments().size()));
    }

    /**
     * Returns the string value of the positional argument at the given index.
     *
     * @param index zero-based argument index
     * @return the argument value, or {@code null} if the index does not exist
     */
    public String getArgument(int index) {
        return getString(getArguments().get(index).getKey());
    }

    /**
     * Returns the boolean value stored under the given key.
     *
     * @param key the field key
     * @return the parsed boolean, or {@code false} on parse failure
     */
    public boolean getBoolean(String key) {
        try {
            return Boolean.parseBoolean(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the long value stored under the given key.
     *
     * @param key the field key
     * @return the parsed long, or {@code 0} on parse failure
     */
    public long getLong(String key) {
        try {
            return Long.parseLong(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * Returns the int value stored under the given key.
     *
     * @param key the field key
     * @return the parsed integer, or {@code 0} on parse failure
     */
    public int getInteger(String key) {
        try {
            return Integer.parseInt(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Returns the double value stored under the given key.
     *
     * @param key the field key
     * @return the parsed double, or {@code 0.0} on parse failure
     */
    public double getDouble(String key) {
        try {
            return Double.parseDouble(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0D;
        }
    }

    /**
     * Returns the float value stored under the given key.
     *
     * @param key the field key
     * @return the parsed float, or {@code 0.0f} on parse failure
     */
    public float getFloat(String key) {
        try {
            return Float.parseFloat(getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0F;
        }
    }

    /**
     * Returns the enum constant of the given type whose name matches the value
     * stored under the given key.
     *
     * @param <T> the enum type
     * @param key the field key
     * @param en  the {@link Class} of the enum type
     * @return the matching constant, or {@code null} on lookup failure
     */
    public <T extends Enum<T>> T getEnum(String key, Class<T> en) {
        try {
            return Enum.valueOf(en, getLiteralContents().get(key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns all key/value entries serialised to the {@code key=value;} wire
     * format as a sorted set of strings.
     *
     * @return a {@link ConcurrentSkipListSet} of justified content strings
     */
    public ConcurrentSkipListSet<String> getJustifiedContents() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getLiteralContents().forEach((key, value) -> {
            r.add(getJustifiedContent(key, value));
        });

        return r;
    }

    /**
     * Serialises a single key/value pair to the {@code key=value;} wire format.
     *
     * @param key   the field key
     * @param value the field value
     * @return the formatted string
     */
    public String getJustifiedContent(String key, String value) {
        return key + "=" + value + ";";
    }

    /**
     * Returns the raw string value stored under the given key, or {@code null}
     * if the key is absent.
     *
     * @param key the field key
     * @return the stored string value, or {@code null}
     */
    public String getString(String key) {
        return getLiteralContents().get(key);
    }

    /**
     * Returns the value stored under the given key as a concurrent sorted set,
     * splitting on the supplied separator.
     *
     * @param key       the field key
     * @param separator the delimiter used to split the stored string
     * @return a {@link ConcurrentSkipListSet} of the individual string tokens
     */
    public ConcurrentSkipListSet<String> getConcurrentStringList(String key, String separator) {
        return new ConcurrentSkipListSet<>(getStringList(key, separator));
    }

    /**
     * Returns the value stored under the given key as a concurrent sorted set,
     * splitting on the {@link #defaultListSeparator}.
     *
     * @param key the field key
     * @return a {@link ConcurrentSkipListSet} of the individual string tokens
     */
    public ConcurrentSkipListSet<String> getConcurrentStringList(String key) {
        return getConcurrentStringList(key, getDefaultListSeparator());
    }

    /**
     * Returns the value stored under the given key as a list, splitting on the
     * supplied separator.
     *
     * @param key       the field key
     * @param separator the delimiter used to split the stored string
     * @return a {@link List} of tokens, or an empty list if the key is absent
     */
    public List<String> getStringList(String key, String separator) {
        String s = getString(key);
        if (s == null) return new ArrayList<>();
        return Arrays.stream(
                    s.split(MatcherUtils.makeLiteral(separator))
                ).collect(Collectors.toList());
    }

    /**
     * Returns the value stored under the given key as a list, splitting on the
     * {@link #defaultListSeparator}.
     *
     * @param key the field key
     * @return a {@link List} of tokens, or an empty list if the key is absent
     */
    public List<String> getStringList(String key) {
        return getStringList(key, getDefaultListSeparator());
    }

    /**
     * Encodes a {@link List} as a single delimited string and stores it under
     * the given key using the supplied separator.
     *
     * @param key        the field key
     * @param stringList the list of values to encode
     * @param separator  the delimiter to place between list elements
     */
    public void write(String key, List<String> stringList, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= stringList.size(); i ++) {
            String current = stringList.get(i - 1);
            if (i == stringList.size()) builder.append(current);
            else builder.append(current).append(separator);
        }
        write(key, builder.toString());
    }

    /**
     * Encodes a {@link List} as a single delimited string and stores it under
     * the given key using the {@link #defaultListSeparator}.
     *
     * @param key        the field key
     * @param stringList the list of values to encode
     */
    public void write(String key, List<String> stringList) {
        write(key, stringList, getDefaultListSeparator());
    }

    /**
     * Encodes a {@link ConcurrentSkipListSet} as a single delimited string and
     * stores it under the given key using the supplied separator.
     *
     * @param key        the field key
     * @param stringList the set of values to encode
     * @param separator  the delimiter to place between elements
     */
    public void write(String key, ConcurrentSkipListSet<String> stringList, String separator) {
        write(key, new ArrayList<>(stringList), separator);
    }

    /**
     * Encodes a {@link ConcurrentSkipListSet} as a single delimited string and
     * stores it under the given key using the {@link #defaultListSeparator}.
     *
     * @param key        the field key
     * @param stringList the set of values to encode
     */
    public void write(String key, ConcurrentSkipListSet<String> stringList) {
        write(key, stringList, getDefaultListSeparator());
    }

    /**
     * Serialises all literal contents of this message to a raw byte array
     * suitable for writing to a plugin-messaging channel.
     *
     * @return the serialised byte payload
     */
    public byte[] read() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        for (String content : getJustifiedContents()) {
            output.writeUTF(content);
        }

        return output.toByteArray();
    }

    /**
     * Returns {@code true} if this message contains the returnable-message
     * correlation key, indicating it is a reply to a previously sent
     * {@link ReturnableMessage}.
     *
     * @return {@code true} when this message matches the returnable pattern
     */
    public boolean isReturnableLike() {
        return hasKey(ReturnableMessage.getKey());
    }

    /**
     * Returns {@code true} if the given key is present in this message's
     * literal contents.
     *
     * @param key the key to test
     * @return {@code true} if the key exists
     */
    public boolean hasKey(String key) {
        return getLiteralContents().containsKey(key);
    }

    /**
     * Returns {@code true} if the given value appears in this message's
     * literal contents under any key.
     *
     * @param value the value to search for
     * @return {@code true} if at least one entry has this value
     */
    public boolean hasValue(String value) {
        return getLiteralContents().containsValue(value);
    }

    /**
     * Dispatches this message through the platform's {@link singularity.messages.ProxyMessenger}.
     */
    public void send() {
        Singularity.getInstance().getProxyMessenger().sendMessage(this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Messages are ordered by their {@link #gottenAt} timestamp so that
     * earlier messages sort before later ones.
     */
    @Override
    public int compareTo(@NotNull ProxiedMessage o) {
        return Long.compare(getGottenAt().getTime(), o.getGottenAt().getTime());
    }

    /**
     * Returns the name of the server the carrier player is currently connected
     * to.
     *
     * @return the carrier's current server name
     */
    public String getServerName() {
        return getCarrier().getServerName();
    }

    /**
     * Returns the {@link CosmicServer} the carrier player is currently
     * connected to.
     *
     * @return the carrier's current server
     */
    public CosmicServer getServer() {
        return getCarrier().getServer();
    }
}
