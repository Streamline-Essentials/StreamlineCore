package singularity.interfaces.audiences.messaging;

/**
 * Represents an audience member that can execute server commands.
 *
 * <p>Implementations dispatch the given command string directly to the server's command
 * dispatcher, as if the implementing entity had typed it in-game (without a leading
 * {@code /}).  The entity's permission level determines which commands are allowed.</p>
 */
public interface ICommandable {

    /**
     * Executes the given command as this entity.
     *
     * <p>The command should be provided without a leading {@code /} unless the platform
     * requires it. Implementations are responsible for routing the call to the correct
     * platform command dispatcher.</p>
     *
     * @param command the command string to execute, without a leading slash
     */
    void runCommand(String command);
}
