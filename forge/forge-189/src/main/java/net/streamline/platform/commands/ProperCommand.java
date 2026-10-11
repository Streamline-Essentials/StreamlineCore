package net.streamline.platform.commands;

import singularity.command.CosmicCommand;
import singularity.interfaces.IProperCommand;

/**
 * A module command on a 1.8.9 client. Module commands run on a server, and this client
 * never hosts one, so there is nothing to register them with; {@code /slclient} explains
 * that when one is typed.
 */
public class ProperCommand implements IProperCommand {

    private final CosmicCommand command;

    public ProperCommand(CosmicCommand command) {
        this.command = command;
    }

    public CosmicCommand getCommand() {
        return command;
    }

    @Override
    public void registerThis() {
    }

    @Override
    public void unregisterThis() {
    }
}
