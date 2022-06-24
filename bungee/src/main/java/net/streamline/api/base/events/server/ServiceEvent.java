package net.streamline.api.base.events.server;

import net.streamline.api.base.events.server.ServerEvent;
import net.streamline.api.base.modules.RegisteredServiceProvider;
import net.streamline.api.base.modules.ServicesManager;
import org.jetbrains.annotations.NotNull;

/**
 * An event relating to a registered service. This is called in a {@link
 * ServicesManager}
 */
public abstract class ServiceEvent extends ServerEvent {
    private final RegisteredServiceProvider<?> provider;

    public ServiceEvent(@NotNull final RegisteredServiceProvider<?> provider) {
        this.provider = provider;
    }

    @NotNull
    public RegisteredServiceProvider<?> getProvider() {
        return provider;
    }
}