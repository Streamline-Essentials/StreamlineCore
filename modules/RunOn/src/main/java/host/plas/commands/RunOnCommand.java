package host.plas.commands;

import host.plas.RunOn;
import singularity.command.CommandExecution;
import singularity.command.ModuleCommand;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.utils.MessageUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class RunOnCommand extends ModuleCommand {
    public RunOnCommand() {
        super(RunOn.getInstance(),
                "run-on",
                "streamline.command.run-on.default",
                "ro", "runon");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String server = context.getStringArg(0);
        String senderValue = context.getStringArg(1);

        String command = MessageUtils.argsToStringMinus(context.getArgsArray(), 0, 1);

        CommandExecution execution = new CommandExecution(senderValue, command);
        execution.execute(server);

        context.getSender().sendMessage("&eCommand sent to &c" + server + " &eas &b" + senderValue + "&8.");
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        if (context.getArgCount() <= 1) {
            r.add("HERE");
            r.add("PROXY");
            r.add("<server-name>");
        }
        if (context.getArgCount() == 2) {
            r.add("@c");
            r.add("@n:<player-name>");
            r.add("@u:<player-uuid>");
            r.add("@<player-name>");
            r.add("<player-name>");
        }
        if (context.getArgCount() == 3) {
            r.add("<command>");
        }
        if (context.getArgCount() > 3) {
            r.add("<arg>");
        }

        return r;
    }
}
