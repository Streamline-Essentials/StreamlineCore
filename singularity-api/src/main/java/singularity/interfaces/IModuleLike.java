package singularity.interfaces;

import gg.drak.thebase.objects.handling.derived.IModifierEventable;
import singularity.command.ModuleCommand;

import java.io.File;
import java.io.InputStream;

/**
 * Core lifecycle and resource contract for any Streamline module or module-like component.
 *
 * <p>All PF4J-based modules loaded by {@code ModuleManager} implement this interface.
 * It extends {@link IModifierEventable} for cross-platform event modifier support and
 * {@link Comparable} to enable deterministic ordering of modules during load/unload cycles.</p>
 */
public interface IModuleLike extends IModifierEventable, Comparable<IModuleLike> {

    /**
     * Returns the unique identifier of this module (typically the module's name or ID).
     *
     * @return the non-null module identifier
     */
    String getIdentifier();

    /**
     * Returns a human-readable, comma-separated string of this module's authors.
     *
     * @return the authors string, or an empty string if no authors are specified
     */
    String getAuthorsStringed();

    /**
     * Logs an informational message through this module's logger.
     *
     * @param message the message to log at INFO level
     */
    void logInfo(String message);

    /**
     * Logs a warning message through this module's logger.
     *
     * @param message the message to log at WARNING level
     */
    void logWarning(String message);

    /**
     * Logs a severe/error message through this module's logger.
     *
     * @param message the message to log at SEVERE level
     */
    void logSevere(String message);

    /**
     * Returns whether this module is currently in the enabled state.
     *
     * @return {@code true} if the module has been started and not yet stopped
     */
    boolean isEnabled();

    /**
     * Starts (enables) this module, triggering the module's enable logic and firing
     * the appropriate lifecycle events.
     */
    void start();

    /**
     * Stops (disables) this module, triggering the module's disable logic and firing
     * the appropriate lifecycle events.
     */
    void stop();

    /**
     * Restarts this module by stopping and then starting it again.
     */
    void restart();

    /**
     * Returns the data folder where this module stores its configuration and persistent files.
     *
     * @return the module's data directory; may not yet exist if the module has never been started
     */
    File getDataFolder();

    /**
     * Opens an {@link InputStream} for a resource file bundled within this module's JAR.
     *
     * @param fileName the path to the resource relative to the JAR root
     * @return an input stream for the resource, or {@code null} if the resource is not found
     */
    InputStream getResourceAsStream(String fileName);

    /**
     * Returns whether this module is currently in a malleable (reconfigurable) state.
     *
     * <p>A malleable module may accept runtime configuration changes that a locked module
     * would reject.</p>
     *
     * @return {@code true} if the module is malleable
     */
    boolean isMalleable();

    /**
     * Sets the malleable state of this module.
     *
     * @param malleable {@code true} to allow runtime reconfiguration; {@code false} to lock it
     */
    void setMalleable(boolean malleable);

    /**
     * Registers the given command with this module, making it available for execution.
     *
     * @param command the command to register; must not be {@code null}
     */
    void addCommand(ModuleCommand command);

    /**
     * Unregisters the given command from this module, removing it from the command dispatcher.
     *
     * @param command the command to remove; no-op if the command was not previously registered
     */
    void removeCommand(ModuleCommand command);
}
