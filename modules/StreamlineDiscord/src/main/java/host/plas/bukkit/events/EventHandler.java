package host.plas.bukkit.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;

import java.util.concurrent.ConcurrentSkipListMap;

public class EventHandler {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, EventConsumer> handlers = new ConcurrentSkipListMap<>();

    public static <E extends Event> void handle(String eventName, E event) {
        EventConsumer handler = handlers.get(eventName);
        if (handler != null) {
            handler.accept(event);
        }
    }

    public static EventConsumer getHandler(String eventName) {
        return handlers.get(eventName);
    }

    public static void register(String eventName, EventConsumer handler) {
        handlers.put(eventName, handler);
    }

    public static void unregister(String eventName) {
        handlers.remove(eventName);
    }

    public static void init() {

    }

    public static void hDeath() {

    }
}
