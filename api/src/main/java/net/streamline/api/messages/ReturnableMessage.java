package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.events.EventProcessor;
import net.streamline.api.events.StreamlineListener;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.scheduler.BaseRunnable;

public class ReturnableMessage extends BaseRunnable implements StreamlineListener {
    @Getter
    private static final long period = 1L;
    @Getter
    private static final long timeout = 200L;

    @Getter @Setter
    private ProxyMessageOut messageOut;
    @Getter @Setter
    private String subChannel;
    @Getter @Setter
    private String[] returned;

    @Getter @Setter
    private boolean finished;

    public ReturnableMessage(ProxyMessageOut out, String subChannel, int lines) {
        super(getTimeout(), getPeriod());
        setMessageOut(out);
        setSubChannel(subChannel);

        String[] returned = new String[lines];
        for (int i = 0; i < returned.length; i ++) {
            returned[i] = "";
        }
        setReturned(returned);

        setFinished(false);

        SLAPI.getInstance().getProxyMessenger().sendMessage(out);

        ModuleUtils.listen(this, SLAPI.getInstance().getBaseModule());
    }

    @Override
    public void run() {
        setFinished(true);
        cancel();
    }

    @EventProcessor
    public void onProxyMessage(ProxyMessageEvent event) {
        if (isFinished()) return;

        if (! event.getMessage().getSubChannel().equals(getSubChannel())) return;

        ByteArrayDataInput input = ByteStreams.newDataInput(event.getMessage().getMessages());

        if (! getSubChannel().equals(input.readUTF())) {
            SLAPI.getInstance().getMessenger().logWarning("Data mis-match on ProxyMessageIn for '" + ResourcePackMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
            return;
        }

        String[] r = getReturned().clone();
        for (int i = 0; i < r.length; i ++) {
            r[i] = input.readUTF();
        }
        setReturned(r);

        setFinished(true);
        cancel();
    }
}
