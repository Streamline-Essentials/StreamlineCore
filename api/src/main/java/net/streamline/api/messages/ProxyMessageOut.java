package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

public class ProxyMessageOut {
    @Getter @Setter
    private String channel;
    @Getter @Setter
    private String subChannel;
    @Getter @Setter
    private String server;
    @Getter @Setter
    private ConcurrentSkipListMap<Integer, Object> messages = new ConcurrentSkipListMap<>();

    public ProxyMessageOut(String channel, String subChannel, Object... objects) {
        this.channel = channel;
        this.subChannel = subChannel;
        this.server = "";
        this.messages = new ConcurrentSkipListMap<>();
        for (Object o : objects) {
            this.messages.put(this.messages.size(), o);
        }
    }

    public ProxyMessageOut(String channel, String subChannel, List<Object> objects) {
        this.channel = channel;
        this.subChannel = subChannel;
        this.messages = new ConcurrentSkipListMap<>();
        for (Object o : objects) {
            this.messages.put(this.messages.size(), o);
        }
    }

    public byte[] asWrite() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        for (int i : getMessages().keySet()) {
            Object object = getMessages().get(i);

            if (object instanceof Boolean thing) {
                output.writeBoolean(thing);
            }
            if (object instanceof Byte thing) {
                output.writeByte(thing);
            }
            if (object instanceof Character thing) {
                output.writeChar(thing);
            }
            if (object instanceof Double thing) {
                output.writeDouble(thing);
            }
            if (object instanceof Float thing) {
                output.writeFloat(thing);
            }
            if (object instanceof Integer thing) {
                output.writeInt(thing);
            }
            if (object instanceof Long thing) {
                output.writeLong(thing);
            }
            if (object instanceof Short thing) {
                output.writeShort(thing);
            }
            if (object instanceof String thing) {
                output.writeUTF(thing);
            }
        }
        return output.toByteArray();
    }
}
