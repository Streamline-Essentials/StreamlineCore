package net.streamline.api.modules;

import org.jetbrains.annotations.NotNull;

public class RegisteredServiceProvider<T> implements Comparable<RegisteredServiceProvider<?>> {
    private Class<T> service;
    private Module module;
    private T provider;
    private ServicePriority priority;

    public RegisteredServiceProvider(@NotNull Class<T> service, @NotNull T provider, @NotNull ServicePriority priority, @NotNull Module module) {
        this.service = service;
        this.module = module;
        this.provider = provider;
        this.priority = priority;
    }

    @NotNull
    public Class<T> getService() {
        return service;
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    @NotNull
    public T getProvider() {
        return provider;
    }

    @NotNull
    public ServicePriority getPriority() {
        return priority;
    }

    @Override
    public int compareTo(@NotNull RegisteredServiceProvider<?> other) {
        if (priority.ordinal() == other.getPriority().ordinal()) {
            return 0;
        } else {
            return priority.ordinal() < other.getPriority().ordinal() ? 1 : -1;
        }
    }
}
