package net.streamline.api.messages;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.concurrent.ConcurrentSkipListMap;

public class MessageQueue {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ConcurrentSkipListMap<Date, MessageTicker>> currentTickers = new ConcurrentSkipListMap<>();

    public static void queue(ProxyMessageOut out) {
        ConcurrentSkipListMap<Date, MessageTicker> current = getCurrentTickers().get(out.getServer());
        if (current == null) current = new ConcurrentSkipListMap<>();
        current.put(new Date(), new MessageTicker(100, out));

        getCurrentTickers().put(out.getServer(), current);
    }

    public static void pop(MessageTicker messageTicker) {
        ConcurrentSkipListMap<Date, MessageTicker> current = getCurrentTickers().get(messageTicker.getMessageOut().getServer());
        if (current == null) current = new ConcurrentSkipListMap<>();
        for (Date i : current.keySet()) {
            MessageTicker ticker = current.get(i);
            if (ticker.startedAt.equals(messageTicker.startedAt)) {
                current.remove(i);
                ticker.cancel();
                return;
            }
        }
    }
}
