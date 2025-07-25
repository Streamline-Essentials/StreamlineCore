package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;

@Getter @Setter
public class CosmicLogPopEvent extends CosmicEvent {
    private final ConcurrentSkipListMap<Integer, ServerLogTextEvent> poppedLogs;

    public CosmicLogPopEvent(ConcurrentSkipListMap<Integer, ServerLogTextEvent> poppedLogs) {
        this.poppedLogs = poppedLogs;
    }

    public void forEachLog(BiConsumer<Integer, ServerLogTextEvent> consumer) {
        poppedLogs.forEach(consumer);
    }
}
