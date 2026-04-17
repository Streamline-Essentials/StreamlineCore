package singularity.interfaces.audiences.messaging;

/**
 * Represents an audience member that can send chat messages as if typed by the player.
 *
 * <p>Unlike {@link ICommandable}, which executes server-side commands, {@code IChatter}
 * causes the implementing entity to emit chat input — which on most platforms will be
 * broadcast to other players or processed by chat plugins before reaching the server.</p>
 */
public interface IChatter {

    /**
     * Sends the given text as a chat message originating from this entity.
     *
     * <p>The {@code command} parameter name is historical; the value is treated as a
     * raw chat string, not a command. On platforms where commands and chat share the
     * same input channel, callers should omit any leading {@code /} to avoid unintended
     * command execution.</p>
     *
     * @param command the chat text to send on behalf of this entity
     */
    void chatAs(String command);
}
