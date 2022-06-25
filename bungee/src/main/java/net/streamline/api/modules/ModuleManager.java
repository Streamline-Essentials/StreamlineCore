package net.streamline.api.modules;

import net.streamline.api.permissions.Permissible;
import net.streamline.api.permissions.Permission;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Set;

/**
 * Handles all module management from the Server
 */
public interface ModuleManager {

    /**
     * Registers the specified module loader
     *
     * @param loader Class name of the ModuleLoader to register
     * @throws IllegalArgumentException Thrown when the given Class is not a
     *     valid ModuleLoader
     */
    public void registerInterface(@NotNull Class<? extends ModuleLoader> loader) throws IllegalArgumentException;

    /**
     * Checks if the given module is loaded and returns it when applicable
     * <p>
     * Please note that the name of the module is case-sensitive
     *
     * @param name Name of the module to check
     * @return Module if it exists, otherwise null
     */
    @Nullable
    public Module getModule(@NotNull String name);

    /**
     * Gets a list of all currently loaded modules
     *
     * @return Array of Modules
     */
    @NotNull
    public Module[] getModules();

    /**
     * Checks if the given module is enabled or not
     * <p>
     * Please note that the name of the module is case-sensitive.
     *
     * @param name Name of the module to check
     * @return true if the module is enabled, otherwise false
     */
    public boolean isModuleEnabled(@NotNull String name);

    /**
     * Checks if the given module is enabled or not
     *
     * @param module Module to check
     * @return true if the module is enabled, otherwise false
     */
    @Contract("null -> false")
    public boolean isModuleEnabled(@Nullable Module module);

    /**
     * Loads the module in the specified file
     * <p>
     * File must be valid according to the current enabled Module interfaces
     *
     * @param file File containing the module to load
     * @return The Module loaded, or null if it was invalid
     * @throws InvalidModuleException Thrown when the specified file is not a
     *     valid module
     * @throws InvalidDescriptionException Thrown when the specified file
     *     contains an invalid description
     * @throws UnknownDependencyException If a required dependency could not
     *     be resolved
     */
    @Nullable
    public Module loadModule(@NotNull File file) throws InvalidModuleException, InvalidDescriptionException, UnknownDependencyException;

    /**
     * Loads the modules contained within the specified directory
     *
     * @param directory Directory to check for modules
     * @return A list of all modules loaded
     */
    @NotNull
    public Module[] loadModules(@NotNull File directory);

    /**
     * Disables all the loaded modules
     */
    public void disableModules();

    /**
     * Disables and removes all modules
     */
    public void clearModules();

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

    /**
     * Gets a {@link Permission} from its fully qualified name
     *
     * @param name Name of the permission
     * @return Permission, or null if none
     */
    @Nullable
    public Permission getPermission(@NotNull String name);

    /**
     * Adds a {@link Permission} to this module manager.
     * <p>
     * If a permission is already defined with the given name of the new
     * permission, an exception will be thrown.
     *
     * @param perm Permission to add
     * @throws IllegalArgumentException Thrown when a permission with the same
     *     name already exists
     */
    public void addPermission(@NotNull Permission perm);

    /**
     * Removes a {@link Permission} registration from this module manager.
     * <p>
     * If the specified permission does not exist in this module manager,
     * nothing will happen.
     * <p>
     * Removing a permission registration will <b>not</b> remove the
     * permission from any {@link Permissible}s that have it.
     *
     * @param perm Permission to remove
     */
    public void removePermission(@NotNull Permission perm);

    /**
     * Removes a {@link Permission} registration from this module manager.
     * <p>
     * If the specified permission does not exist in this module manager,
     * nothing will happen.
     * <p>
     * Removing a permission registration will <b>not</b> remove the
     * permission from any {@link Permissible}s that have it.
     *
     * @param name Permission to remove
     */
    public void removePermission(@NotNull String name);

    /**
     * Gets the default permissions for the given op status
     *
     * @param op Which set of default permissions to get
     * @return The default permissions
     */
    @NotNull
    public Set<Permission> getDefaultPermissions(boolean op);

    /**
     * Recalculates the defaults for the given {@link Permission}.
     * <p>
     * This will have no effect if the specified permission is not registered
     * here.
     *
     * @param perm Permission to recalculate
     */
    public void recalculatePermissionDefaults(@NotNull Permission perm);

    /**
     * Subscribes the given Permissible for information about the requested
     * Permission, by name.
     * <p>
     * If the specified Permission changes in any form, the Permissible will
     * be asked to recalculate.
     *
     * @param permission Permission to subscribe to
     * @param permissible Permissible subscribing
     */
    public void subscribeToPermission(@NotNull String permission, @NotNull Permissible permissible);

    /**
     * Unsubscribes the given Permissible for information about the requested
     * Permission, by name.
     *
     * @param permission Permission to unsubscribe from
     * @param permissible Permissible subscribing
     */
    public void unsubscribeFromPermission(@NotNull String permission, @NotNull Permissible permissible);

    /**
     * Gets a set containing all subscribed {@link Permissible}s to the given
     * permission, by name
     *
     * @param permission Permission to query for
     * @return Set containing all subscribed permissions
     */
    @NotNull
    public Set<Permissible> getPermissionSubscriptions(@NotNull String permission);

    /**
     * Subscribes to the given Default permissions by operator status
     * <p>
     * If the specified defaults change in any form, the Permissible will be
     * asked to recalculate.
     *
     * @param op Default list to subscribe to
     * @param permissible Permissible subscribing
     */
    public void subscribeToDefaultPerms(boolean op, @NotNull Permissible permissible);

    /**
     * Unsubscribes from the given Default permissions by operator status
     *
     * @param op Default list to unsubscribe from
     * @param permissible Permissible subscribing
     */
    public void unsubscribeFromDefaultPerms(boolean op, @NotNull Permissible permissible);

    /**
     * Gets a set containing all subscribed {@link Permissible}s to the given
     * default list, by op status
     *
     * @param op Default list to query for
     * @return Set containing all subscribed permissions
     */
    @NotNull
    public Set<Permissible> getDefaultPermSubscriptions(boolean op);

    /**
     * Gets a set of all registered permissions.
     * <p>
     * This set is a copy and will not be modified live.
     *
     * @return Set containing all current registered permissions
     */
    @NotNull
    public Set<Permission> getPermissions();

    /**
     * Returns whether or not timing code should be used for event calls
     *
     * @return True if event timings are to be used
     */
    public boolean useTimings();
}