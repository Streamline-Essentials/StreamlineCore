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
    private byte[] messages;

    public ProxyMessageOut(String channel, String subChannel, byte... messages) {
        this.channel = channel;
        this.subChannel = subChannel;
        this.server = "";
        this.messages = messages;
    }
}
