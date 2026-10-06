package host.plas.commands;

import host.plas.MobGriefControl;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.modules.ModuleUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /mobgrief reload} — re-reads config.yml.
 */
public class MobGriefCommand extends ModuleCommand {
    public MobGriefCommand() {
        super(MobGriefControl.getInstance(), "mobgrief", "streamline.command.mobgrief.default", "mgc");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (! "reload".equalsIgnoreCase(context.getStringArg(0))) {
            context.sendMessage("&cUsage: &f/mobgrief reload");
            return;
        }

        MobGriefControl.getGriefConfig().init();
        context.sendMessage("&aReloaded the mob grief config&8.");
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
