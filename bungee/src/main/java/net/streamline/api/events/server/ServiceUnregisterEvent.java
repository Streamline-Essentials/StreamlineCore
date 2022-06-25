package net.streamline.api.events.server;

import net.streamline.api.modules.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a service is unregistered.
 * <p>
 * Warning: The order in which register and unregister events are called
 * should not be relied upon.
 */
public class ServiceUnregisterEvent extends ServiceEvent {
    public ServiceUnregisterEvent(@NotNull RegisteredServiceProvider<?> serviceProvider) {
        super(serviceProvider);
    }
}
