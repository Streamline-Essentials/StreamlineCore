package net.streamline.api.modules;

import com.google.re2j.Pattern;
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
