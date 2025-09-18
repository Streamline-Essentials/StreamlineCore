package singularity.command;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.utils.MessageUtils;

public class CommandMessageBuilder {
    @Getter @Setter
    private static String subChannel = "main-command-message";

    public static ProxiedMessage build(CosmicPlayer carrier, String server, CommandExecution execution) {
        ProxiedMessage r = new ProxiedMessage(carrier, Singularity.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("server", server);
        r.write("command", execution.getCommand());
        r.write("as", execution.getSenderValue());

        return r;
    }

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
