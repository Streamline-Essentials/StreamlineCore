package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import net.streamline.api.utils.MessageUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A list of event handlers, stored per-event.
 */
public class StreamEventHandler {    @Getter @Setter
    private static ConcurrentHashMap<Class<? extends StreamlineEvent>, ConcurrentSkipListSet<RegisteredListener<?>>> regularEvents = new ConcurrentHashMap<>();

    public static String getMethodNamed(Method method) {
        return method.getDeclaringClass().getSimpleName() + "::" + method.getName();
    }

    public static void put(Class<? extends StreamlineEvent> aClass, RegisteredListener<?>... registeredListener) {
        ConcurrentSkipListSet<RegisteredListener<?>> listeners = getRegularEvents().get(aClass);
        if (listeners == null) listeners = new ConcurrentSkipListSet<>();
        listeners.addAll(Arrays.stream(registeredListener).toList());
        getRegularEvents().put(aClass, listeners);
    }

    public static void unput(Class<? extends StreamlineEvent> aClass, RegisteredListener<?>... registeredListener) {
        ConcurrentSkipListSet<RegisteredListener<?>> listeners = getRegularEvents().get(aClass);
        if (listeners == null) listeners = new ConcurrentSkipListSet<>();
        Arrays.stream(registeredListener).toList().forEach(listeners::remove);
        getRegularEvents().put(aClass, listeners);
    }

    public static void bake(StreamlineListener listener, ModuleLike module) {
        listener.setUp(module).forEach((aClass, registeredListeners) -> {
            put(aClass, registeredListeners.toArray(new RegisteredListener<?>[0]));
        });
    }

    public static <T extends ModuleLike> void unbake(T module) {
        getRegularEvents().forEach((aClass, registeredListeners) -> {
            registeredListeners.forEach(registeredListener -> {
                if (registeredListener.getModule().equals(module)) getRegularEvents().get(aClass).remove(registeredListener);
            });
        });
    }

    public static ConcurrentSkipListSet<RegisteredListener<?>> getRegularListeners(Class<? extends StreamlineEvent> event) {
        ConcurrentSkipListSet<RegisteredListener<?>> listeners = new ConcurrentSkipListSet<>();

        getRegularEvents().forEach((aClass, registeredListeners) -> {
            if (event.equals(aClass)) listeners.addAll(registeredListeners);
        });

        return listeners;
    }

    public static void fireEvent(StreamlineEvent event) {
        getRegularListeners(event.getClass()).forEach(registeredListener -> {
            if (! registeredListener.getModule().isEnabled()) {
                return;
            }

            try {
                registeredListener.callEvent(event);
            } catch (Throwable ex) {
                MessageUtils.logSevere("Could not pass event '" + event.getEventName() + "' to '" + registeredListener.getModule().identifier() + "' for reason: ");
                ex.printStackTrace();
            }
        });
    }
}
