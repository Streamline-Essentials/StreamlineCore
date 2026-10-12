package host.plas.commands;

import host.plas.StreamlineMOTD;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.modules.ModuleUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /slmotd reload} — re-reads config.yml and rebuilds the ping response.
 */
public class MOTDCommand extends ModuleCommand {
    public MOTDCommand() {
        super(StreamlineMOTD.getInstance(), "slmotd", "streamline.command.slmotd.default", "streamlinemotd");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (! "reload".equalsIgnoreCase(context.getStringArg(0))) {
            context.sendMessage("&cUsage: &f/slmotd reload");
            return;
        }

        // init() re-reads the file, fills in missing defaults and rebuilds the MOTD,
        // sample, version and favicon; the runner picks up a changed update period on its next run.
        StreamlineMOTD.getConfig().init();
        context.sendMessage("&aReloaded the MOTD config&8.");
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) {
            String partial = context.getStringArg(0);
            return ModuleUtils.getCompletion(List.of("reload"), partial == null ? "" : partial);
        }
        return new ConcurrentSkipListSet<>();
    }
}
