package net.streamline.api.base.modules;

import com.google.re2j.Pattern;
import net.streamline.api.base.events.Event;
import net.streamline.api.base.events.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Represents a module loader, which handles direct access to specific types
 * of modules
 */
public interface ModuleLoader {

    /**
     * Loads the module contained in the specified file
     *
     * @param file File to attempt to load
     * @return Module that was contained in the specified file, or null if
     *     unsuccessful
     * @throws InvalidModuleException Thrown when the specified file is not a
     *     module
     * @throws UnknownDependencyException If a required dependency could not
     *     be found
     */
    @NotNull
    public Module loadModule(@NotNull File file) throws InvalidModuleException, UnknownDependencyException;

    /**
     * Loads a ModuleDescriptionFile from the specified file
     *
     * @param file File to attempt to load from
     * @return A new ModuleDescriptionFile loaded from the module.yml in the
     *     specified file
     * @throws InvalidDescriptionException If the module description file
     *     could not be created
     */
    @NotNull
    public ModuleDescriptionFile getModuleDescription(@NotNull File file) throws InvalidDescriptionException;

    /**
     * Returns a list of all filename filters expected by this ModuleLoader
     *
     * @return The filters
     */
    @NotNull
    public Pattern[] getModuleFileFilters();

    /**
     * Creates and returns registered listeners for the event classes used in
     * this listener
     *
     * @param listener The object that will handle the eventual call back
     * @param module The module to use when creating registered listeners
     * @return The registered listeners.
     */
    @NotNull
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Module module);

    /**
     * Enables the specified module
     * <p>
     * Attempting to enable a module that is already enabled will have no
     * effect
     *
     * @param module Module to enable
     */
    public void enableModule(@NotNull Module module);

    /**
     * Disables the specified module
     * <p>
     * Attempting to disable a module that is not enabled will have no effect
     *
     * @param module Module to disable
     */
    public void disableModule(@NotNull Module module);
}
