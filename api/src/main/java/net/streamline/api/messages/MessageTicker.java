package net.streamline.api.messages;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.scheduler.BaseRunnable;

public class MessageTicker extends BaseRunnable {
    @Getter @Setter
    private ProxyMessageOut messageOut;

    public MessageTicker(long delay, ProxyMessageOut out) {
        super(delay, 20);
        this.messageOut = out;
    }

    @Override
    public void run() {
        SLAPI.getInstance().getProxyMessenger().sendMessage(getMessageOut());
        this.cancel();
    }
}
