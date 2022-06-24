package net.streamline.api.modules;

import net.streamline.api.events.Event;
import net.streamline.api.events.EventException;
import net.streamline.api.events.EventPriority;
import net.streamline.api.events.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extends RegisteredListener to include timing information
 */
public class TimedRegisteredListener extends RegisteredListener {
    private int count;
    private long totalTime;
    private Class<? extends Event> eventClass;
    private boolean multiple = false;

    public TimedRegisteredListener(@NotNull final Listener moduleListener, @NotNull final EventExecutor eventExecutor, @NotNull final EventPriority eventPriority, @NotNull final Module registeredModule, final boolean listenCancelled) {
        super(moduleListener, eventExecutor, eventPriority, registeredModule, listenCancelled);
    }

    @Override
    public void callEvent(@NotNull Event event) throws EventException {
        if (event.isAsynchronous()) {
            super.callEvent(event);
            return;
        }
        count++;
        Class<? extends Event> newEventClass = event.getClass();
        if (this.eventClass == null) {
            this.eventClass = newEventClass;
        } else if (!this.eventClass.equals(newEventClass)) {
            multiple = true;
            this.eventClass = getCommonSuperclass(newEventClass, this.eventClass).asSubclass(Event.class);
        }
        long start = System.nanoTime();
        super.callEvent(event);
        totalTime += System.nanoTime() - start;
    }

    @NotNull
    private static Class<?> getCommonSuperclass(@NotNull Class<?> class1, @NotNull Class<?> class2) {
        while (!class1.isAssignableFrom(class2)) {
            class1 = class1.getSuperclass();
        }
        return class1;
    }

    /**
     * Resets the call count and total time for this listener
     */
    public void reset() {
        count = 0;
        totalTime = 0;
    }

    /**
     * Gets the total times this listener has been called
     *
     * @return Times this listener has been called
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the total time calls to this listener have taken
     *
     * @return Total time for all calls of this listener
     */
    public long getTotalTime() {
        return totalTime;
    }

    /**
     * Gets the class of the events this listener handled. If it handled
     * multiple classes of event, the closest shared superclass will be
     * returned, such that for any event this listener has handled,
     * <code>this.getEventClass().isAssignableFrom(event.getClass())</code>
     * and no class <code>this.getEventClass().isAssignableFrom(clazz)
     * {@literal && this.getEventClass() != clazz &&}
     * event.getClass().isAssignableFrom(clazz)</code> for all handled events.
     *
     * @return the event class handled by this RegisteredListener
     */
    @Nullable
    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    /**
     * Gets whether this listener has handled multiple events, such that for
     * some two events, <code>eventA.getClass() != eventB.getClass()</code>.
     *
     * @return true if this listener has handled multiple events
     */
    public boolean hasMultiple() {
        return multiple;
    }
}