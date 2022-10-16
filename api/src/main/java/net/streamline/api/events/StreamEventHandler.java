package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import net.streamline.api.utils.MessageUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A list of event handlers, stored per-event.
 */
public class StreamEventHandler {
    @Getter @Setter
    private static ConcurrentSkipListMap<Integer, StreamlineListener> listeners = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static int index = 0;
    @Getter @Setter
    public static List<Method> methods = new ArrayList<>();

    public static void kill() {
        setListeners(new ConcurrentSkipListMap<>());
        setIndex(0);
        setMethods(new ArrayList<>());
    }

    public ConcurrentSkipListMap<String, Method> getConcurrentMethods() {
        ConcurrentSkipListMap<String, Method> r = new ConcurrentSkipListMap<>();

        for (Method method : getClass().getMethods()) {
            r.put(method.getName(), method);
        }
        for (Method method : getClass().getDeclaredMethods()) {
            r.put(method.getName(), method);
        }

        return r;
    }

    public static ConcurrentHashMap<Class<? extends StreamlineEvent>, ConcurrentSkipListSet<RegisteredListener<?>>> setUp(ModuleLike module, StreamlineListener listener) {
        ConcurrentHashMap<Class<? extends StreamlineEvent>, ConcurrentSkipListSet<RegisteredListener<?>>> ret = new ConcurrentHashMap<>();

        for (final Method method : getMethods()) {
            final EventProcessor eh = method.getAnnotation(EventProcessor.class);
            if (eh == null) continue;
            // Do not register bridge or synthetic methods to avoid event duplication
            // Fixes SPIGOT-893
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1 || !StreamlineEvent.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                module.logSevere("Module '" + module.identifier() + "' attempted to register an invalid EventProcessor method signature \"" + method.toGenericString() + "\" in '" + listener.getClass() + "'");
                continue;
            }
            final Class<? extends StreamlineEvent> eventClass = checkClass.asSubclass(StreamlineEvent.class);
            method.setAccessible(true);
            ConcurrentSkipListSet<RegisteredListener<?>> eventSet = ret.computeIfAbsent(eventClass, k -> new ConcurrentSkipListSet<>());

            for (Class<?> clazz = eventClass; StreamlineEvent.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
                // This loop checks for extending deprecated events
                if (clazz.getAnnotation(Deprecated.class) != null) {
                    module.logInfo(
                            String.format(
                                    "'%s' has registered a listener for '%s' on method '%s', but the event is Deprecated. Please notify these authors: %s.",
                                    module.identifier(),
                                    clazz.getName(),
                                    method.toGenericString(),
                                    module.getAuthorsStringed()));
                    break;
                }
            }

            EventExecutor executor = (listener1, event) -> {
                try {
                    if (!eventClass.isAssignableFrom(event.getClass())) {
                        return;
                    }
                    method.invoke(listener1, event);
                } catch (InvocationTargetException ex) {
                    throw new EventException(ex.getCause());
                } catch (Throwable t) {
                    throw new EventException(t);
                }

                event.setCompleted(true);
            };

            if (module instanceof StreamlineModule sm) eventSet.add(new RegisteredListener<>(listener, executor, eh.priority(), sm, eh.ignoreCancelled()));
            if (module instanceof StreamlineSpringModule ssm) eventSet.add(new RegisteredListener<>(listener, executor, eh.priority(), ssm, eh.ignoreCancelled()));
        }
        return ret;
    }

    @Getter @Setter
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
        setUp(module, listener).forEach((aClass, registeredListeners) -> {
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
