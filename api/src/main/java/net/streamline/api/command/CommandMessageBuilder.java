package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

public class CommandMessageBuilder {
    @Getter @Setter
    private static String subChannel = "main-command-message";

    public static ProxiedMessage build(StreamPlayer carrier, String server, CommandExecution execution) {
        ProxiedMessage r = new ProxiedMessage(carrier, SLAPI.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("server", server);
        r.write("command", execution.getCommand());
        r.write("as", execution.getSenderValue());

        return r;
    }

    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) return;

        MessageUtils.logDebug("Handling command message: " + in);

        String server = in.getString("server");
        String command = in.getString("command");
        String as = in.getString("as");

        if (server == null || command == null || as == null) {
            MessageUtils.logWarning("Received a command message with null values: " + in);
            return;
        }

        CommandExecution execution = new CommandExecution(as, command);
        execution.execute(server);
    }
}
