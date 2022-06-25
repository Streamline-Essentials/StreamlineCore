package net.streamline.api.scheduler;

import net.streamline.api.modules.Module;

public class SimpleAsyncDebugger {
    private SimpleAsyncDebugger next = null;
    private final int expiry;
    private final Module module;
    private final Class<?> clazz;

    SimpleAsyncDebugger(final int expiry, final Module module, final Class<?> clazz) {
        this.expiry = expiry;
        this.module = module;
        this.clazz = clazz;

    }

    final SimpleAsyncDebugger getNextHead(final int time) {
        SimpleAsyncDebugger next, current = this;
        while (time > current.expiry && (next = current.next) != null) {
            current = next;
        }
        return current;
    }

    final SimpleAsyncDebugger setNext(final SimpleAsyncDebugger next) {
        return this.next = next;
    }

    StringBuilder debugTo(final StringBuilder string) {
        for (SimpleAsyncDebugger next = this; next != null; next = next.next) {
            string.append(next.module.getDescription().getName()).append(':').append(next.clazz.getName()).append('@').append(next.expiry).append(',');
        }
        return string;
    }
}
