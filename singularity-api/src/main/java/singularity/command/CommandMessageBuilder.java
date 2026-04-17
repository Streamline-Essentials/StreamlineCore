package singularity.command;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.utils.MessageUtils;

/**
 * Builds and handles {@link ProxiedMessage} packets used to route a
 * {@link CommandExecution} from one server to another via the proxy plugin-messaging
 * channel.
 *
 * <p>Use {@link #build} to create a packet carrying the server target, the command
 * string, and the sender value. Use {@link #handle} to decode an incoming packet and
 * execute the encapsulated command on the receiving server.</p>
 */
public class CommandMessageBuilder {

    /**
     * The plugin-messaging sub-channel name used to identify command-routing packets.
     */
    @Getter @Setter
    private static String subChannel = "main-command-message";

    /**
     * Builds a {@link ProxiedMessage} that instructs the target server to execute
     * {@code execution} as the sender described in that execution.
     *
     * @param carrier   the online player used to carry the plugin message
     * @param server    the name of the destination server
     * @param execution the command execution data to encode in the message
     * @return a ready-to-send {@link ProxiedMessage}
     */
    public static ProxiedMessage build(CosmicPlayer carrier, String server, CommandExecution execution) {
        ProxiedMessage r = new ProxiedMessage(carrier, Singularity.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("server", server);
        r.write("command", execution.getCommand());
        r.write("as", execution.getSenderValue());

        return r;
    }

    /**
     * Decodes an incoming {@link ProxiedMessage} and executes the embedded command
     * if the message targets the correct sub-channel. Messages with missing fields
     * are logged as warnings and ignored.
     *
     * @param in the incoming proxied message to decode and dispatch
     */
    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) return;

        String server = in.getString("server");
        String command = in.getString("command");
        String as = in.getString("as");

        MessageUtils.logDebug("Handling command message: " + command);

        if (server == null || command == null || as == null) {
            MessageUtils.logWarning("Received a command message with null values: " + in);
            return;
        }

        CommandExecution execution = new CommandExecution(as, command);
        execution.executeMayFail();
    }
}
