package net.streamline.api.messages;

import lombok.Getter;
import lombok.Setter;

public class ProxyMessageIn {
    @Getter @Setter
    private String channel;
    @Getter @Setter
    private String subChannel;
    @Getter @Setter
    private byte[] messages;

    public ProxyMessageIn(String channel, String subChannel, byte... messages) {
        this.channel = channel;
        this.subChannel = subChannel;
        this.messages = messages;
    }
}
