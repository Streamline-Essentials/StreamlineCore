package net.streamline.api.base.timers;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.messages.ProxyMessageHelper;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MathUtils;

import java.util.concurrent.ConcurrentSkipListMap;

public class CachePurgeTimer extends BaseRunnable {
    public CachePurgeTimer() {
        super(200, 200);
    }

    @Override
    public void run() {
        ProxyMessageHelper.getPendingMessages().forEach((date, returnableMessage) -> {
            if (returnableMessage.isFinished()) {
                if (! returnableMessage.isCancelled()) returnableMessage.cancel();
                ProxyMessageHelper.getPendingMessages().remove(date);
            }
        });
    }
}
