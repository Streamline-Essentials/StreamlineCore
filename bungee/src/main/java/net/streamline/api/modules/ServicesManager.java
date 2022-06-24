package net.streamline.api.modules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface ServicesManager {
    /**
     * Register a provider of a service.
     *
     * @param <T> Provider
     * @param service service class
     * @param provider provider to register
     * @param module module with the provider
     * @param priority priority of the provider
     */
    public <T> void register(@NotNull Class<T> service, @NotNull T provider, @NotNull Module module, @NotNull ServicePriority priority);

    /**
     * Unregister all the providers registered by a particular module.
     *
     * @param module The module
     */
    public void unregisterAll(@NotNull Module module);

    /**
     * Unregister a particular provider for a particular service.
     *
     * @param service The service interface
     * @param provider The service provider implementation
     */
    public void unregister(@NotNull Class<?> service, @NotNull Object provider);

    /**
     * Unregister a particular provider.
     *
     * @param provider The service provider implementation
     */
    public void unregister(@NotNull Object provider);

    /**
     * Queries for a provider. This may return null if no provider has been
     * registered for a service. The highest priority provider is returned.
     *
     * @param <T> The service interface
     * @param service The service interface
     * @return provider or null
     */
    @Nullable
    public <T> T load(@NotNull Class<T> service);

    /**
     * Queries for a provider registration. This may return null if no provider
     * has been registered for a service.
     *
     * @param <T> The service interface
     * @param service The service interface
     * @return provider registration or null
     */
    @Nullable
    public <T> RegisteredServiceProvider<T> getRegistration(@NotNull Class<T> service);

    /**
     * Get registrations of providers for a module.
     *
     * @param module The module
     * @return provider registrations
     */
    @NotNull
    public List<RegisteredServiceProvider<?>> getRegistrations(@NotNull Module module);

    /**
     * Get registrations of providers for a service. The returned list is
     * unmodifiable.
     *
     * @param <T> The service interface
     * @param service The service interface
     * @return list of registrations
     */
    @NotNull
    public <T> Collection<RegisteredServiceProvider<T>> getRegistrations(@NotNull Class<T> service);

    /**
     * Get a list of known services. A service is known if it has registered
     * providers for it.
     *
     * @return list of known services
     */
    @NotNull
    public Collection<Class<?>> getKnownServices();

    /**
     * Returns whether a provider has been registered for a service. Do not
     * check this first only to call <code>load(service)</code> later, as that
     * would be a non-thread safe situation.
     *
     * @param <T> service
     * @param service service to check
     * @return whether there has been a registered provider
     */
    public <T> boolean isProvidedFor(@NotNull Class<T> service);
}
