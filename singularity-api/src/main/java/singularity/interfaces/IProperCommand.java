package singularity.interfaces;

/**
 * Contract for a platform-adapted command wrapper that can be registered with and
 * unregistered from the native command dispatcher of the running platform.
 *
 * <p>Each platform module (Velocity, BungeeCord, Spigot) provides its own
 * {@code ProperCommand} implementation that bridges a {@link singularity.command.CosmicCommand}
 * to the platform's command registration system.</p>
 */
public interface IProperCommand {

    /**
     * Registers this command with the platform's native command dispatcher, making it
     * available for players and the console to execute.
     */
    void registerThis();

    /**
     * Unregisters this command from the platform's native command dispatcher, preventing
     * further execution until re-registered.
     */
    void unregisterThis();
}
