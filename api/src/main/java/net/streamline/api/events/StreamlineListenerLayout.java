package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Simple interface for tagging all EventListeners
 */
public class StreamlineListenerLayout {
    @Getter @Setter
    private int index;
    @Getter @Setter
    private List<Method> methods = new ArrayList<>();
    @Getter @Setter
    private StreamlineListener listener;

    public StreamlineListenerLayout(StreamlineListener listener) {
        setListener(listener);
        setIndex(StreamEventHandler.getListeners().size());
        StreamEventHandler.getListeners().put(getIndex(), getListener());
        setMethods(new ArrayList<>(getConcurrentMethods().values()));
    }

    public StreamlineListenerLayout(List<Method> methods) {
        setListener(listener);
        setIndex(StreamEventHandler.getListeners().size());
        StreamEventHandler.getListeners().put(getIndex(), getListener());
        setMethods(methods);
    }

    public ConcurrentSkipListMap<String, Method> getConcurrentMethods() {
        ConcurrentSkipListMap<String, Method> r = new ConcurrentSkipListMap<>();

        for (Method method : getListener().getClass().getMethods()) {
            r.put(method.getName(), method);
        }
        for (Method method : getListener().getClass().getDeclaredMethods()) {
            r.put(method.getName(), method);
        }

        return r;
    }

    public ConcurrentHashMap<Class<? extends StreamlineEvent>, ConcurrentSkipListSet<RegisteredListener<?>>> setUp(ModuleLike module) {
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
                module.logSevere("Module '" + module.identifier() + "' attempted to register an invalid EventProcessor method signature \"" + method.toGenericString() + "\" in '" + getListener().getClass() + "'");
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

            if (module instanceof StreamlineModule sm)
                eventSet.add(new RegisteredListener<>(getListener(), executor, eh.priority(), sm, eh.ignoreCancelled()));
            if (module instanceof StreamlineSpringModule ssm)
                eventSet.add(new RegisteredListener<>(getListener(), executor, eh.priority(), ssm, eh.ignoreCancelled()));
        }
        return ret;
    }
}
